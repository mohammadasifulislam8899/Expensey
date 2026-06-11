package com.xentoryx.expensey.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.xentoryx.expensey.feature.auth.presentation.forgot_password.ForgotPasswordScreen
import com.xentoryx.expensey.feature.auth.presentation.login.LoginScreen
import com.xentoryx.expensey.feature.auth.presentation.register.RegisterScreen
import com.xentoryx.expensey.feature.auth.presentation.reset_password.ResetPasswordScreen
import com.xentoryx.expensey.feature.auth.presentation.verify_email.VerifyEmailScreen
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.authNavGraph(
    navController: NavController,
    onAuthSuccess: () -> Unit
) {
    navigation<AuthGraphRoute>(startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(
                viewModel = koinViewModel(),
                onNavigateToHome = onAuthSuccess,
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(ForgotPasswordRoute)
                },
                onNavigateToVerifyEmail = { userId, email ->
                    navController.navigate(VerifyEmailRoute(userId, email))
                }
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                viewModel = koinViewModel(),
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToVerifyEmail = { userId, email ->
                    navController.navigate(VerifyEmailRoute(userId, email)) {
                        popUpTo<LoginRoute> { inclusive = false }
                    }
                }
            )
        }
        composable<VerifyEmailRoute> {
            VerifyEmailScreen(
                viewModel = koinViewModel(),
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                }
            )
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                viewModel = koinViewModel(),
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable<ResetPasswordRoute> {
            ResetPasswordScreen(
                viewModel = koinViewModel(),
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                }
            )
        }
    }
}