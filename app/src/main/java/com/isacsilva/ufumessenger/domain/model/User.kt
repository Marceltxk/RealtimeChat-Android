package com.isacsilva.ufumessenger.domain.model

data class User(
    val uid: String = "",
    val email: String? = null,
    val phone: String? = null,
    val username: String? = null,
    val username_lowercase: String? = null,
    val profilePictureUrl: String? = null,
    val fcmToken: String? = null,
    val birthDate: String? = null,
    val contacts: List<String> = emptyList(),

    // Status definido pelo usuário (ex: "Disponível", "Ocupado")
    val userSetStatus: String? = null, // Anteriormente 'status', default alterado

    // Status de presença automática
    val presenceStatus: String? = "Offline", // Novo campo, default para "Offline"
    val lastSeen: Long? = null  // Timestamp em milissegundos, já existente
)
