package com.isacsilva.ufumessenger.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.isacsilva.ufumessenger.domain.contracts.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegistrationUiState(
    val isLoading: Boolean = false,
    val registrationSuccess: Boolean = false,
    val errorMessage: String? = null,
    val usernameError: String? = null,
    val phoneNumberError: String? = null,
    val codeError: String? = null,
    val isCodeSent: Boolean = false,
    val verificationId: String? = null
)

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState = _uiState.asStateFlow()

    fun startPhoneNumberVerification(username: String, phoneNumber: String, activity: Activity) {
        if (username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Username cannot be empty") }
            return
        } else if(username.length < 6) {
            _uiState.update { it.copy(usernameError = "Username should have at least 6 characters") }
            return
        } else {
            _uiState.update { it.copy(usernameError = null) }
        }

        if (phoneNumber.isBlank()) {
            _uiState.update { it.copy(phoneNumberError = "Phone number cannot be empty") }
            return
        } else {
            _uiState.update { it.copy(phoneNumberError = null) }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val phoneCheckResult = authRepository.checkIfPhoneNumberExists(phoneNumber)

            phoneCheckResult.onSuccess { numberExists ->
                if (numberExists) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            phoneNumberError = "This phone number is already registered. Please log in."
                        )
                    }
                } else {
                    authRepository.verifyPhoneNumber(phoneNumber, activity, verificationCallbacks)
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to verify phone number: ${exception.message}"
                    )
                }
            }
        }
    }

    private val verificationCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            _uiState.update { it.copy(isLoading = false) }
        }

        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
            _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isCodeSent = true,
                    verificationId = verificationId
                )
            }
        }
    }

    fun signInWithCode(code: String, username: String) {
        val verificationId = _uiState.value.verificationId

        if (verificationId == null) {
            _uiState.update { it.copy(errorMessage = "Verification process not started.") }
            return
        }

        if (code.length != 6) {
            _uiState.update { it.copy(codeError = "The code must be 6 digits long.") }
            return
        } else {
            _uiState.update { it.copy(codeError = null) }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val result = authRepository.signInWithPhoneAuthCredential(credential, username)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, registrationSuccess = true) }
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false, errorMessage = exception.message) }
            }
        }
    }

    fun onErrorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onRegistrationHandled() {
        _uiState.update { it.copy(registrationSuccess = false) }
    }
}