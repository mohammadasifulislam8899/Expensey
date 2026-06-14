package com.xentoryx.expensey.app.navigation

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xentoryx.expensey.feature.accounts.presentation.list.AccountsListScreen
import com.xentoryx.expensey.feature.budget.presentation.list.BudgetsListScreen
import com.xentoryx.expensey.feature.auth.domain.usecase.LogoutUseCase
import com.xentoryx.expensey.feature.dashboard.presentation.dashboard.DashboardScreen
import com.xentoryx.expensey.feature.dashboard.presentation.settings.SettingsScreen
import com.xentoryx.expensey.feature.transaction.presentation.add.AddTransactionScreen
import com.xentoryx.expensey.feature.transaction.presentation.list.TransactionsListScreen
import com.xentoryx.expensey.feature.pdf_export.presentation.PdfExportScreen
import com.xentoryx.expensey.feature.category.domain.usecase.GetCategoriesUseCase
import com.xentoryx.expensey.feature.accounts.domain.usecase.GetAccountsUseCase
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    logoutUseCase: LogoutUseCase = koinInject(),
    getCategoriesUseCase: GetCategoriesUseCase = koinInject(),
    getAccountsUseCase: GetAccountsUseCase = koinInject()
) {
    val homeNavController = rememberNavController()
    val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isMenuExtended = remember { mutableStateOf(false) }

    val fabAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        )
    )

    val clickAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = LinearEasing
        )
    )

    val renderEffect = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurEffect = android.graphics.RenderEffect.createBlurEffect(80f, 80f, android.graphics.Shader.TileMode.MIRROR)
            val alphaMatrix = android.graphics.RenderEffect.createColorFilterEffect(
                android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix(
                        floatArrayOf(
                            1f, 0f, 0f, 0f, 0f,
                            0f, 1f, 0f, 0f, 0f,
                            0f, 0f, 1f, 0f, 0f,
                            0f, 0f, 0f, 50f, -5000f
                        )
                    )
                )
            )
            android.graphics.RenderEffect.createChainEffect(alphaMatrix, blurEffect).asComposeRenderEffect()
        } else {
            null
        }
    }

    // Determine bottom bar visibility dynamically
    val showBottomBar = remember(currentRoute) {
        currentRoute == DashboardRoute::class.qualifiedName ||
        currentRoute == TransactionsRoute::class.qualifiedName ||
        currentRoute == AccountsRoute::class.qualifiedName ||
        currentRoute == BudgetsRoute::class.qualifiedName
    }

    // Determine currently selected index based on active route
    val selectedTab = when (currentRoute) {
        DashboardRoute::class.qualifiedName -> 0
        TransactionsRoute::class.qualifiedName -> 1
        BudgetsRoute::class.qualifiedName -> 3
        AccountsRoute::class.qualifiedName -> 4
        else -> 0
    }

    LaunchedEffect(Unit) {
        launch {
            try {
                getCategoriesUseCase.sync()
            } catch (_: Exception) {}
        }
        launch {
            try {
                getAccountsUseCase.sync()
            } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    Surface(
                        color = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavigationBarItem(
                                modifier = Modifier.weight(1f),
                                selected = selectedTab == 0,
                                onClick = {
                                    homeNavController.navigate(DashboardRoute) {
                                        popUpTo(homeNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                modifier = Modifier.weight(1f),
                                selected = selectedTab == 1,
                                onClick = {
                                    homeNavController.navigate(TransactionsRoute) {
                                        popUpTo(homeNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Transactions") },
                                label = { Text("Transactions", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.weight(1.2f))

                            NavigationBarItem(
                                modifier = Modifier.weight(1f),
                                selected = selectedTab == 3,
                                onClick = {
                                    homeNavController.navigate(BudgetsRoute) {
                                        popUpTo(homeNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.PieChart, contentDescription = "Budgets") },
                                label = { Text("Budgets", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                modifier = Modifier.weight(1f),
                                selected = selectedTab == 4,
                                onClick = {
                                    homeNavController.navigate(AccountsRoute) {
                                        popUpTo(homeNavController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Accounts") },
                                label = { Text("Accounts", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            NavHost(
                navController = homeNavController,
                startDestination = DashboardRoute,
                modifier = Modifier.fillMaxSize()
            ) {
                composable<DashboardRoute> {
                    DashboardScreen(
                        viewModel = koinViewModel(),
                        onProfileClick = {
                            homeNavController.navigate(SettingsRoute)
                        }
                    )
                }
                composable<TransactionsRoute> {
                    TransactionsListScreen(
                        viewModel = koinViewModel(),
                        onAddTransactionClick = {
                            homeNavController.navigate(AddTransactionRoute(null))
                        },
                        onDownloadClick = {
                            homeNavController.navigate(PdfExportRoute)
                        },
                        onTransactionClick = { txn ->
                            homeNavController.navigate(AddTransactionRoute(txn.id))
                        }
                    )
                }
                composable<AccountsRoute> {
                    AccountsListScreen(
                        viewModel = koinViewModel()
                    )
                }
                composable<BudgetsRoute> {
                    BudgetsListScreen(
                        viewModel = koinViewModel()
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen(
                        onBackClick = {
                            homeNavController.popBackStack()
                        },
                        onLogoutClick = onLogout
                    )
                }
                composable<AddTransactionRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<AddTransactionRoute>()
                    AddTransactionScreen(
                        viewModel = koinViewModel(),
                        transactionId = route.transactionId,
                        transactionType = route.transactionType,
                        onBackClick = {
                            homeNavController.popBackStack()
                        }
                    )
                }
                composable<PdfExportRoute> {
                    PdfExportScreen(
                        viewModel = koinViewModel(),
                        onBackClick = {
                            homeNavController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    // Backdrop dimming overlay when expanded
    if (showBottomBar && isMenuExtended.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    isMenuExtended.value = false
                }
        )
    }

    // Central Fluid FAB Group Container floating on top of Scaffold
    if (showBottomBar) {
        val showExpandedMenu = isMenuExtended.value || fabAnimationProgress > 0f
        val boxWidth = if (showExpandedMenu) 260.dp else 76.dp
        val boxHeight = if (showExpandedMenu) 200.dp else 76.dp

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = boxWidth, height = boxHeight)
                .offset(y = (-24).dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Circle(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                animationProgress = 0.5f
            )

            FabGroup(
                animationProgress = fabAnimationProgress,
                renderEffect = renderEffect
            )

            FabGroup(
                animationProgress = fabAnimationProgress,
                renderEffect = null,
                onExpenseClick = {
                    isMenuExtended.value = false
                    homeNavController.navigate(
                        AddTransactionRoute(
                            transactionId = null,
                            transactionType = "EXPENSE"
                        )
                    )
                },
                onTransferClick = {
                    isMenuExtended.value = false
                    homeNavController.navigate(
                        AddTransactionRoute(
                            transactionId = null,
                            transactionType = "TRANSFER"
                        )
                    )
                },
                onIncomeClick = {
                    isMenuExtended.value = false
                    homeNavController.navigate(
                        AddTransactionRoute(
                            transactionId = null,
                            transactionType = "INCOME"
                        )
                    )
                },
                toggleAnimation = {
                    isMenuExtended.value = !isMenuExtended.value
                }
            )

            Circle(
                color = Color.White,
                animationProgress = clickAnimationProgress
            )
        }
    }
}
}

private fun Easing.transform(from: Float, to: Float, value: Float): Float {
    val range = to - from
    val progress = if (range == 0f) 0f else ((value - from) / range).coerceIn(0f, 1f)
    return this.transform(progress)
}

@Composable
private fun Circle(color: Color, animationProgress: Float) {
    val animationValue = sin(PI * animationProgress).toFloat()

    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(56.dp)
            .scale(2f - animationValue)
            .border(
                width = 2.dp,
                color = color.copy(alpha = color.alpha * animationValue),
                shape = CircleShape
            )
    )
}

@Composable
private fun FabGroup(
    animationProgress: Float,
    renderEffect: androidx.compose.ui.graphics.RenderEffect?,
    onExpenseClick: () -> Unit = {},
    onTransferClick: () -> Unit = {},
    onIncomeClick: () -> Unit = {},
    toggleAnimation: () -> Unit = {}
) {
    val showChildren = animationProgress > 0f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.renderEffect = renderEffect }
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (showChildren) {
            // Left Child (Expense)
            AnimatedFab(
                icon = Icons.AutoMirrored.Filled.CallMade,
                containerColor = Color(0xFFE57373),
                modifier = Modifier
                    .offset(
                        x = (-84).dp * FastOutSlowInEasing.transform(0f, 0.8f, animationProgress),
                        y = (-72).dp * FastOutSlowInEasing.transform(0f, 0.8f, animationProgress)
                    ),
                opacity = LinearEasing.transform(0.2f, 0.7f, animationProgress),
                onClick = onExpenseClick,
                scale = 0.85f
            )

            // Center Child (Transfer)
            AnimatedFab(
                icon = Icons.Default.SwapHoriz,
                containerColor = Color(0xFF64B5F6),
                modifier = Modifier
                    .offset(
                        y = (-100).dp * FastOutSlowInEasing.transform(0.1f, 0.9f, animationProgress)
                    ),
                opacity = LinearEasing.transform(0.3f, 0.8f, animationProgress),
                onClick = onTransferClick,
                scale = 0.85f
            )

            // Right Child (Income)
            AnimatedFab(
                icon = Icons.AutoMirrored.Filled.CallReceived,
                containerColor = Color(0xFF81C784),
                modifier = Modifier
                    .offset(
                        x = 84.dp * FastOutSlowInEasing.transform(0.2f, 1.0f, animationProgress),
                        y = (-72).dp * FastOutSlowInEasing.transform(0.2f, 1.0f, animationProgress)
                    ),
                opacity = LinearEasing.transform(0.4f, 0.9f, animationProgress),
                onClick = onIncomeClick,
                scale = 0.85f
            )
        }

        // Base merging helper circle (only drawn in gooey blur layer)
        if (renderEffect != null) {
            AnimatedFab(
                modifier = Modifier
                    .scale(1f - LinearEasing.transform(0.5f, 0.85f, animationProgress)),
                containerColor = MaterialTheme.colorScheme.primary,
                scale = 1.0f
            )
        }

        // Toggle button (Add/Cross)
        AnimatedFab(
            icon = Icons.Default.Add,
            modifier = Modifier
                .rotate(
                    225f * FastOutSlowInEasing.transform(0.35f, 0.65f, animationProgress)
                ),
            onClick = toggleAnimation,
            containerColor = MaterialTheme.colorScheme.primary,
            scale = 1.0f
        )
    }
}

@Composable
private fun AnimatedFab(
    modifier: Modifier,
    icon: ImageVector? = null,
    opacity: Float = 1f,
    containerColor: Color = MaterialTheme.colorScheme.secondary,
    onClick: () -> Unit = {},
    scale: Float = 1.0f
) {
    FloatingActionButton(
        onClick = onClick,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
        containerColor = containerColor,
        modifier = modifier.scale(scale),
        shape = CircleShape
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = Color.White.copy(alpha = opacity)
            )
        }
    }
}
