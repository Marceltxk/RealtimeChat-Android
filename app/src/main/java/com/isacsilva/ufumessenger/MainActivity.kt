package com.isacsilva.ufumessenger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.isacsilva.ufumessenger.navigation.NavGraph
import com.isacsilva.ufumessenger.navigation.Screen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.isacsilva.ufumessenger.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Permissions", "Permissão de notificações concedida.")
        } else {
            Log.w("Permissions", "Permissão de notificações negada.")
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                viewModel.setUserOffline()
                Log.d("MainActivityLifecycle", "onStop: setUserOffline chamado")
            }

        })

        lifecycleScope.launch {
            lifecycle.whenStarted {
                Log.d("MainActivityLifecycle", "whenStarted: Bloco iniciado, coletando authState")
                viewModel.authState.collectLatest { authState ->
                    Log.d("MainActivityLifecycle", "whenStarted: authState coletado: user=${authState.user?.uid}, initialLoading=${authState.isInitialLoading}")
                    if (authState.user != null && !authState.isInitialLoading) { // Adicionada checagem de !isInitialLoading aqui também
                        viewModel.setUserOnline()
                        Log.d("MainActivityLifecycle", "whenStarted: Usuário logado e não carregando, setUserOnline chamado")
                    }
                }
            }
        }


        askNotificationPermission()

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.authState.value.isInitialLoading
            }
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()
                val authState by viewModel.authState.collectAsStateWithLifecycle()

                LaunchedEffect(authState.user, authState.isInitialLoading) {
                    if (authState.user == null && !authState.isInitialLoading) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    }
                }

                val startDestination = if (authState.isInitialLoading) {
                    Screen.Login.route
                } else {
                    if (authState.user != null) {
                        Screen.Home.route
                    } else {
                        Screen.Login.route
                    }
                }

                if (!authState.isInitialLoading) {
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
