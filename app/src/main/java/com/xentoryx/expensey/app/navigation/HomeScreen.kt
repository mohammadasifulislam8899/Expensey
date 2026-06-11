package com.xentoryx.expensey.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    // Determine bottom bar visibility dynamically
    val showBottomBar = remember(currentRoute) {
        currentRoute == DashboardRoute::class.qualifiedName ||
        currentRoute == TransactionsRoute::class.qualifiedName ||
        currentRoute == AccountsRoute::class.qualifiedName ||
        currentRoute == BudgetsRoute::class.qualifiedName ||
        currentRoute == SettingsRoute::class.qualifiedName
    }

    // Determine currently selected index based on active route
    val selectedTab = when (currentRoute) {
        DashboardRoute::class.qualifiedName -> 0
        TransactionsRoute::class.qualifiedName -> 1
        AccountsRoute::class.qualifiedName -> 2
        BudgetsRoute::class.qualifiedName -> 3
        SettingsRoute::class.qualifiedName -> 4
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

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
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
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
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
                        label = { Text("Txns") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
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
                        label = { Text("Accounts") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
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
                        label = { Text("Budgets") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = {
                            homeNavController.navigate(SettingsRoute) {
                                popUpTo(homeNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
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
                        viewModel = koinViewModel()
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
                    SettingsScreen()
                }
                composable<AddTransactionRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<AddTransactionRoute>()
                    AddTransactionScreen(
                        viewModel = koinViewModel(),
                        transactionId = route.transactionId,
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
}
