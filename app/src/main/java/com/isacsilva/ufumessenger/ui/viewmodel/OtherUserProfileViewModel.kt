package com.isacsilva.ufumessenger.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isacsilva.ufumessenger.domain.contracts.UserRepository
import com.isacsilva.ufumessenger.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtherUserProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class OtherUserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherUserProfileUiState())
    val uiState: StateFlow<OtherUserProfileUiState> = _uiState.asStateFlow()

    init {
        // userId deve ser passado como argumento de navegação
        savedStateHandle.get<String>("userId")?.let { userId ->
            if (userId.isNotBlank()) {
                fetchUserProfile(userId)
            } else {
                _uiState.value = OtherUserProfileUiState(isLoading = false, error = "User ID inválido.")
            }
        } ?: run {
            _uiState.value = OtherUserProfileUiState(isLoading = false, error = "User ID não fornecido.")
        }
    }

    private fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = OtherUserProfileUiState(isLoading = true) // Inicia o carregamento
            userRepository.getUserById(userId).fold(
                onSuccess = { user ->
                    _uiState.value = OtherUserProfileUiState(user = user, isLoading = false)
                },
                onFailure = { exception ->
                    _uiState.value = OtherUserProfileUiState(
                        isLoading = false,
                        error = "Falha ao carregar perfil: ${exception.localizedMessage}"
                    )
                }
            )
        }
    }
}
