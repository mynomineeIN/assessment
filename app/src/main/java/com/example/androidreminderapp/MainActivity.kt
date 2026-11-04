package com.example.androidreminderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.androidreminderapp.ui.auth.AuthSessionState
import com.example.androidreminderapp.ui.auth.AuthViewModel
import com.example.androidreminderapp.ui.auth.LoginScreen
import com.example.androidreminderapp.ui.home.HomeScreen
import com.example.androidreminderapp.ui.home.HomeViewModel
import com.example.androidreminderapp.ui.reminder.AddEditReminderScreen
import com.example.androidreminderapp.ui.theme.AndroidReminderAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AndroidReminderAppTheme {
                val viewModel: AuthViewModel = hiltViewModel()
                val sessionState by viewModel.sessionState.collectAsState()
                val navController = rememberNavController()

                LaunchedEffect(sessionState) {
                    when (sessionState) {
                        AuthSessionState.Authenticated -> {
                            navController.navigate("home") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        AuthSessionState.Unauthenticated -> {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        AuthSessionState.Loading -> {
                            // Stay on loading / splash
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (sessionState) {
                        AuthSessionState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        else -> {
                            val startDest = if (sessionState is AuthSessionState.Authenticated) "home" else "login"
                            NavHost(
                                navController = navController,
                                startDestination = startDest
                            ) {
                                composable("login") {
                                    LoginScreen(
                                        onLoginSuccess = {
                                            navController.navigate("home") {
                                                popUpTo("login") { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }

                                composable("home") {
                                    HomeScreen(
                                        viewModel = hiltViewModel<HomeViewModel>(),
                                        onAddReminder = {
                                            navController.navigate("add_reminder")
                                        },
                                        onEditReminder = { reminderId ->
                                            navController.navigate("add_reminder/$reminderId")
                                        },
                                        onLogout = {
                                            viewModel.signOut()
                                        }
                                    )
                                }

                                composable("add_reminder") {
                                    AddEditReminderScreen(
                                        reminderId = null,
                                        viewModel = hiltViewModel(),
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                composable(
                                    route = "add_reminder/{reminderId}",
                                    arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
                                ) { backStackEntry ->
                                    val reminderId = backStackEntry.arguments?.getLong("reminderId")
                                    AddEditReminderScreen(
                                        reminderId = reminderId,
                                        viewModel = hiltViewModel(),
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
