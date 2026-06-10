package com.xentoryx.expensey.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xentoryx.expensey.feature.accounts.presentation.list.AccountsListScreen
import com.xentoryx.expensey.feature.budget.presentation.list.BudgetsListScreen
import com.xentoryx.expensey.feature.auth.domain.usecase.LogoutUseCase
import com.xentoryx.expensey.feature.dashboard.presentation.dashboard.DashboardScreen
import com.xentoryx.expensey.feature.dashboard.presentation.settings.SettingsScreen
import com.xentoryx.expensey.feature.transaction.presentation.add.AddTransactionScreen
import com.xentoryx.expensey.feature.transaction.presentation.list.TransactionsListScreen
import com.xentoryx.expensey.feature.pdf_export.presentation.PdfExportScreen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    logoutUseCase: LogoutUseCase = koinInject()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddTransaction by remember { mutableStateOf(false) }
    var editTransactionId by remember { mutableStateOf<String?>(null) }
    var showPdfExport by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showAddTransaction) {
        AddTransactionScreen(
            viewModel = koinViewModel(),
            onBackClick = { showAddTransaction = false }
        )
    } else if (editTransactionId != null) {
        AddTransactionScreen(
            viewModel = koinViewModel(),
            transactionId = editTransactionId,
            onBackClick = { editTransactionId = null }
        )
    } else if (showPdfExport) {
        PdfExportScreen(
            viewModel = koinViewModel(),
            onBackClick = { showPdfExport = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
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
                        onClick = { selectedTab = 1 },
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
                        onClick = { selectedTab = 2 },
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
                        onClick = { selectedTab = 3 },
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
                        onClick = { selectedTab = 4 },
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
            },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0.dp)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = koinViewModel()
                    )
                    1 -> TransactionsListScreen(
                        viewModel = koinViewModel(),
                        onAddTransactionClick = { showAddTransaction = true },
                        onDownloadClick = { showPdfExport = true },
                        onTransactionClick = { txn -> editTransactionId = txn.id }
                    )
                    2 -> AccountsListScreen(
                        viewModel = koinViewModel()
                    )
                    3 -> BudgetsListScreen(
                        viewModel = koinViewModel()
                    )
                    4 -> SettingsScreen()
                    else -> Unit
                }
            }
        }
    }
}

@Composable
fun PlaceholderTabScreen(
    name: String,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PieChart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "$name Tab",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This feature is coming soon! Keep tracking and crush your limits.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onLogoutClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.background)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
        }
    }
}
