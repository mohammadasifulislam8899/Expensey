package com.xentoryx.expensey.feature.auth.presentation.login

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xentoryx.expensey.R
import com.xentoryx.expensey.app.ui.theme.*
import com.xentoryx.expensey.core.presentation.components.*
import com.xentoryx.expensey.core.presentation.util.ObserveAsEvents
import com.xentoryx.expensey.core.presentation.util.toUserMessage
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToVerifyEmail: (userId: String, email: String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // ✅ Recomposition optimized validation checking
    val isFormValid by remember {
        derivedStateOf {
            state.email.isNotBlank() && state.password.isNotBlank()
        }
    }

    // ✅ Scroll করার জন্য state
    LaunchedEffect(state.email, state.password) {
        if (state.email.isNotEmpty() || state.password.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is LoginEffect.NavigateToHome -> onNavigateToHome()
            is LoginEffect.NavigateToRegister -> onNavigateToRegister()
            is LoginEffect.NavigateToForgotPassword -> onNavigateToForgotPassword()
            is LoginEffect.NavigateToVerifyEmail -> onNavigateToVerifyEmail(effect.userId, effect.email)
            is LoginEffect.ShowError -> {
                scope.launch {
                    snackbarHostState.showSnackbar(effect.error.toUserMessage(context))
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "CrushAmbient")
    val characterYOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CharacterFloating"
    )

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val showCharacter = screenHeight > 580.dp
    val characterHeight = if (screenHeight < 720.dp) 160.dp else 280.dp
    val circleSize = if (screenHeight < 720.dp) 130.dp else 220.dp
    val characterPadding = if (screenHeight < 720.dp) 6.dp else 12.dp

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = CrushBg,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CrushCanvasDecoration(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .windowInsetsPadding(WindowInsets.systemBars) // ✅ System bars padding
                    .imePadding() // ✅ IME (keyboard) padding
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 24.dp), // ✅ Extra bottom padding
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Giant Character Illustration (Responsive sizing & visibility)
                if (showCharacter) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer { translationY = characterYOffset }
                            .fillMaxWidth()
                            .height(characterHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(circleSize)
                                .background(CrushLavender.copy(alpha = 0.05f), CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = CrushLavender.copy(alpha = 0.18f),
                                    shape = CircleShape
                                )
                        )

                        Image(
                            painter = painterResource(R.drawable.expend),
                            contentDescription = "Active Gym Character",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(characterPadding),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Headlines
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Set Your Goal,",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp,
                            color = CrushTextPrimary,
                            fontSize = 26.sp
                        )
                    )

                    Text(
                        text = "Crush Your Limit!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp,
                            color = CrushTextPrimary,
                            fontSize = 26.sp
                        ),
                        modifier = Modifier.drawBehind {
                            val strokeWidth = 3.5.dp.toPx()
                            val y = size.height + 4.dp.toPx()
                            val path = Path().apply {
                                moveTo(size.width * 0.02f, y)
                                quadraticTo(
                                    size.width * 0.5f, y + 5.dp.toPx(),
                                    size.width * 0.98f, y - 2.dp.toPx()
                                )
                            }
                            drawPath(
                                path = path,
                                color = CrushYellow,
                                style = Stroke(
                                    width = strokeWidth,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 7f), 0f)
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sign in to your OptiSpend account",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                        color = CrushTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Form Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CrushOutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                        label = "Email Address",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = CrushLavender
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CrushOutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                        label = "Password",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = CrushLavender
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.onEvent(LoginEvent.TogglePasswordVisibility) }
                            ) {
                                Icon(
                                    if (state.isPasswordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = "Toggle password",
                                    tint = CrushTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (state.isPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.onEvent(LoginEvent.LoginClicked)
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CrushLavender,
                            letterSpacing = 0.1.sp
                        ),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 4.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.onEvent(LoginEvent.ForgotPasswordClicked) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    CrushActionButton(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onEvent(LoginEvent.LoginClicked)
                        },
                        enabled = isFormValid && !state.isLoading,
                        isLoading = state.isLoading,
                        text = "Sign In"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Don't have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrushTextSecondary
                    )
                    Text(
                        text = "Sign Up",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CrushLavender,
                            letterSpacing = 0.1.sp
                        ),
                        modifier = Modifier.clickable {
                            viewModel.onEvent(LoginEvent.RegisterClicked)
                        }
                    )
                }

                // ✅ Bottom extra space for keyboard
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}