package com.isacsilva.ufumessenger.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.isacsilva.ufumessenger.R
import com.isacsilva.ufumessenger.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Pegando o context para passarmos ao Supabase
    val context = LocalContext.current

    // Estado para o ExposedDropdownMenuBox de Status
    var expandedStatusDropdown by remember { mutableStateOf(false) }
    val userSelectableStatusOptions = listOf("Disponível", "Ocupado")

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                // Ajuste feito: passando o context e a uri para o ViewModel
                viewModel.uploadProfilePicture(context, uri)
            }
        }
    )

    val initialDateMillis = remember(uiState.editableBirthDate) {
        if (uiState.editableBirthDate.isNotBlank()) {
            try {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(uiState.editableBirthDate)?.time
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    if (uiState.showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { viewModel.onDatePickerDialogDismissed() },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onBirthDateSelected(datePickerState.selectedDateMillis)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDatePickerDialogDismissed() }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(uiState.profileUploadError) {
        uiState.profileUploadError?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(message = error)
                viewModel.clearProfileUploadError()
            }
        }
    }

    LaunchedEffect(uiState.profileSaveSuccessMessage) {
        uiState.profileSaveSuccessMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message = message)
                viewModel.clearProfileSaveSuccessMessage()
            }
        }
    }

    LaunchedEffect(uiState.profileSaveErrorMessage) {
        uiState.profileSaveErrorMessage?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(message = error)
                viewModel.clearProfileSaveErrorMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoadingUser) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(60.dp))
                }
            } else {
                uiState.user?.let { currentUser ->
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = currentUser.profilePictureUrl ?: R.drawable.ic_person_placeholder,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(128.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable(enabled = !uiState.isUploadingProfilePicture) {
                                    pickMediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.ic_person_placeholder),
                            error = painterResource(id = R.drawable.ic_person_placeholder)
                        )
                        if (uiState.isUploadingProfilePicture) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(40.dp)
                                    .align(Alignment.Center)
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
                                    contentDescription = "Editar foto de perfil",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = uiState.editableUsername,
                        onValueChange = viewModel::onUsernameChanged,
                        label = { Text("Nome de Usuário") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isSavingProfile
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.editableEmail,
                        onValueChange = viewModel::onEmailChanged,
                        label = { Text("E-mail") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isSavingProfile
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.editableBirthDate,
                        onValueChange = viewModel::onBirthDateTextChanged,
                        label = { Text("Data de Nascimento") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = viewModel::onBirthDateClicked
                            ),
                        singleLine = true,
                        enabled = !uiState.isSavingProfile,
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = "Selecionar data",
                                modifier = Modifier.clickable(onClick = viewModel::onBirthDateClicked)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedStatusDropdown,
                            onExpandedChange = { expandedStatusDropdown = !expandedStatusDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.editableStatus.ifEmpty { "Disponível" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatusDropdown)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                enabled = !uiState.isSavingProfile
                            )
                            ExposedDropdownMenu(
                                expanded = expandedStatusDropdown,
                                onDismissRequest = { expandedStatusDropdown = false }
                            ) {
                                userSelectableStatusOptions.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            viewModel.onStatusChanged(selectionOption)
                                            expandedStatusDropdown = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Telefone: ${currentUser.phone ?: "Não informado"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (uiState.isSavingProfile) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = { viewModel.saveProfile() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoadingUser
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Salvar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salvar Alterações")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                } ?: run {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum usuário logado ou falha ao carregar o perfil.")
                    }
                }
            }

            if (!uiState.isLoadingUser) {
                Spacer(modifier = Modifier.weight(1f)) // Empurra o botão de logout para baixo
                Button(
                    onClick = {
                        viewModel.signOut()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    enabled = !uiState.isSavingProfile && !uiState.isUploadingProfilePicture
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sair")
                }
            }
        }
    }
}