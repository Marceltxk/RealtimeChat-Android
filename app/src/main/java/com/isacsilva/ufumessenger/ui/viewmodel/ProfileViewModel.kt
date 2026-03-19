package com.isacsilva.ufumessenger.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isacsilva.ufumessenger.domain.contracts.AuthRepository
import com.isacsilva.ufumessenger.domain.contracts.UserRepository
import com.isacsilva.ufumessenger.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.isacsilva.ufumessenger.util.SupabaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val user: User? = null,
    val isLoadingUser: Boolean = true,
    val editableUsername: String = "",
    val editableEmail: String = "",
    val editableBirthDate: String = "",
    val editableStatus: String = "",
    val showDatePickerDialog: Boolean = false,
    val isUploadingProfilePicture: Boolean = false,
    val profileUploadError: String? = null,
    val isSavingProfile: Boolean = false,
    val profileSaveSuccessMessage: String? = null,
    val profileSaveErrorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val utcDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    init {
        viewModelScope.launch {
            authRepository.getAuthState().collectLatest { authState ->
                _uiState.update { currentState ->
                    val isDifferentUser = currentState.user?.uid != null && currentState.user.uid != authState.user?.uid
                    val shouldResetEditableFields = currentState.editableUsername.isEmpty() ||
                            currentState.editableEmail.isEmpty() ||
                            currentState.editableBirthDate.isEmpty() ||
                            currentState.editableStatus.isEmpty() ||
                            isDifferentUser

                    currentState.copy(
                        user = authState.user,
                        isLoadingUser = authState.isInitialLoading,
                        editableUsername = if (shouldResetEditableFields) authState.user?.username ?: "" else currentState.editableUsername,
                        editableEmail = if (shouldResetEditableFields) authState.user?.email ?: "" else currentState.editableEmail,
                        editableBirthDate = if (shouldResetEditableFields) authState.user?.birthDate ?: "" else currentState.editableBirthDate,
                        editableStatus = if (shouldResetEditableFields) authState.user?.userSetStatus ?: "" else currentState.editableStatus
                    )
                }
            }
        }
    }

    fun onUsernameChanged(newUsername: String) {
        _uiState.update { it.copy(editableUsername = newUsername) }
    }

    fun onEmailChanged(newEmail: String) {
        _uiState.update { it.copy(editableEmail = newEmail) }
    }

    fun onBirthDateTextChanged(newBirthDate: String) {
        _uiState.update { it.copy(editableBirthDate = newBirthDate) }
    }

    fun onBirthDateClicked() {
        _uiState.update { it.copy(showDatePickerDialog = true) }
    }

    fun onDatePickerDialogDismissed() {
        _uiState.update { it.copy(showDatePickerDialog = false) }
    }

    fun onBirthDateSelected(dateInMillis: Long?) {
        if (dateInMillis != null) {
            val selectedDateString = utcDateFormatter.format(Date(dateInMillis))
            _uiState.update { it.copy(editableBirthDate = selectedDateString) }
        }
        onDatePickerDialogDismissed()
    }

    fun onStatusChanged(newUserSetStatus: String) {
        _uiState.update { it.copy(editableStatus = newUserSetStatus) }
    }

    fun saveProfile() {
        val currentUser = _uiState.value.user
        if (currentUser == null || currentUser.uid.isEmpty()) {
            _uiState.update { it.copy(profileSaveErrorMessage = "Usuário não autenticado.") }
            return
        }

        val newUsername = _uiState.value.editableUsername.trim()
        val newEmail = _uiState.value.editableEmail.trim()
        val newBirthDate = _uiState.value.editableBirthDate.trim()
        val newEditableStatus = _uiState.value.editableStatus.trim()

        if (newUsername.isEmpty()) {
            _uiState.update { it.copy(profileSaveErrorMessage = "Nome de usuário não pode estar vazio.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, profileSaveErrorMessage = null, profileSaveSuccessMessage = null) }

            val result = userRepository.updateUserProfile(
                userId = currentUser.uid,
                newUsername = newUsername,
                newEmail = newEmail.ifBlank { null },
                newBirthDate = newBirthDate.ifBlank { null },
                newStatus = newEditableStatus
            )

            result.onSuccess {
                _uiState.update { currentState ->
                    val updatedUser = currentState.user?.copy(
                        username = newUsername,
                        email = newEmail.ifBlank { currentState.user.email },
                        birthDate = newBirthDate.ifBlank { currentState.user.birthDate },
                        userSetStatus = newEditableStatus.ifBlank { currentState.user.userSetStatus }
                    )
                    currentState.copy(
                        isSavingProfile = false,
                        profileSaveSuccessMessage = "Perfil atualizado com sucesso!",
                        user = updatedUser,
                        editableUsername = newUsername,
                        editableEmail = newEmail,
                        editableBirthDate = newBirthDate,
                        editableStatus = newEditableStatus
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        profileSaveErrorMessage = "Falha ao atualizar perfil: ${exception.message}"
                    )
                }
            }
        }
    }

    // A MÁGICA ACONTECE AQUI: Adicionamos o context como parâmetro e chamamos o Supabase
    fun uploadProfilePicture(context: Context, uri: Uri) {
        viewModelScope.launch {
            val currentUser = _uiState.value.user
            if (currentUser == null || currentUser.uid.isEmpty()) {
                _uiState.value = _uiState.value.copy(profileUploadError = "Usuário não autenticado ou UID inválido.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isUploadingProfilePicture = true, profileUploadError = null)

            try {
                // Monta o caminho do arquivo no Supabase (Ex: user_123_16848484.jpg)
                val fileName = "user_${currentUser.uid}_${System.currentTimeMillis()}.jpg"
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

                // Chama o nosso ajudante do Supabase para o bucket "avatars"
                val downloadUrl = SupabaseHelper.uploadFile(
                    context = context,
                    bucket = "avatars",
                    path = fileName,
                    fileUri = uri,
                    mimeType = mimeType
                )

                if (downloadUrl != null) {
                    // Se deu certo, atualiza o Firestore com o link público gerado pelo Supabase
                    firestore.collection("users").document(currentUser.uid)
                        .update("profilePictureUrl", downloadUrl)
                        .await()

                    _uiState.update { currentState ->
                        val updatedUserWithPic = currentState.user?.copy(profilePictureUrl = downloadUrl)
                        currentState.copy(
                            isUploadingProfilePicture = false,
                            user = updatedUserWithPic
                        )
                    }
                } else {
                    throw Exception("O Supabase retornou uma URL nula.")
                }

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Falha no upload da foto de perfil", e)
                _uiState.value = _uiState.value.copy(
                    isUploadingProfilePicture = false,
                    profileUploadError = "Falha no upload: ${e.localizedMessage ?: "Erro desconhecido"}"
                )
            }
        }
    }

    fun clearProfileUploadError() {
        _uiState.value = _uiState.value.copy(profileUploadError = null)
    }

    fun clearProfileSaveSuccessMessage() {
        _uiState.update { it.copy(profileSaveSuccessMessage = null) }
    }

    fun clearProfileSaveErrorMessage() {
        _uiState.update { it.copy(profileSaveErrorMessage = null) }
    }

    fun signOut() {
        authRepository.signOut()
    }
}