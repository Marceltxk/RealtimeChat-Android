package com.isacsilva.ufumessenger.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.isacsilva.ufumessenger.ui.screens.AddParticipantsScreen
import com.isacsilva.ufumessenger.ui.screens.ChatScreen
import com.isacsilva.ufumessenger.ui.screens.CreateGroupScreen
import com.isacsilva.ufumessenger.ui.screens.EditGroupScreen
import com.isacsilva.ufumessenger.ui.screens.FinalizeGroupScreen
import com.isacsilva.ufumessenger.ui.screens.HomeScreen
import com.isacsilva.ufumessenger.ui.screens.LoginScreen
import com.isacsilva.ufumessenger.ui.screens.OtherUserProfileScreen
import com.isacsilva.ufumessenger.ui.screens.ProfileScreen
import com.isacsilva.ufumessenger.ui.screens.RegistrationScreen
import com.isacsilva.ufumessenger.ui.screens.UserSearchScreen
import com.isacsilva.ufumessenger.ui.screens.MediaViewScreen
import com.isacsilva.ufumessenger.ui.viewmodel.LoginViewModel
import com.isacsilva.ufumessenger.ui.viewmodel.RegistrationViewModel

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Home : Screen("home_screen")
    object Profile : Screen("profile_screen")
    object Chat : Screen("chat_screen/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_screen/$conversationId"
    }
    object UserSearch : Screen("user_search_screen")
    object Registration : Screen("registration_screen")

    object OtherUserProfile : Screen("other_user_profile_screen/{userId}") {
        fun createRoute(userId: String) = "other_user_profile_screen/$userId"
    }
    object CreateGroup : Screen("create_group_screen")
    object FinalizeGroup : Screen("finalize_group_screen/{memberIds}") {
        fun createRoute(memberIds: List<String>): String {
            val ids = memberIds.joinToString(",")
            return "finalize_group_screen/$ids"
        }
    }

    object EditGroup : Screen("edit_group/{conversationId}") {
        fun createRoute(conversationId: String) = "edit_group/$conversationId"
    }

    object AddParticipants : Screen("add_participants/{conversationId}") {
        fun createRoute(conversationId: String) = "add_participants/$conversationId"
    }

    object MediaView : Screen("media_view_screen/{mediaType}/{mediaUrl}") {
        // Argumentos da rota para MediaView
        const val ARG_MEDIA_TYPE = "mediaType"
        const val ARG_MEDIA_URL = "mediaUrl" // Este é o nome do parâmetro na string da rota

        fun createRoute(mediaType: String, mediaUrl: String): String {
            Log.d("NavGraphCreateRoute", "Original mediaUrl: $mediaUrl for navigation")
            val encodedUrl = URLEncoder.encode(mediaUrl, StandardCharsets.UTF_8.toString())
            return "media_view_screen/$mediaType/$encodedUrl"
        }
    }

}

@Composable
fun NavGraph(navController: NavHostController, startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()

            LoginScreen(
                uiState = uiState,
                onSendCodeClick = loginViewModel::startPhoneNumberVerification,
                onSignInClick = loginViewModel::signInWithCode,
                onSignUpClick = {
                    navController.navigate(Screen.Registration.route)
                },
                onErrorMessageShown = loginViewModel::onErrorMessageShown,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    loginViewModel.onLoginHandled()
                }
            )
        }

        composable(route = Screen.Registration.route) {
            val registrationViewModel: RegistrationViewModel = hiltViewModel()
            val uiState by registrationViewModel.uiState.collectAsStateWithLifecycle()

            RegistrationScreen(
                uiState = uiState,
                onSendCodeClick = registrationViewModel::startPhoneNumberVerification,
                onRegisterClick = registrationViewModel::signInWithCode,
                onRegistrationSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Registration.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    registrationViewModel.onRegistrationHandled()
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onErrorMessageShown = registrationViewModel::onErrorMessageShown
            )

        }

        composable(route = Screen.Profile.route) {
            // Este é o perfil do usuário LOGADO
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""

            ChatScreen(
                conversationId = conversationId, // ← Passe o conversationId aqui
                navController = navController
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }, // Perfil do usuário logado
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNewChatClick = {
                    navController.navigate(Screen.UserSearch.route)
                }
            )
        }

        composable(route = Screen.UserSearch.route) {
            UserSearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId)) {
                        popUpTo(Screen.UserSearch.route) {
                            inclusive = true
                        }
                    }
                },
                onNewGroupClick = {
                    navController.navigate(Screen.CreateGroup.route)
                }
            )
        }

        composable(route = Screen.CreateGroup.route) {
            CreateGroupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNextClick = { selectedContactIds ->
                    navController.navigate(Screen.FinalizeGroup.createRoute(selectedContactIds))
                }
            )
        }

        composable(
            route = Screen.FinalizeGroup.route,
            arguments = listOf(navArgument("memberIds") { type = NavType.StringType })
        ) {
            FinalizeGroupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGroupCreated = { groupId ->
                    navController.navigate(Screen.Chat.createRoute(groupId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.OtherUserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            OtherUserProfileScreen(navController = navController)
        }

        composable(
            route = Screen.MediaView.route,
            arguments = listOf(
                navArgument(Screen.MediaView.ARG_MEDIA_TYPE) { type = NavType.StringType },
                navArgument(Screen.MediaView.ARG_MEDIA_URL) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaType = backStackEntry.arguments?.getString(Screen.MediaView.ARG_MEDIA_TYPE)
            // A URL obtida dos argumentos já está no formato correto (decodificada uma vez pela navegação).
            // Não precisamos decodificá-la novamente aqui.
            val mediaUrlFromArgs = backStackEntry.arguments?.getString(Screen.MediaView.ARG_MEDIA_URL)

            if (mediaType != null && mediaUrlFromArgs != null) {
                // Log para mostrar a URL recebida dos argumentos e que será passada para a tela
                Log.d("NavGraph", "MediaView received URL from args: $mediaUrlFromArgs")

                // Passa a mediaUrlFromArgs diretamente para MediaViewScreen
                MediaViewScreen(navController = navController, mediaType = mediaType, mediaUrl = mediaUrlFromArgs)
            } else {
                // Lidar com o caso de argumentos nulos, talvez voltar ou mostrar um erro
                Log.e("NavGraph", "MediaView: Tipo de mídia ou URL (codificada) não encontrados nos argumentos.")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Erro: Argumentos da MediaView não encontrados.")
                }
            }
        }

        composable("edit_group/{conversationId}") { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            EditGroupScreen(conversationId, navController)
        }

        composable("add_participants/{conversationId}") { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            AddParticipantsScreen(conversationId, navController)
        }

    }
}
