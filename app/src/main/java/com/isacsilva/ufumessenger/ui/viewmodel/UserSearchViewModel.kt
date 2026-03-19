package com.isacsilva.ufumessenger.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.isacsilva.ufumessenger.domain.contracts.ChatRepository
import com.isacsilva.ufumessenger.domain.contracts.UserRepository
import com.isacsilva.ufumessenger.domain.model.User
import com.isacsilva.ufumessenger.ui.screens.Contato
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserSearchUiState(
    val query: String = "",
    val searchResults: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserSearchUiState())
    val uiState: StateFlow<UserSearchUiState> = _uiState.asStateFlow()

    private val _navigateToChat = Channel<String>()
    val navigateToChat = _navigateToChat.receiveAsFlow()

    private val _contatos = MutableStateFlow<List<Contato>>(emptyList())
    val contatos: StateFlow<List<Contato>> = _contatos

    private var todosUsuariosFirebase: List<User> = emptyList()

    // Carregar usuários do Firebase quando o ViewModel for criado
    init {
        carregarTodosUsuarios()
    }

    fun carregarTodosUsuarios() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = userRepository.getAllUsers()
            result.onSuccess { users ->
                todosUsuariosFirebase = users
                atualizarResultadosCombinados()
                _uiState.update { it.copy(isLoading = false) }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erro ao carregar usuários"
                    )
                }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Buscar usuários do Firebase que correspondem à query
                val firebaseUsers = if (newQuery.isNotEmpty()) {
                    val result = userRepository.searchUsersByUsername(newQuery)
                    result.getOrElse { emptyList() }
                } else {
                    emptyList()
                }

                // Buscar contatos locais que correspondem à query
                val contatosFiltrados = _contatos.value.filter {
                    it.nome.contains(newQuery, ignoreCase = true) ||
                            it.telefone.contains(newQuery)
                }.map {
                    User(uid = it.telefone, username = it.nome, profilePictureUrl = null)
                }

                // Combinar e ordenar resultados
                val combinados = (firebaseUsers + contatosFiltrados)
                    .distinctBy { it.uid }
                    .sortedBy { it.username?.lowercase() ?: "" }

                _uiState.update {
                    it.copy(
                        searchResults = combinados,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erro na busca"
                    )
                }
            }
        }
    }

    private val _showInvitePopup = Channel<String?>()
    val showInvitePopup = _showInvitePopup.receiveAsFlow()

    fun onUserSelected(targetUserId: String) {
        viewModelScope.launch {
            val userExists = todosUsuariosFirebase.any { it.uid == targetUserId }

            if (userExists) {
                _uiState.update { it.copy(isLoading = true) }
                val result = chatRepository.createOrGetConversation(targetUserId)
                result.onSuccess { conversationId ->
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToChat.send(conversationId)
                }.onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Não foi possível iniciar a conversa."
                        )
                    }
                }
            } else {
                _showInvitePopup.send(targetUserId)
            }
        }
    }

    fun clearInvitePopup() {
        viewModelScope.launch {
            _showInvitePopup.send(null)
        }
    }

    @SuppressLint("Range")
    fun lerContatos(context: Context) {
        viewModelScope.launch {
            try {
                val lista = mutableListOf<Contato>()
                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )

                cursor?.use {
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (it.moveToNext()) {
                        val nome = it.getString(nameIndex) ?: ""
                        val telefone = it.getString(numberIndex) ?: ""

                        // Limpar e formatar telefone - CORREÇÃO AQUI
                        val telefoneLimpo = telefone.replace("[^0-9]".toRegex(), "")

                        // CORREÇÃO: Verificar length depois de garantir que não é null
                        if (nome.isNotBlank() && telefoneLimpo.length >= 10) {
                            // Evitar duplicatas
                            if (lista.none { contato -> contato.telefone == telefoneLimpo }) {
                                lista.add(Contato(nome, telefoneLimpo))
                            }
                        }
                    }
                }

                // Ordenar contatos alfabeticamente
                val contatosOrdenados = lista.sortedBy { it.nome.lowercase() }

                _contatos.value = contatosOrdenados
                sincronizarComFirebase(contatosOrdenados)
                atualizarResultadosCombinados()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Erro ao carregar contatos: ${e.message}")
                }
            }
        }
    }

    private fun atualizarResultadosCombinados() {
        // Converter contatos para Users
        val contatosConvertidos = _contatos.value.map {
            User(uid = it.telefone, username = it.nome, profilePictureUrl = null)
        }

        // Combinar usuários do Firebase + contatos do dispositivo
        val todosUsuarios = todosUsuariosFirebase + contatosConvertidos

        // Ordenar alfabeticamente por username
        val combinadosOrdenados = todosUsuarios
            .distinctBy { it.uid }
            .sortedBy { it.username?.lowercase() ?: "" }

        _uiState.update {
            it.copy(searchResults = combinadosOrdenados)
        }
    }

    fun sincronizarComFirebase(contatos: List<Contato>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("contatos/$uid")

        // Limpar contatos antigos antes de adicionar novos
        ref.removeValue().addOnCompleteListener {
            contatos.forEachIndexed { index, contato ->
                ref.child(index.toString()).setValue(contato)
            }
        }
    }

    fun atualizarContatos(context: Context) {
        lerContatos(context)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}