// ui/screens/MediaViewScreen.kt
package com.isacsilva.ufumessenger.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.isacsilva.ufumessenger.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewScreen(
    navController: NavController,
    mediaType: String,
    mediaUrl: String
) {
    Log.d("MediaViewScreen", "Attempting to load media. Type: $mediaType, URL: $mediaUrl")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mediaType == "video") "Vídeo" else "Imagem") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (mediaType) {
                "image" -> {
                    Log.d("MediaViewScreen", "Displaying image from URL: $mediaUrl")
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(mediaUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_broken_image_placeholder)
                            .build(),
                        contentDescription = "Visualização de Imagem",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                "video" -> {
                    Log.d("MediaViewScreen", "Setting up video player for URL: $mediaUrl")
                    val context = LocalContext.current

                    val exoPlayer = remember(context, mediaUrl) {
                        ExoPlayer.Builder(context).build().apply {
                            val mediaItem = MediaItem.fromUri(mediaUrl)
                            setMediaItem(mediaItem)
                            prepare()

                        }
                    }

                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(key1 = lifecycleOwner, key2 = exoPlayer) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_PAUSE -> {
                                    exoPlayer.pause()
                                }
                                Lifecycle.Event.ON_RESUME -> {

                                }
                                Lifecycle.Event.ON_DESTROY -> {

                                }
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)

                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            exoPlayer.release()
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer

                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text("Tipo de mídia desconhecido: $mediaType")
                }
            }
        }
    }
}
