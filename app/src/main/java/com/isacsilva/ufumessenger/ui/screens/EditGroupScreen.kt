package com.isacsilva.ufumessenger.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.isacsilva.ufumessenger.domain.model.User
import com.isacsilva.ufumessenger.ui.viewmodel.ChatViewModel
import com.isacsilva.ufumessenger.ui.viewmodel.GroupActionState
import kotlinx.coroutines.launch
import com.isacsilva.ufumessenger.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupScreen(
    conversationId: String,
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    var groupName by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupActionState by viewModel.groupActionState.collectAsStateWithLifecycle()
    val currentUserId = Firebase.auth.currentUser?.uid

    // Launcher para seleção de imagem
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.updateGroupImage(conversationId, it)
            }
        }
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Efeito para recarregar os detalhes após ações do grupo
    LaunchedEffect(groupActionState) {
        when (groupActionState) {
            is GroupActionState.Success -> {
                // Recarregar os detalhes da conversa após sucesso
                viewModel.loadConversationDetails(conversationId)
                scope.launch {
                    snackbarHostState.showSnackbar((groupActionState as GroupActionState.Success).message)
                    viewModel.resetGroupActionState()
                }
            }
            is GroupActionState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar((groupActionState as GroupActionState.Error).message)
                    viewModel.resetGroupActionState()
                }
            }
            else -> {}
        }
    }

    // Carregar detalhes iniciais da conversa
    LaunchedEffect(conversationId) {
        viewModel.loadConversationDetails(conversationId)
    }

    // Atualizar nome do grupo quando os detalhes carregarem
    LaunchedEffect(uiState.conversationDetails) {
        uiState.conversationDetails?.conversation?.groupName?.let {
            groupName = it
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Editar Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // SEÇÃO DA IMAGEM DO GRUPO - ATUALIZADA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    val groupImageUrl = uiState.conversationDetails?.conversation?.groupImageUrl

                    if (!groupImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = groupImageUrl,
                            contentDescription = "Imagem do Grupo",
                            modifier = Modifier
                                .size(128.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable(enabled = groupActionState !is GroupActionState.Loading) {
                                    pickMediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentScale = ContentScale.Crop,
                            // Adicionar placeholder e error para melhor UX
                            placeholder = painterResource(id = R.drawable.ic_person_placeholder),
                            error = painterResource(id = R.drawable.ic_person_placeholder)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Imagem do Grupo",
                            modifier = Modifier
                                .size(128.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable(enabled = groupActionState !is GroupActionState.Loading) {
                                    pickMediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Indicador de loading ou botão de editar
                    if (groupActionState is GroupActionState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Alterar imagem do grupo",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Campo de nome do grupo
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Nome do Grupo") },
                modifier = Modifier.fillMaxWidth(),
                enabled = groupActionState !is GroupActionState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botão de salvar
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        viewModel.updateGroupName(conversationId, groupName)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = groupName.isNotBlank() &&
                        groupName != uiState.conversationDetails?.conversation?.groupName &&
                        groupActionState !is GroupActionState.Loading
            ) {
                if (groupActionState is GroupActionState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvando...")
                } else {
                    Text("Salvar Alterações")
                }
            }

            // Seção de participantes
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Participantes:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            ) {
                items(uiState.conversationDetails?.conversation?.participants ?: emptyList()) { participantId ->
                    val user = uiState.participantsDetails[participantId]
                    ParticipantItem(
                        user = user,
                        onRemove = {
                            viewModel.removeParticipantFromGroup(conversationId, participantId)
                        },
                        canRemove = participantId != currentUserId && currentUserId != null,
                        isDisabled = groupActionState is GroupActionState.Loading
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão para adicionar participantes
            Button(
                onClick = {
                    navController.navigate("add_participants/$conversationId")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = groupActionState !is GroupActionState.Loading
            ) {
                Text("Adicionar Participantes")
            }
        }
    }
}

@Composable
fun ParticipantItem(
    user: User?,
    onRemove: () -> Unit,
    canRemove: Boolean,
    isDisabled: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar do participante
            AsyncImage(
                model = user?.profilePictureUrl ?: "",
                contentDescription = "Foto de ${user?.username}",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_person_placeholder),
                error = painterResource(id = R.drawable.ic_person_placeholder)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Nome do participante
            Text(
                text = user?.username ?: "Carregando...",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = if (isDisabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface
            )

            // Botão de remover (apenas se pode remover e não está desabilitado)
            if (canRemove && !isDisabled) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remover participante",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}