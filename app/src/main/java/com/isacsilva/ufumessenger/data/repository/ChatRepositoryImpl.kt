package com.isacsilva.ufumessenger.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.isacsilva.ufumessenger.domain.contracts.ChatRepository
import com.isacsilva.ufumessenger.domain.model.Conversation
import com.isacsilva.ufumessenger.domain.model.ConversationWithDetails
import com.isacsilva.ufumessenger.domain.model.Message
import com.isacsilva.ufumessenger.domain.model.MessageStatus
import com.isacsilva.ufumessenger.domain.model.MessageType
import com.isacsilva.ufumessenger.domain.model.User
import com.isacsilva.ufumessenger.util.SupabaseHelper
import com.isacsilva.ufumessenger.utils.NotificationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.isacsilva.ufumessenger.util.CryptoUtils

class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context, // Injetado para ler arquivos locais
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val storage: FirebaseStorage // Mantido no construtor para não quebrar o Hilt, mas não será mais usado
) : ChatRepository {

    override suspend fun createOrGetConversation(targetUserId: String): Result<String> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Utilizador não autenticado."))
        val participants = listOf(currentUserId, targetUserId).sorted()
        return try {
            val existingConversation = firestore.collection("conversations").whereEqualTo("participants", participants).limit(1).get().await()
            if (!existingConversation.isEmpty) {
                Result.success(existingConversation.documents.first().id)
            } else {
                val newConversation = Conversation(participants = participants, lastMessage = "Nenhuma mensagem ainda.")
                val newDocRef = firestore.collection("conversations").add(newConversation).await()
                Result.success(newDocRef.id)
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun createGroupConversation(groupName: String, participantIds: List<String>): Result<String> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Utilizador não autenticado."))
        val allParticipantIds = (participantIds + currentUserId).distinct()
        return try {
            val newConversationData = mapOf(
                "participants" to allParticipantIds, "isGroup" to true, "groupName" to groupName,
                "lastMessage" to "Grupo criado.", "lastMessageTimestamp" to FieldValue.serverTimestamp()
            )
            val newDocRef = firestore.collection("conversations").add(newConversationData).await()
            Result.success(newDocRef.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun markMessagesAsRead(conversationId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        try {
            val conversationDoc = firestore.collection("conversations").document(conversationId).get().await()
            val participantsRaw = conversationDoc.get("participants") as? List<*>
            val participants = participantsRaw?.mapNotNull { it as? String }
            val otherUserId = participants?.firstOrNull { it != currentUserId } ?: return

            val messagesToUpdateQuery = firestore.collection("conversations").document(conversationId).collection("messages")
                .whereEqualTo("senderId", otherUserId).whereIn("status", listOf(MessageStatus.SENT, MessageStatus.DELIVERED)).get().await()

            if (messagesToUpdateQuery.isEmpty) return
            val batch = firestore.batch()
            for (document in messagesToUpdateQuery.documents) {
                batch.update(document.reference, "status", MessageStatus.READ)
            }
            batch.commit().await()
        } catch (e: Exception) { Log.e("ChatRepoImpl", "Erro ao marcar mensagens como lidas", e) }
    }

    override fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("conversations").document(conversationId)
            .collection("messages").orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java).mapIndexed { index, message ->
                        // MÁGICA AQUI: Descriptografa o texto antes de entregar para a UI
                        val decryptedText = CryptoUtils.decrypt(message.text)
                        message.copy(
                            id = snapshot.documents[index].id,
                            text = decryptedText
                        )
                    }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in."))
        return try {
            val conversationRef = firestore.collection("conversations").document(conversationId)
            val messageRef = conversationRef.collection("messages").document()

            val encryptedText = CryptoUtils.encrypt(text)

            val newMessage = Message(id = messageRef.id, senderId = currentUserId, text = encryptedText)
            firestore.batch().apply {
                set(messageRef, newMessage)
                // Atualiza a última mensagem do grupo já criptografada também
                update(conversationRef, mapOf("lastMessage" to encryptedText, "lastMessageTimestamp" to FieldValue.serverTimestamp()))
            }.commit().await()

            NotificationUtils.getOtherParticipantId(conversationId)?.let { NotificationUtils.sendMessageNotification(it, text, conversationId) }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- UPLOADS USANDO SUPABASE A PARTIR DAQUI ---

    override suspend fun sendImageMessage(conversationId: String, imageUri: Uri, caption: String?): Result<Unit> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in."))
        return try {
            val imageFileName = "${UUID.randomUUID()}.jpg"
            val storagePath = "images/$conversationId/$imageFileName"

            // Usando Supabase para o Upload!
            val downloadUrl = SupabaseHelper.uploadFile(context, "chat-media", storagePath, imageUri, "image/jpeg")
                ?: return Result.failure(Exception("Falha no upload do Supabase"))

            val conversationRef = firestore.collection("conversations").document(conversationId)
            val messageRef = conversationRef.collection("messages").document()
            val messageText = if (!caption.isNullOrBlank()) caption else MessageType.IMAGE_LABEL
            val newMessage = Message(
                id = messageRef.id, senderId = currentUserId, type = MessageType.IMAGE,
                mediaUrl = downloadUrl, text = messageText, fileName = imageUri.lastPathSegment ?: imageFileName
            )

            val lastMessageTextForConversation = if (!caption.isNullOrBlank()) "${MessageType.IMAGE_LABEL}: $caption" else MessageType.IMAGE_LABEL
            firestore.batch().apply {
                set(messageRef, newMessage)
                update(conversationRef, mapOf("lastMessage" to lastMessageTextForConversation, "lastMessageTimestamp" to FieldValue.serverTimestamp()))
            }.commit().await()

            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun sendVideoMessage(conversationId: String, videoUri: Uri, caption: String?, videoThumbnailBytes: ByteArray?, videoDuration: Long?): Result<Unit> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in."))
        return try {
            val videoFileId = UUID.randomUUID().toString()
            val originalFileName = videoUri.lastPathSegment ?: videoFileId
            val fileExtension = originalFileName.substringAfterLast('.', "mp4")
            val fullVideoFileNameInStorage = "$videoFileId.$fileExtension"
            val videoStoragePath = "videos/$conversationId/$fullVideoFileNameInStorage"

            // Usando Supabase para Vídeo
            val videoDownloadUrl = SupabaseHelper.uploadFile(context, "chat-media", videoStoragePath, videoUri, "video/mp4")
                ?: return Result.failure(Exception("Falha no upload do vídeo no Supabase"))

            // Usando Supabase para a Miniatura do Vídeo
            var thumbnailDownloadUrl: String? = null
            if (videoThumbnailBytes != null) {
                val thumbnailPath = "video_thumbnails/$conversationId/${videoFileId}_thumb.jpg"
                thumbnailDownloadUrl = SupabaseHelper.uploadBytes("chat-media", thumbnailPath, videoThumbnailBytes, "image/jpeg")
            }

            val conversationRef = firestore.collection("conversations").document(conversationId)
            val messageRef = conversationRef.collection("messages").document()
            val messageText = if (!caption.isNullOrBlank()) caption else MessageType.VIDEO_LABEL

            val newMessage = Message(
                id = messageRef.id, senderId = currentUserId, type = MessageType.VIDEO,
                mediaUrl = videoDownloadUrl, thumbnailUrl = thumbnailDownloadUrl, text = messageText,
                fileName = originalFileName, duration = videoDuration
            )

            val lastMessageTextForConversation = if (!caption.isNullOrBlank()) "${MessageType.VIDEO_LABEL}: $caption" else MessageType.VIDEO_LABEL
            firestore.batch().apply {
                set(messageRef, newMessage)
                update(conversationRef, mapOf("lastMessage" to lastMessageTextForConversation, "lastMessageTimestamp" to FieldValue.serverTimestamp()))
            }.commit().await()

            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun sendAudioMessage(conversationId: String, audioUri: Uri, durationMs: Long): Result<Unit> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in."))
        return try {
            val fileName = "${UUID.randomUUID()}.m4a"
            val storagePath = "audios/$conversationId/$fileName"

            // Usando Supabase para Áudio
            val downloadUrl = SupabaseHelper.uploadFile(context, "chat-media", storagePath, audioUri, "audio/m4a")
                ?: return Result.failure(Exception("Falha no upload de áudio no Supabase"))

            val conversationRef = firestore.collection("conversations").document(conversationId)
            val messageRef = conversationRef.collection("messages").document()

            val newMessage = Message(
                id = messageRef.id, senderId = currentUserId, type = MessageType.AUDIO,
                mediaUrl = downloadUrl, duration = durationMs, text = MessageType.AUDIO_LABEL
            )

            firestore.batch().apply {
                set(messageRef, newMessage)
                update(conversationRef, mapOf("lastMessage" to MessageType.AUDIO_LABEL, "lastMessageTimestamp" to FieldValue.serverTimestamp()))
            }.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateGroupImage(conversationId: String, imageUri: Uri): Result<String> {
        return try {
            val path = "images/group_$conversationId/${System.currentTimeMillis()}.jpg"
            val downloadUrl = SupabaseHelper.uploadFile(context, "chat-media", path, imageUri, "image/jpeg")
                ?: return Result.failure(Exception("Falha no upload da imagem do grupo no Supabase"))

            updateGroupImage(conversationId, downloadUrl)
            Result.success(downloadUrl)
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- O RESTANTE DO CÓDIGO PERMANECE INTACTO ---

    override suspend fun sendLocationMessage(conversationId: String, latitude: Double, longitude: Double, address: String?): Result<Unit> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in."))
        return try {
            val conversationRef = firestore.collection("conversations").document(conversationId)
            val messageRef = conversationRef.collection("messages").document()
            val text = address ?: "Localização enviada"
            val newMessage = Message(id = messageRef.id, senderId = currentUserId, type = MessageType.LOCATION, text = text, latitude = latitude, longitude = longitude)
            firestore.batch().apply {
                set(messageRef, newMessage)
                update(conversationRef, mapOf("lastMessage" to "${MessageType.LOCATION_LABEL}: $text", "lastMessageTimestamp" to FieldValue.serverTimestamp()))
            }.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override fun getUserConversations(): Flow<List<ConversationWithDetails>> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return flowOf(emptyList())
        val conversationsFlow: Flow<List<Conversation>> = callbackFlow {
            val listener = firestore.collection("conversations").whereArrayContains("participants", currentUserId).orderBy("lastMessageTimestamp", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { documentToConversation(it) } ?: emptyList())
            }
            awaitClose { listener.remove() }
        }
        return conversationsFlow.flatMapLatest { conversations ->
            if (conversations.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val detailedFlows = conversations.map { c ->
                if (c.isGroup) flowOf(ConversationWithDetails(c, null))
                else {
                    val otherId = c.participants.firstOrNull { it != currentUserId } ?: ""
                    getUserFlow(otherId).map { u -> ConversationWithDetails(c, u) }
                }
            }
            combine(detailedFlows) { it.toList() }
        }
    }

    private fun documentToConversation(doc: DocumentSnapshot): Conversation? {
        return try {
            // Pegamos o texto bruto do banco
            val rawLastMessage = doc.getString("lastMessage")
            val rawPinnedText = doc.getString("pinnedMessageText")

            val decryptedLastMessage = rawLastMessage?.let { CryptoUtils.decrypt(it) }
            val decryptedPinnedText = rawPinnedText?.let { CryptoUtils.decrypt(it) }

            Conversation(
                id = doc.id,
                participants = doc.get("participants") as? List<String> ?: emptyList(),
                lastMessage = decryptedLastMessage,
                lastMessageTimestamp = doc.getDate("lastMessageTimestamp"),
                isGroup = doc.getBoolean("isGroup") ?: false,
                groupName = doc.getString("groupName"),
                pinnedMessageId = doc.getString("pinnedMessageId"),
                pinnedMessageText = decryptedPinnedText,
                pinnedMessageSenderId = doc.getString("pinnedMessageSenderId"),
                groupImageUrl = doc.getString("groupImageUrl")
            )
        } catch (e: Exception) { null }
    }

    override fun getConversationDetails(conversationId: String): Flow<ConversationWithDetails?> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: ""

        // Criamos o fluxo manualmente, sem depender da importação de extensão 'snapshots()'
        val conversationFlow = callbackFlow<Conversation?> {
            val listener = firestore.collection("conversations").document(conversationId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(documentToConversation(snapshot))
                    } else {
                        trySend(null)
                    }
                }
            awaitClose { listener.remove() }
        }

        return conversationFlow.flatMapLatest<Conversation?, ConversationWithDetails?> { c ->
            if (c == null) flowOf(null)
            else if (c.isGroup) flowOf(ConversationWithDetails(c, null))
            else {
                val otherId = c.participants.firstOrNull { it != currentUserId } ?: ""
                if (otherId.isBlank()) flowOf(ConversationWithDetails(c, null))
                else getUserFlow(otherId).map { u -> ConversationWithDetails(c, u) }
            }
        }
    }

    private fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        if (userId.isBlank()) { trySend(null); close(); return@callbackFlow }
        val listener = firestore.collection("users").document(userId).addSnapshotListener { s, e ->
            if (e != null) { trySend(null); close(e); return@addSnapshotListener }
            trySend(s?.toObject(User::class.java))
        }
        awaitClose { listener.remove() }
    }

    override suspend fun pinMessage(conversationId: String, message: Message?): Result<Unit> {
        return try {
            val ref = firestore.collection("conversations").document(conversationId)
            if (message == null) ref.update(mapOf("pinnedMessageId" to null, "pinnedMessageText" to null, "pinnedMessageSenderId" to null)).await()
            else ref.update(mapOf("pinnedMessageId" to message.id, "pinnedMessageText" to message.text, "pinnedMessageSenderId" to message.senderId)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateGroupName(conversationId: String, newName: String): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Não autenticado"))
            val doc = firestore.collection("conversations").document(conversationId).get().await()
            if (!(doc.get("participants") as? List<*>)?.contains(uid)!!) return Result.failure(Exception("Não participante"))
            firestore.collection("conversations").document(conversationId).update("groupName", newName).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun addParticipantsToGroup(conversationId: String, userIds: List<String>): Result<Unit> {
        return try {
            val doc = firestore.collection("conversations").document(conversationId).get().await()
            val current = doc.get("participants") as? List<String> ?: emptyList()
            firestore.collection("conversations").document(conversationId).update("participants", (current + userIds).distinct()).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun removeParticipantFromGroup(conversationId: String, userId: String): Result<Unit> {
        return try {
            val doc = firestore.collection("conversations").document(conversationId).get().await()
            val current = doc.get("participants") as? List<String> ?: emptyList()
            firestore.collection("conversations").document(conversationId).update("participants", current.filter { it != userId }).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getGroupDetails(conversationId: String): Result<Conversation> {
        return try {
            val doc = firestore.collection("conversations").document(conversationId).get().await()
            Result.success(doc.toObject(Conversation::class.java)?.copy(id = doc.id)!!)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getAvailableUsers(): Result<List<User>> {
        return try {
            val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Não autenticado"))
            val snap = firestore.collection("users").get().await()
            Result.success(snap.documents.mapNotNull { it.toObject(User::class.java)?.copy(uid = it.id) }.filter { it.uid != uid })
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateGroupImage(conversationId: String, imageUrl: String): Result<Unit> {
        return try {
            firestore.collection("conversations").document(conversationId).update("groupImageUrl", imageUrl).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}