package com.isacsilva.ufumessenger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.isacsilva.ufumessenger.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.isacsilva.ufumessenger.ui.viewmodel.OtherUserProfileViewModel
import com.isacsilva.ufumessenger.util.rememberFormattedUserStatus // NOVO IMPORT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    navController: NavController,
    viewModel: OtherUserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Erro desconhecido",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                uiState.user != null -> {
                    val user = uiState.user!!
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = user.profilePictureUrl?.ifEmpty { R.drawable.ic_person_placeholder } ?: R.drawable.ic_person_placeholder,
                            contentDescription = "Foto do perfil de ${user.username ?: "usuário"}",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.ic_person_placeholder),
                            error = painterResource(id = R.drawable.ic_person_placeholder)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = user.username ?: "Nome não disponível",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        // Exibição do Status de Presença (Online/Visto por último)
                        val formattedPresenceStatus = rememberFormattedUserStatus(user = user)
                        if (formattedPresenceStatus.isNotBlank()) {
                            Text(
                                text = formattedPresenceStatus,
                                fontSize = 16.sp,
                                color = if (user.presenceStatus == "Online") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = user.userSetStatus?.takeIf { it.isNotBlank() } ?: "Disponível",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // Mudado para onSurfaceVariant para não competir com o status "Online"
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = user.email ?: "Email não disponível",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (!user.birthDate.isNullOrBlank()) {
                            Text(
                                text = "Nascimento: ${user.birthDate}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    Text("Usuário não encontrado.")
                }
            }
        }
    }
}
