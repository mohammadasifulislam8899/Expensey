package com.xentoryx.expensey.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.xentoryx.expensey.core.presentation.util.BiometricPromptManager
import com.xentoryx.expensey.app.ui.theme.CrushTextPrimary
import com.xentoryx.expensey.app.ui.theme.CrushTextSecondary
import com.xentoryx.expensey.core.presentation.components.CrushCanvasDecoration
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

@Serializable object DashboardRoute
@Serializable object TransactionsRoute
@Serializable object AccountsRoute
@Serializable object BudgetsRoute
@Serializable object SettingsRoute
@Serializable data class AddTransactionRoute(val transactionId: String? = null)
@Serializable object PdfExportRoute

// ─── Root NavHost ─────────────────────────────────────────────────────────────

@Composable
fun RootNavigation(
    navController: NavHostController,
    tokenManager: TokenManager = koinInject()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricPromptManager = remember(activity) { activity?.let { BiometricPromptManager(it) } }

    var isLoggedInState by remember { mutableStateOf<Boolean?>(null) }
    var isBiometricVerified by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        tokenManager.isLoggedIn.collect { loggedIn ->
            isLoggedInState = loggedIn
            if (loggedIn) {
                val bioEnabled = tokenManager.biometricEnabled.first()
                if (bioEnabled && biometricPromptManager?.isBiometricAvailable() == true) {
                    isBiometricVerified = isBiometricVerified ?: false
                } else {
                    isBiometricVerified = true
                }
            } else {
                isBiometricVerified = true
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                val parentRoute = navController.currentBackStackEntry?.destination?.parent?.route
                val authRouteName = AuthGraphRoute::class.qualifiedName
                if (currentRoute != null && currentRoute != authRouteName && parentRoute != authRouteName) {
                    navController.navigate(AuthGraphRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    fun triggerBiometricUnlock() {
        biometricPromptManager?.showBiometricPrompt(
            title = "Unlock Expensey",
            description = "Verify your identity to access your personal finance tracker.",
            onSuccess = {
                isBiometricVerified = true
            },
            onError = { _, _ -> },
            onFailed = {}
        )
    }

    // Auto-trigger biometric prompt if lock screen is visible
    LaunchedEffect(isBiometricVerified) {
        if (isBiometricVerified == false) {
            triggerBiometricUnlock()
        }
    }

    if (isLoggedInState == null || isBiometricVerified == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CrushBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CrushLavender)
        }
    } else if (isLoggedInState == true && isBiometricVerified == false) {
        BiometricLockScreen(
            onUnlockClick = { triggerBiometricUnlock() }
        )
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

@Composable
fun BiometricLockScreen(
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrushBg),
        contentAlignment = Alignment.Center
    ) {
        CrushCanvasDecoration(modifier = Modifier.fillMaxSize())
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(CrushLavender.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = CrushLavender,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                text = "Expensey Locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CrushTextPrimary
            )
            Text(
                text = "Verify your identity to access your transactions.",
                fontSize = 14.sp,
                color = CrushTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(containerColor = CrushLavender),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Unlock with Biometrics", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}