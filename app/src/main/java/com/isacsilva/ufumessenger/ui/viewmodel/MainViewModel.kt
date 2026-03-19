package com.isacsilva.ufumessenger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import com.isacsilva.ufumessenger.domain.contracts.AuthRepository
import com.isacsilva.ufumessenger.domain.contracts.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.getAuthState()

    fun setUserOnline() {
        Log.d(
            "MainViewModel",
            "setUserOnline CALLED. User: ${authState.value.user?.uid}"
        ) // NOVO LOG
        viewModelScope.launch {
            authState.value.user?.uid?.let { userId ->
                Log.d(
                    "MainViewModel",
                    "setUserOnline: ATUALIZANDO status para Online para userId: $userId"
                ) // NOVO LOG
                authRepository.updateUserStatus(userId, "Online", null)
            }
        }
    }


    fun setUserOffline() {
        Log.d("MainViewModel", "setUserOffline CALLED. User: ${authState.value.user?.uid}")
        // Não use viewModelScope aqui se a Activity está prestes a ser destruída
        // e a operação de rede precisa de mais tempo.
        // Lançar em um escopo diferente pode ser uma opção, mas requer cuidado.
        // Por ora, vamos focar em garantir que a chamada ao repositório aconteça.

        val userId = authState.value.user?.uid
        if (userId != null) {
            val lastSeenTime = System.currentTimeMillis()
            Log.d(
                "MainViewModel",
                "setUserOffline: CHAMANDO authRepository.updateUserStatus para Offline para userId: $userId, lastSeen: $lastSeenTime"
            )

            // Para esta chamada específica de onStop, podemos precisar que o repositório
            // lide com isso de forma "fire-and-forget" ou use um escopo mais resiliente.
            // Vamos manter o viewModelScope por enquanto, mas o problema pode estar no await
            // dentro do repositório se o escopo for cancelado.
            viewModelScope.launch { // Mantenha o viewModelScope por agora, o problema pode estar no await do repo
                authRepository.updateUserStatus(userId, "Offline", lastSeenTime)
            }
        } else {
            Log.d("MainViewModel", "setUserOffline: Usuário nulo, nenhuma ação tomada.")
        }
    }
}