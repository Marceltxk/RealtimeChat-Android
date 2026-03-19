package com.isacsilva.ufumessenger.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.isacsilva.ufumessenger.R
import com.isacsilva.ufumessenger.domain.model.Conversation
import com.isacsilva.ufumessenger.domain.model.Message
import com.isacsilva.ufumessenger.domain.model.MessageStatus
import com.isacsilva.ufumessenger.domain.model.MessageType
import com.isacsilva.ufumessenger.domain.model.User
import com.isacsilva.ufumessenger.navigation.Screen
import com.isacsilva.ufumessenger.ui.viewmodel.ChatViewModel
import com.isacsilva.ufumessenger.ui.viewmodel.GroupActionState
import com.isacsilva.ufumessenger.util.DateFormatter
import com.isacsilva.ufumessenger.util.rememberFormattedUserStatus
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isSearchVisible by remember { mutableStateOf(false) }

    val conversation = uiState.conversationDetails?.conversation
    val pinnedMessageId = conversation?.pinnedMessageId
    val actualConversationId = conversation?.id

    var videoUriForPreview by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val currentUserId = Firebase.auth.currentUser?.uid
    val isParticipant = remember(uiState.conversationDetails) {
        val participants = uiState.conversationDetails?.conversation?.participants ?: emptyList()
        currentUserId in participants
    }

    val groupActionState by viewModel.groupActionState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- LAUNCHER DA CÂMERA (Tirar a foto) ---
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempPhotoUri != null && actualConversationId != null) {
                viewModel.sendImageMessage(tempPhotoUri!!, actualConversationId, text.ifBlank { null })
                text = ""
                tempPhotoUri = null
            } else {
                Log.d("ChatScreen", "Falha ao capturar imagem ou cancelado.")
            }
        }
    )

    // --- LAUNCHER DE PERMISSÃO DA CÂMERA ---
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val photoFile = File(context.cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                tempPhotoUri = uri
                cameraLauncher.launch(uri)
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar("Permissão de Câmera negada") }
            }
        }
    )

    // --- LAUNCHER DO VÍDEO (Mantido como extra) ---
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                videoUriForPreview = uri
            }
        }
    )


    // --- LAUNCHER DO GPS ---
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                com.isacsilva.ufumessenger.util.getCurrentLocation(
                    context = context,
                    onLocationFetched = { lat, lng ->
                        viewModel.sendLocationMessage(lat, lng, "Localização Atual")
                        coroutineScope.launch { snackbarHostState.showSnackbar("Localização enviada!") }
                    },
                    onError = { e ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(e.message ?: "Erro ao obter GPS") }
                    }
                )
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar("Permissão de GPS negada") }
            }
        }
    )

    // --- GRAVADOR E LAUNCHER DO MICROFONE ---
    val audioRecorder = remember { com.isacsilva.ufumessenger.util.AudioRecorderHelper(context) }
    var isRecording by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Se o usuário permitiu, já começamos a gravar na hora!
                isRecording = true
                audioRecorder.startRecording()
                coroutineScope.launch { snackbarHostState.showSnackbar("Gravando áudio... Clique no microfone para parar e enviar.") }
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar("Permissão de Microfone negada") }
            }
        }
    )

    LaunchedEffect(uiState.messages.size, uiState.searchQuery) {
        if (uiState.messages.isNotEmpty() && uiState.searchQuery.isBlank()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
            if (actualConversationId != null) {
                viewModel.markMessagesAsRead(actualConversationId)
            }
        }
    }

    LaunchedEffect(groupActionState) {
        when (val state = groupActionState) {
            is GroupActionState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetGroupActionState()
            }
            is GroupActionState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetGroupActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearchVisible) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClose = {
                        isSearchVisible = false
                        viewModel.onSearchQueryChange("")
                    }
                )
            } else {
                ChatTopAppBar(
                    navController = navController,
                    conversation = conversation,
                    otherParticipant = uiState.conversationDetails?.otherParticipant,
                    onSearchClick = { isSearchVisible = true },
                    onEditGroupClick = {
                        if (conversation?.isGroup == true && actualConversationId != null && isParticipant) {
                            navController.navigate(Screen.EditGroup.createRoute(actualConversationId))
                        }
                    },
                    onGroupImageChange = { uri ->
                        if (actualConversationId != null) {
                            viewModel.updateGroupImage(actualConversationId, uri)
                        }
                    }
                )
            }
        },
        bottomBar = {
            if(isParticipant) {
                MessageInput(
                    text = text,
                    onTextChange = { text = it },
                    onSendClick = {
                        val currentVideoPreviewUri = videoUriForPreview
                        if (currentVideoPreviewUri != null) {
                            if (actualConversationId != null) {
                                viewModel.sendVideoMessage(currentVideoPreviewUri, actualConversationId, text.ifBlank { null })
                                videoUriForPreview = null
                                text = ""
                            }
                        } else if (text.isNotBlank()) {
                            viewModel.sendMessage(text)
                            text = ""
                        }
                    },
                    onCameraClick = {
                        // Ao clicar, pede a permissão da câmera primeiro. O launcher cuida do resto!
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onLocationClick = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    onMicClick = {
                        if (isRecording) {
                            // Se já estava gravando, o clique vai PARAR e ENVIAR
                            isRecording = false
                            val (uri, durationMs) = audioRecorder.stopRecording()

                            // Evita enviar áudios vazios ou cliques acidentais (menor que 1 segundo)
                            if (uri != null && durationMs > 1000) {
                                viewModel.sendAudioMessage(uri, durationMs)
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Áudio muito curto ou cancelado.") }
                            }
                        } else {
                            // Se não estava gravando, pede a permissão (que aciona o start)
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    isRecording = isRecording,
                    previewVideoUri = videoUriForPreview,
                    onRemovePreviewVideo = { videoUriForPreview = null },
                    onVideoAttachmentClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                )
            } else {
                RemovedUserMessageBar()
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PinnedMessageBar(
                conversation = conversation,
                onUnpin = {
                    if (isParticipant) {
                        viewModel.onPinMessage(null)
                    }
                },
                onClick = {
                    val index = uiState.messages.indexOfFirst { it.id == pinnedMessageId }
                    if (index != -1) {
                        coroutineScope.launch { listState.animateScrollToItem(index) }
                    }
                }
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    EncryptionBanner()
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val messagesToShow = if (uiState.searchQuery.isNotBlank()) uiState.filteredMessages else uiState.messages
                items(messagesToShow, key = { it.id }) { message ->
                    val sender = uiState.participantsDetails[message.senderId]
                    MessageBubble(
                        message = message,
                        sender = sender,
                        isGroupChat = conversation?.isGroup == true,
                        isPinned = message.id == pinnedMessageId,
                        onLongPress = {
                            if (isParticipant) {
                                viewModel.onPinMessage(message)
                            }
                        },
                        navController = navController,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun RemovedUserMessageBar() {
    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Você não é mais participante deste grupo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatDuration(millis: Long?): String {
    if (millis == null || millis <= 0) return ""
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopAppBar(
    navController: NavController,
    conversation: Conversation?,
    otherParticipant: User?,
    onSearchClick: () -> Unit,
    onEditGroupClick: () -> Unit,
    onGroupImageChange: (Uri) -> Unit
) {
    val currentUserId = Firebase.auth.currentUser?.uid
    val isParticipant = remember(conversation) {
        val participants = conversation?.participants ?: emptyList()
        currentUserId in participants
    }

    var showImagePicker by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? -> uri?.let { onGroupImageChange(it) } }
    )

    val userStatus = rememberFormattedUserStatus(otherParticipant)

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    enabled = conversation?.isGroup == false && otherParticipant != null,
                    onClick = {
                        otherParticipant?.uid?.let { userId ->
                            if (userId.isNotBlank()) navController.navigate(Screen.OtherUserProfile.createRoute(userId))
                        }
                    }
                )
            ) {
                if (conversation?.isGroup == true) {
                    AsyncImage(
                        model = conversation.groupImageUrl?.ifEmpty { R.drawable.ic_group_placeholder } ?: R.drawable.ic_group_placeholder,
                        contentDescription = "Imagem do Grupo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(enabled = isParticipant) { showImagePicker = true },
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_group_placeholder),
                        error = painterResource(id = R.drawable.ic_group_placeholder)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = conversation.groupName ?: "Grupo",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    AsyncImage(
                        model = otherParticipant?.profilePictureUrl?.ifEmpty { R.drawable.ic_person_placeholder },
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_person_placeholder),
                        error = painterResource(id = R.drawable.ic_person_placeholder)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = otherParticipant?.username ?: "Carregando...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (userStatus.isNotBlank()) {
                            Text(
                                text = userStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (userStatus == "Online") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Buscar Mensagens")
            }
            if (conversation?.isGroup == true && isParticipant) {
                IconButton(onClick = onEditGroupClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Editar Grupo")
                }
            }
        }
    )

    if (showImagePicker) {
        LaunchedEffect(showImagePicker) {
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            showImagePicker = false
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    sender: User?,
    isGroupChat: Boolean,
    isPinned: Boolean,
    onLongPress: () -> Unit,
    navController: NavController,
    context: android.content.Context
) {
    val currentUserId = Firebase.auth.currentUser?.uid
    val isSentByCurrentUser = message.senderId == currentUserId

    val bubbleColor = if (isPinned) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else if (isSentByCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isSentByCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isGroupChat && !isSentByCurrentUser) {
            AsyncImage(
                model = sender?.profilePictureUrl?.ifEmpty { R.drawable.ic_person_placeholder },
                contentDescription = "Foto de perfil",
                modifier = Modifier.size(32.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_person_placeholder),
                error = painterResource(id = R.drawable.ic_person_placeholder)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isSentByCurrentUser) Alignment.End else Alignment.Start) {
            if (isGroupChat && !isSentByCurrentUser && sender != null) {
                Text(
                    text = sender.username ?: "Utilizador",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isSentByCurrentUser || (isGroupChat && !isSentByCurrentUser)) 16.dp else 0.dp,
                            topEnd = if (!isSentByCurrentUser || (isGroupChat && isSentByCurrentUser)) 16.dp else 0.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = {
                            if (message.mediaUrl != null) {
                                when (message.type) {
                                    MessageType.IMAGE -> navController.navigate(Screen.MediaView.createRoute("image", message.mediaUrl!!))
                                    MessageType.VIDEO -> navController.navigate(Screen.MediaView.createRoute("video", message.mediaUrl!!))
                                }
                            }
                        },
                        onLongClick = onLongPress,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    )
            ) {
                when (message.type) {
                    MessageType.IMAGE -> {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(message.mediaUrl)
                                    .crossfade(true)
                                    .placeholder(R.drawable.ic_image_placeholder)
                                    .error(R.drawable.ic_broken_image_placeholder)
                                    .build(),
                                contentDescription = message.fileName ?: "Imagem",
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            val caption = if (!message.text.isNullOrBlank() && message.text != MessageType.IMAGE_LABEL) message.text else null
                            if (caption != null) {
                                Text(text = caption, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                            }
                            MessageMetadataRow(message, isSentByCurrentUser, isPinned)
                        }
                    }
                    MessageType.VIDEO -> {
                        Column(
                            modifier = if (!message.text.isNullOrBlank() && message.text != MessageType.VIDEO_LABEL)
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            else Modifier
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(message.thumbnailUrl ?: R.drawable.ic_video_placeholder)
                                        .crossfade(true)
                                        .placeholder(R.drawable.ic_video_placeholder)
                                        .error(R.drawable.ic_video_placeholder)
                                        .build(),
                                    contentDescription = "Miniatura",
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Reproduzir",
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                        .padding(8.dp),
                                    tint = Color.White
                                )
                            }
                            val caption = if (!message.text.isNullOrBlank() && message.text != MessageType.VIDEO_LABEL) message.text else null
                            if (caption != null) {
                                Text(text = caption, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                            }
                            MessageMetadataRow(
                                message, isSentByCurrentUser, isPinned,
                                modifier = if (caption == null && (message.text == MessageType.VIDEO_LABEL || message.text.isNullOrBlank()))
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp) else Modifier
                            )
                        }
                    }
                    MessageType.LOCATION -> {
                        Column(modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                val gmmIntentUri = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                context.startActivity(mapIntent)
                            }
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(60.dp).align(Alignment.CenterHorizontally), tint = MaterialTheme.colorScheme.primary)
                            Text(message.text ?: "Ver Localização", style = MaterialTheme.typography.labelMedium)
                            MessageMetadataRow(message, isSentByCurrentUser, isPinned)
                        }
                    }
                    MessageType.AUDIO -> {
                        // Estados para controlar o botão e o download
                        var isPlaying by remember { mutableStateOf(false) }
                        var isPrepared by remember { mutableStateOf(false) }
                        val mediaPlayer = remember { android.media.MediaPlayer() }

                        DisposableEffect(message.mediaUrl) {
                            onDispose {
                                if (isPlaying) mediaPlayer.stop()
                                mediaPlayer.release()
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            IconButton(onClick = {
                                if (isPlaying) {
                                    mediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    try {
                                        if (isPrepared) {
                                            // Se já preparou antes, só solta o play de onde parou!
                                            mediaPlayer.start()
                                            isPlaying = true
                                        } else {
                                            mediaPlayer.reset()

                                            // Avisa o Android que isso é uma mídia (importante para o volume do celular)
                                            mediaPlayer.setAudioAttributes(
                                                android.media.AudioAttributes.Builder()
                                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                                    .build()
                                            )

                                            mediaPlayer.setDataSource(message.mediaUrl)

                                            // O ouvinte sempre VEM ANTES de chamar o prepareAsync
                                            mediaPlayer.setOnPreparedListener { mp ->
                                                isPrepared = true
                                                mp.start()
                                                isPlaying = true
                                            }

                                            mediaPlayer.setOnCompletionListener { mp ->
                                                isPlaying = false
                                                mp.seekTo(0) // Volta pro começo quando o áudio terminar
                                            }

                                            mediaPlayer.setOnErrorListener { _, what, extra ->
                                                Log.e("AudioPlayer", "Erro do MediaPlayer: $what, $extra")
                                                isPlaying = false
                                                isPrepared = false
                                                true
                                            }

                                            mediaPlayer.prepareAsync() // Agora sim, manda baixar e preparar!
                                        }
                                    } catch (e: Exception) {
                                        Log.e("AudioPlayer", "Erro crítico no botão de áudio", e)
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar" else "Reproduzir"
                                )
                            }
                            Text("Mensagem de Voz (${formatDuration(message.duration)})")
                            MessageMetadataRow(message, isSentByCurrentUser, isPinned)
                        }
                    }
                    else -> { // TEXTO
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(text = message.text ?: "", modifier = Modifier.weight(1f, fill = false))
                            MessageMetadataRow(message, isSentByCurrentUser, isPinned, isText = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageMetadataRow(
    message: Message,
    isSentByCurrentUser: Boolean,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    isText: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isText) Arrangement.End else Arrangement.Start,
        modifier = modifier.then(
            if (!isText) Modifier.fillMaxWidth().padding(top = 4.dp)
            else Modifier.padding(start = 8.dp)
        )
    ) {
        if (isPinned) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = "Mensagem fixada",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = DateFormatter.formatFullTimestamp(message.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        if (isSentByCurrentUser) {
            Spacer(modifier = Modifier.width(4.dp))
            MessageStatusIcon(status = message.status)
        }
    }
}

@Composable
fun MessageStatusIcon(status: String) {
    val icon = when (status) {
        MessageStatus.SENT -> Icons.Default.Done
        MessageStatus.DELIVERED -> Icons.Default.DoneAll
        MessageStatus.READ -> Icons.Filled.DoneAll
        else -> null
    }
    val iconColor = if (status == MessageStatus.READ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    icon?.let {
        Icon(imageVector = it, contentDescription = "Status da mensagem", tint = iconColor, modifier = Modifier.size(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 4.dp) {
        Row(modifier = Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fechar") }
            TextField(
                value = query, onValueChange = onQueryChange, modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar mensagens...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent, cursorColor = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1, singleLine = true
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, contentDescription = "Limpar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PinnedMessageBar(conversation: Conversation?, onUnpin: () -> Unit, onClick: () -> Unit) {
    val pinnedMessageText = conversation?.pinnedMessageText
    AnimatedVisibility(visible = pinnedMessageText != null) {
        if (pinnedMessageText != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 2.dp
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PushPin, contentDescription = "Fixada", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = pinnedMessageText, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onUnpin, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Desafixar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun EncryptionBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFFFEF6C6),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Criptografia",
                    modifier = Modifier.size(14.dp),
                    tint = Color.DarkGray
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "As mensagens desta conversa são protegidas com criptografia de ponta a ponta.",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onLocationClick: () -> Unit,
    onMicClick: () -> Unit,
    isRecording: Boolean,
    previewVideoUri: Uri?,
    onRemovePreviewVideo: () -> Unit,
    onVideoAttachmentClick: () -> Unit
) {
    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Column {
            if (previewVideoUri != null) {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).fillMaxWidth().height(100.dp)) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Vídeo", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Vídeo selecionado: ${previewVideoUri.lastPathSegment}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onRemovePreviewVideo, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape)) {
                        Icon(Icons.Default.Clear, contentDescription = "Remover", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                // OS TRÊS BOTÕES DE HARDWARE EXIGIDOS
                IconButton(onClick = onCameraClick) { Icon(Icons.Filled.PhotoCamera, contentDescription = "Câmera") }
                IconButton(onClick = onLocationClick) { Icon(Icons.Filled.LocationOn, contentDescription = "GPS") }
                IconButton(onClick = onMicClick) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = "Microfone",
                        tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = text, onValueChange = onTextChange, modifier = Modifier.weight(1f),
                    placeholder = { Text(if (previewVideoUri != null) "Adicionar legenda..." else "Digite uma mensagem...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent, cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp), maxLines = 5
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSendClick,
                    enabled = text.isNotBlank() || previewVideoUri != null,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary, disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}