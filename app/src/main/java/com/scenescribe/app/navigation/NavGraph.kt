package com.scenescribe.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scenescribe.app.data.TokenManager
import com.scenescribe.app.data.api.models.UserDto
import com.scenescribe.app.ui.auth.AuthScreen
import com.scenescribe.app.ui.feedback.FeedbackScreen
import com.scenescribe.app.ui.home.HomeScreen
import com.scenescribe.app.ui.profile.ProfileScreen
import com.scenescribe.app.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Home    : Screen("home",    "Home",    "🏠")
    object Profile : Screen("profile", "Profile", "👤")
}

private val bottomNavScreens = listOf(Screen.Home, Screen.Profile)

@Composable
fun SceneScribeNavGraph(
    tokenManager: TokenManager,
    initialUser: UserDto?
) {
    var currentUser by remember { mutableStateOf(initialUser) }
    val navController = rememberNavController()
    val startDest = if (currentUser != null) Screen.Home.route else "auth"

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val showBottomBar = currentUser != null &&
            currentRoute in bottomNavScreens.map { it.route }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = CardBackground,
                    tonalElevation = 0.dp
                ) {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Text(screen.icon, fontSize = 20.sp)
                            },
                            label = {
                                Text(
                                    screen.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Accent,
                                selectedTextColor   = Accent,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor      = Accent.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDest
            ) {
                composable("auth") {
                    AuthScreen(
                        onAuthenticated = { user, token ->
                            if (token.isNotBlank()) {
                                tokenManager.saveAuth(token, user)
                            }
                            currentUser = tokenManager.getUser() ?: user
                            navController.navigate(Screen.Home.route) {
                                popUpTo("auth") { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToFeedback = { id ->
                            navController.navigate("feedback/$id")
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateToFeedback = { id ->
                            navController.navigate("feedback/$id")
                        }
                    )
                }

                composable(
                    route = "feedback/{submissionId}",
                    arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("submissionId") ?: return@composable
                    FeedbackScreen(
                        submissionId = id,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
