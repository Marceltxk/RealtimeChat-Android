package com.isacsilva.ufumessenger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isacsilva.ufumessenger.domain.contracts.ChatRepository
import com.isacsilva.ufumessenger.domain.model.ConversationWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val conversations: List<ConversationWithDetails> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filteredConversations: List<ConversationWithDetails> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    chatRepository: ChatRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(chatRepository.getUserConversations(), _searchQuery)
        { conversations, query ->
            val filteredList = if (query.isBlank()) {
                conversations
            } else {
                conversations.filter { details ->
                    val conversation = details.conversation
                    if (conversation.isGroup) {
                        conversation.groupName?.contains(query, ignoreCase = true) ?: false
                    } else {
                        details.otherParticipant?.username?.contains(query, ignoreCase = true)
                            ?: false
                    }
                }
            }
            HomeUiState(
                conversations = conversations,
                isLoading = false,
                searchQuery = query,
                filteredConversations = filteredList
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}