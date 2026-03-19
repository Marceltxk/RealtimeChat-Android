package com.isacsilva.ufumessenger.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isacsilva.ufumessenger.domain.contracts.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinalizeGroupUiState(
    val groupName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FinalizeGroupViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val memberIds: List<String> = savedStateHandle.get<String>("memberIds")?.split(",") ?: emptyList()

    private val _uiState = MutableStateFlow(FinalizeGroupUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigateToChat = Channel<String>()
    val navigateToChat = _navigateToChat.receiveAsFlow()

    fun onGroupNameChange(newName: String) {
        _uiState.update { it.copy(groupName = newName) }
    }

    fun createGroup() {
        if (_uiState.value.groupName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "O nome do grupo não pode estar vazio.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = chatRepository.createGroupConversation(_uiState.value.groupName, memberIds)
            result.onSuccess { newGroupId ->
                _navigateToChat.send(newGroupId)
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao criar o grupo.") }
            }
        }
    }
}