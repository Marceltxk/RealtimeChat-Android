package com.isacsilva.ufumessenger.domain.contracts

import android.net.Uri
import com.isacsilva.ufumessenger.domain.model.Conversation
import com.isacsilva.ufumessenger.domain.model.ConversationWithDetails
import com.isacsilva.ufumessenger.domain.model.Message
import com.isacsilva.ufumessenger.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getUserConversations(): Flow<List<ConversationWithDetails>>
    fun getMessages(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(conversationId: String, text: String): Result<Unit>
    suspend fun createOrGetConversation(targetUserId: String): Result<String>
    fun getConversationDetails(conversationId: String): Flow<ConversationWithDetails?>
    suspend fun markMessagesAsRead(conversationId: String)




    suspend fun createGroupConversation(
        groupName: String,
        participantIds: List<String>
    ): Result<String>

    suspend fun pinMessage(conversationId: String, message: Message?): Result<Unit>

    suspend fun sendImageMessage(
        conversationId: String,
        imageUri: Uri,
        caption: String? = null
    ): Result<Unit>
    suspend fun updateGroupName(conversationId: String, newName: String): Result<Unit>
    suspend fun addParticipantsToGroup(conversationId: String, userIds: List<String>): Result<Unit>
    suspend fun removeParticipantFromGroup(conversationId: String, userId: String): Result<Unit>
    suspend fun getGroupDetails(conversationId: String): Result<Conversation>

    suspend fun getAvailableUsers(): Result<List<User>>

    suspend fun updateGroupImage(conversationId: String, imageUri: Uri): Result<String>
    suspend fun updateGroupImage(conversationId: String, imageUrl: String): Result<Unit>

    suspend fun sendVideoMessage(
        conversationId: String,
        videoUri: Uri,
        caption: String? = null,
        videoThumbnailBytes: ByteArray?,
        videoDuration: Long?
    ): Result<Unit>

    // --- NOVOS MÉTODOS PARA SENSORES ---
    suspend fun sendLocationMessage(
        conversationId: String,
        latitude: Double,
        longitude: Double,
        address: String? = null
    ): Result<Unit>

    suspend fun sendAudioMessage(
        conversationId: String,
        audioUri: Uri,
        durationMs: Long
    ): Result<Unit>
}
