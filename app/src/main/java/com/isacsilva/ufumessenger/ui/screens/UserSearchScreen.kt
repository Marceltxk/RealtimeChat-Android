package com.isacsilva.ufumessenger.ui.screens

import android.Manifest
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.isacsilva.ufumessenger.domain.model.User
import com.isacsilva.ufumessenger.ui.viewmodel.UserSearchViewModel
import com.google.accompanist.permissions.*
import kotlinx.coroutines.flow.collectLatest
import androidx.hilt.navigation.compose.hiltViewModel
import com.isacsilva.ufumessenger.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (conversationId: String) -> Unit,
    onNewGroupClick: () -> Unit,
    viewModel: UserSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val invitePopup by viewModel.showInvitePopup.collectAsState(initial = null)

    LaunchedEffect(invitePopup) {
        if (invitePopup != null) {
            println("Popup deve ser exibido para: $invitePopup")
        }
    }

    invitePopup?.let { telefone ->
        AlertDialog(
            onDismissRequest = { viewModel.clearInvitePopup() },
            title = { Text("Usuário não possui o app") },
            text = { Text("Deseja enviar um convite por SMS para $telefone?") },
            confirmButton = {
                TextButton(onClick = {
                    enviarConviteSMS(context, telefone)
                    viewModel.clearInvitePopup()
                }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearInvitePopup()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Solicita permissão e sincroniza contatos
    SolicitarPermissaoContatos(viewModel, context)

    LaunchedEffect(Unit) {
        viewModel.carregarTodosUsuarios()
        viewModel.navigateToChat.collectLatest { conversationId ->
            onNavigateToChat(conversationId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova Conversa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Buscar por nome de usuário...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNewGroupClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = "Novo Grupo",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Novo Grupo",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.searchResults) { user ->
                        UserSearchItem(
                            user = user,
                            onClick = { viewModel.onUserSelected(user.uid) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!user.profilePictureUrl.isNullOrEmpty()) {
            AsyncImage(
                model = user.profilePictureUrl,
                contentDescription = "Foto do perfil de ${user.username}",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Ícone padrão de usuário",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = user.username ?: "Usuário sem nome",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SolicitarPermissaoContatos(viewModel: UserSearchViewModel, context: Context) {
    val permissionState = rememberPermissionState(Manifest.permission.READ_CONTACTS)

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            viewModel.lerContatos(context)
        }
    }
}

fun enviarConviteSMS(context: Context, telefone: String) {
    val smsIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        data = "sms:$telefone".toUri()
        putExtra("sms_body", "Oi! Baixe o ChatApp: https://linkdoapp.com")
    }
    context.startActivity(smsIntent)
}
