package com.isacsilva.ufumessenger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isacsilva.ufumessenger.domain.contracts.ChatRepository
import com.isacsilva.ufumessenger.domain.contracts.UserRepository
import com.isacsilva.ufumessenger.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CreateGroupUiState(
    val potentialMembers: List<User> = emptyList(),
    val selectedContactIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository // 1. INJETAR O ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPotentialMembers()
    }

    private fun loadPotentialMembers() {
        // 2. BUSCAR AS DUAS FONTES DE DADOS
        val contactsFlow = userRepository.getContacts()
        val conversationsFlow = chatRepository.getUserConversations()

        // 3. COMBINAR OS RESULTADOS DOS DOIS FLUXOS
        combine(contactsFlow, conversationsFlow) { contacts, conversationsWithDetails ->
            // Extrai os utilizadores das conversas
            val usersFromConversations = conversationsWithDetails.mapNotNull { it.otherParticipant }

            // Junta as duas listas, remove duplicados pelo ID e ordena por nome
            val combinedList = (contacts + usersFromConversations)
                .distinctBy { it.uid }
                .sortedBy { it.username }

            // Atualiza o estado da UI com a lista final
            _uiState.update {
                it.copy(potentialMembers = combinedList, isLoading = false)
            }
        }.launchIn(viewModelScope)
    }

    fun onContactSelected(contactId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val newSelectedIds = currentState.selectedContactIds.toMutableSet()
            if (isSelected) {
                newSelectedIds.add(contactId)
            } else {
                newSelectedIds.remove(contactId)
            }
            currentState.copy(selectedContactIds = newSelectedIds)
        }
    }
}