// NotificationUtils.kt
package com.isacsilva.ufumessenger.utils

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

object NotificationUtils {

    private val functions = FirebaseFunctions.getInstance()

    suspend fun sendPushNotification(
        recipientId: String,
        title: String,
        message: String,
        conversationId: String? = null,
        messageType: String = "text"
    ) {
        try {
            // Obter token FCM do destinatário
            val recipientDoc = Firebase.firestore.collection("users")
                .document(recipientId)
                .get()
                .await()

            val fcmToken = recipientDoc.getString("fcmToken")
            val senderId = Firebase.auth.currentUser?.uid

            if (fcmToken.isNullOrEmpty()) {
                Log.w("NotificationUtils", "Usuário $recipientId não tem token FCM registrado")
                return
            }

            // Dados para a notificação
            val data = hashMapOf<String, Any>(
                "token" to fcmToken,
                "title" to title,
                "body" to message,
                "conversationId" to (conversationId ?: ""),
                "senderId" to (senderId ?: ""),
                "type" to messageType,
                "senderName" to (Firebase.auth.currentUser?.displayName ?: "Alguém")
            )

            // Chamar Cloud Function
            functions.getHttpsCallable("sendNotification")
                .call(data)
                .await()

            Log.d("NotificationUtils", "Notificação enviada para: $recipientId")

        } catch (e: Exception) {
            Log.e("NotificationUtils", "Erro ao enviar notificação push", e)
        }
    }

    suspend fun sendMessageNotification(
        recipientId: String,
        messageText: String,
        conversationId: String,
        messageType: String = "text"
    ) {
        val senderName = Firebase.auth.currentUser?.displayName ?: "Alguém"
        val title = if (messageType == "image") "📷 $senderName" else senderName
        val body = if (messageType == "image") "Enviou uma imagem" else messageText

        sendPushNotification(recipientId, title, body, conversationId, messageType)
    }

    suspend fun getOtherParticipantId(conversationId: String): String? {
        return try {
            val currentUserId = Firebase.auth.currentUser?.uid ?: return null
            val conversation = Firebase.firestore.collection("conversations")
                .document(conversationId)
                .get()
                .await()

            val participants = conversation.get("participants") as? List<String> ?: emptyList()
            participants.firstOrNull { it != currentUserId }
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Erro ao obter outro participante", e)
            null
        }
    }
}