package com.xentoryx.expensey.feature.auth.presentation.register

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.res.painterResource
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
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit,
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
            state.fullName.isNotBlank() &&
            state.email.isNotBlank() &&
            state.password.isNotBlank() &&
            state.confirmPassword.isNotBlank() &&
            state.password == state.confirmPassword
        }
    }

    // ✅ Auto-scroll on content input
    LaunchedEffect(state.fullName, state.email, state.password, state.confirmPassword) {
        if (state.fullName.isNotEmpty() || state.email.isNotEmpty() || state.password.isNotEmpty() || state.confirmPassword.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is RegisterEffect.NavigateToLogin -> onNavigateToLogin()
            is RegisterEffect.NavigateToVerifyEmail -> onNavigateToVerifyEmail(
                effect.userId,
                effect.email
            )
            is RegisterEffect.ShowError -> scope.launch {
                snackbarHostState.showSnackbar(effect.error.toUserMessage(context))
            }
            is RegisterEffect.ShowSuccess -> scope.launch {
                snackbarHostState.showSnackbar(effect.message)
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
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Floating Gym Character
                Box(
                    modifier = Modifier
                        .graphicsLayer { translationY = characterYOffset }
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
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
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Headlines
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Create Account,",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp,
                            color = CrushTextPrimary,
                            fontSize = 26.sp
                        )
                    )

                    Text(
                        text = "Start Today!",
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
                    text = "Start managing your finances with OptiSpend",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                        color = CrushTextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Register Form Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CrushOutlinedTextField(
                        value = state.fullName,
                        onValueChange = { viewModel.onEvent(RegisterEvent.FullNameChanged(it)) },
                        label = "Full Name",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = CrushLavender
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CrushOutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.onEvent(RegisterEvent.EmailChanged(it)) },
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
                        onValueChange = { viewModel.onEvent(RegisterEvent.PasswordChanged(it)) },
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
                                onClick = { viewModel.onEvent(RegisterEvent.TogglePasswordVisibility) }
                            ) {
                                Icon(
                                    if (state.isPasswordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
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
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CrushOutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = { viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
                        label = "Confirm Password",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = CrushLavender
                            )
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
                                viewModel.onEvent(RegisterEvent.RegisterClicked)
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    CrushActionButton(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onEvent(RegisterEvent.RegisterClicked)
                        },
                        enabled = isFormValid && !state.isLoading,
                        isLoading = state.isLoading,
                        text = "Create Account"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrushTextSecondary
                    )
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CrushLavender,
                            letterSpacing = 0.1.sp
                        ),
                        modifier = Modifier.clickable {
                            viewModel.onEvent(RegisterEvent.LoginClicked)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}