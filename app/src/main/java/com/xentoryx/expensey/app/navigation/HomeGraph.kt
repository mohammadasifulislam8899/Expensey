package com.xentoryx.expensey.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xentoryx.expensey.app.ui.theme.CrushBg
import com.xentoryx.expensey.app.ui.theme.CrushLavender
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.auth.navigation.AuthGraphRoute
import com.xentoryx.expensey.feature.auth.navigation.authNavGraph
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

// ─── Root Route Definitions ──────────────────────────────────────────────────

@Serializable object HomeGraph

// ─── Root NavHost ─────────────────────────────────────────────────────────────

@Composable
fun RootNavigation(
    navController: NavHostController,
    tokenManager: TokenManager = koinInject()
) {
    var isLoggedInState by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        isLoggedInState = tokenManager.isLoggedIn.first()
    }

    if (isLoggedInState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CrushBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CrushLavender)
        }
    } else {
        val startDestination = if (isLoggedInState == true) HomeGraph else AuthGraphRoute

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // ── Auth nested graph ─────────────────────────────────────────────
            authNavGraph(
                navController = navController,
                onAuthSuccess = {
                    navController.navigate(HomeGraph) {
                        popUpTo<AuthGraphRoute> { inclusive = true }
                    }
                }
            )

            // ── Home ──────────────────────────────────────────────────────────
            composable<HomeGraph> {
                HomeScreen(
                    onLogout = {
                        navController.navigate(AuthGraphRoute) {
                            popUpTo<HomeGraph> { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}