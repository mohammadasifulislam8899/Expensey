package com.xentoryx.expensey.feature.auth.presentation.verify_email

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
fun VerifyEmailScreen(
    viewModel: VerifyEmailViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
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
            state.otp.length == 6
        }
    }

    // ✅ Auto-scroll on content input
    LaunchedEffect(state.otp) {
        if (state.otp.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is VerifyEmailEffect.NavigateToLogin -> onNavigateToLogin()
            is VerifyEmailEffect.NavigateToHome -> onNavigateToHome()
            is VerifyEmailEffect.ShowError -> scope.launch {
                snackbarHostState.showSnackbar(effect.error.toUserMessage(context))
            }
            is VerifyEmailEffect.ShowSuccess -> scope.launch {
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
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 24.dp),
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
                        text = "Verify Email,",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.5.sp,
                            color = CrushTextPrimary,
                            fontSize = 26.sp
                        )
                    )

                    Text(
                        text = "Check Your Inbox!",
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
                    text = "We've sent a 6-digit code to\n${state.email}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                        color = CrushTextSecondary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // OTP Form Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CrushOutlinedTextField(
                        value = state.otp,
                        onValueChange = { viewModel.onEvent(VerifyEmailEvent.OtpChanged(it)) },
                        label = "Enter 6-digit OTP",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (state.otp.length == 6) {
                                    viewModel.onEvent(VerifyEmailEvent.VerifyClicked)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    CrushActionButton(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onEvent(VerifyEmailEvent.VerifyClicked)
                        },
                        enabled = isFormValid && !state.isLoading,
                        isLoading = state.isLoading,
                        text = "Verify"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Resend Section
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.canResend) {
                        Text(
                            text = "Resend OTP",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CrushLavender,
                                letterSpacing = 0.1.sp
                            ),
                            modifier = Modifier.clickable {
                                viewModel.onEvent(VerifyEmailEvent.ResendClicked)
                            }
                        )
                    } else {
                        Text(
                            text = "Resend OTP in ${state.resendCountdown}s",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = CrushTextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Back to Login",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CrushLavender,
                        letterSpacing = 0.1.sp
                    ),
                    modifier = Modifier.clickable {
                        onNavigateToLogin()
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}