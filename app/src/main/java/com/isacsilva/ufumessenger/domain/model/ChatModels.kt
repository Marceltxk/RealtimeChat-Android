package com.isacsilva.ufumessenger.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String? = null,
    @ServerTimestamp val lastMessageTimestamp: Date? = null,
    val groupImageUrl: String? = null,
    val pinnedMessageId: String? = null,
    val pinnedMessageText: String? = null,
    val pinnedMessageSenderId: String? = null,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val localName: String? = null
)

data class ConversationWithDetails(
    val conversation: Conversation,
    val otherParticipant: User?
)

object MessageStatus {
    const val SENT = "SENT"
    const val DELIVERED = "DELIVERED"
    const val READ = "READ"
}

object MessageType {
    const val TEXT = "TEXT"
    const val IMAGE = "IMAGE"
    const val VIDEO = "VIDEO"
    const val AUDIO = "AUDIO" // --- NOVA EVOLUÇÃO ---
    const val LOCATION = "LOCATION" // --- NOVA EVOLUÇÃO ---
    const val FILE = "FILE"
    const val IMAGE_LABEL = "📷 Imagem"
    const val VIDEO_LABEL = "📹 Vídeo"
    const val AUDIO_LABEL = "🎤 Áudio" // --- NOVA EVOLUÇÃO ---
    const val LOCATION_LABEL = "📍 Localização" // --- NOVA EVOLUÇÃO ---
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    val status: String = MessageStatus.SENT,
    val type: String = MessageType.TEXT,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val duration: Long? = null,
    val latitude: Double? = null, // --- NOVA EVOLUÇÃO ---
    val longitude: Double? = null // --- NOVA EVOLUÇÃO ---
)