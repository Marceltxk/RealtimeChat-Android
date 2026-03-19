
package com.isacsilva.ufumessenger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.isacsilva.ufumessenger.domain.model.User
import com.isacsilva.ufumessenger.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.isacsilva.ufumessenger.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddParticipantsScreen(
    conversationId: String,
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUsers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var availableUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Carregar usuários disponíveis
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                availableUsers = withContext(Dispatchers.IO) {
                    viewModel.getAvailableUsers(conversationId)
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Erro ao carregar usuários"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adicionar Participantes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    if (selectedUsers.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.addParticipantsToGroup(conversationId, selectedUsers.toList())
                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Text("Adicionar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar usuários") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                val filteredUsers = availableUsers.filter { user ->
                    user.username?.contains(searchQuery, ignoreCase = true) == true &&
                            user.uid != FirebaseAuth.getInstance().currentUser?.uid
                }

                LazyColumn {
                    items(filteredUsers) { user ->
                        UserListItem(
                            user = user,
                            isSelected = selectedUsers.contains(user.uid),
                            onSelect = { selected ->
                                selectedUsers = if (selected) {
                                    selectedUsers + user.uid
                                } else {
                                    selectedUsers - user.uid
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Composable para item de usuário
@Composable
fun UserListItem(
    user: User,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onSelect(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar do usuário
            AsyncImage(
                model = user.profilePictureUrl ?: "",
                contentDescription = "Foto de ${user.username}",
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Nome do usuário
            Text(
                text = user.username ?: "Usuário",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // Checkbox de seleção
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelect
            )
        }
    }
}