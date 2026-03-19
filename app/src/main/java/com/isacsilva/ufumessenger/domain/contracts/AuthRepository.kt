package com.isacsilva.ufumessenger.domain.contracts

import android.app.Activity
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    fun getAuthState(): StateFlow<AuthState>
    suspend fun verifyPhoneNumber(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    )

    suspend fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential,
        username: String
    ): Result<Unit>
    fun signOut()

    suspend fun checkIfPhoneNumberExists(phoneNumber: String): Result<Boolean>


    suspend fun updateUserStatus(userId: String, presenceStatus: String, lastSeen: Long?)
}
