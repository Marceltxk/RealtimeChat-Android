package com.isacsilva.ufumessenger.domain.contracts

import com.isacsilva.ufumessenger.domain.model.User


data class AuthState(
    val user: User? = null,
    val isInitialLoading: Boolean = true
)

