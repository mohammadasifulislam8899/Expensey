package com.xentoryx.expensey.feature.dashboard.presentation.dashboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xentoryx.expensey.app.ui.theme.BrandIncome
import com.xentoryx.expensey.app.ui.theme.BrandExpense
import com.xentoryx.expensey.app.ui.theme.BrandSuccess
import com.xentoryx.expensey.app.ui.theme.BrandWarning
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
import com.xentoryx.expensey.core.presentation.components.CrushCanvasDecoration
import com.xentoryx.expensey.core.presentation.components.SyncCenterSheet
import com.xentoryx.expensey.core.presentation.util.ObserveAsEvents
import com.xentoryx.expensey.core.presentation.util.toUserMessage
import com.xentoryx.expensey.core.sync.SyncAccountsWorker
import com.xentoryx.expensey.core.sync.SyncBudgetsWorker
import com.xentoryx.expensey.core.sync.SyncCategoriesWorker
import com.xentoryx.expensey.core.sync.SyncRecurringTransactionsWorker
import com.xentoryx.expensey.core.sync.SyncWorker
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.core.domain.model.AppCurrency
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.core.storage.CurrencyConverter
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isOnline by rememberConnectivityState()
    val coroutineScope = rememberCoroutineScope()
    val tokenManager: TokenManager = koinInject()
    val userCurrency by tokenManager.userCurrency.collectAsState(initial = "BDT")

    var showSyncSheet by remember { mutableStateOf(false) }
    var isSyncingLocal by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is DashboardEffect.ShowError -> {
                Toast.makeText(context, effect.error.toUserMessage(context), Toast.LENGTH_LONG).show()
            }
        }
    }

    val summary = state.dashboardSummary

    val unsyncedCount = remember(summary) {
        if (summary == null) 0
        else {
            val pendingTx = summary.recentTransactions.count { it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.FAILED }
            val pendingAcc = summary.accounts.count { it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.FAILED }
            pendingTx + pendingAcc
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dashboard",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 26.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Crush your limits today!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Global Sync Badge Icon
                        IconButton(
                            onClick = { showSyncSheet = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = if (unsyncedCount > 0) Icons.Default.CloudQueue else Icons.Default.CloudDone,
                                    contentDescription = "Sync Center",
                                    tint = if (unsyncedCount > 0) BrandWarning else BrandSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (unsyncedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.onEvent(DashboardEvent.Refresh) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "App Tour & Help",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { onProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MA",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Ambient design canvas
            CrushCanvasDecoration(modifier = Modifier.fillMaxSize())

            when {
                state.isLoading && summary == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.error != null && summary == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Failed to load dashboard data.",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.error!!.toUserMessage(context),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.onEvent(DashboardEvent.LoadSummary) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                else -> {
                    val points = remember(summary) {
                        if (summary == null) emptyList()
                        else {
                            var current = summary.totalBalance
                            val list = mutableListOf<Double>()
                            list.add(current)
                            summary.recentTransactions.forEach { tx ->
                                val isExpense = tx.type.uppercase(Locale.US) == "EXPENSE"
                                if (isExpense) {
                                    current += tx.amount
                                } else if (tx.type.uppercase(Locale.US) == "INCOME") {
                                    current -= tx.amount
                                }
                                list.add(current)
                            }
                            list.reversed()
                        }
                    }

                    val displayPoints = remember(points) {
                        if (points.size >= 2) points
                        else listOf(1000.0, 1200.0, 1100.0, 1500.0, 1350.0, 1800.0) // Aesthetic fallback trend
                    }

                    val defaultCurrencyCode = userCurrency

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        if (!isOnline) {
                            item {
                                OfflineBanner()
                            }
                        }

                        // 1. Net Worth command center card (Sparkline trend + Balance Card)
                        item {
                            summary?.let {
                                NetWorthOverviewCard(
                                    totalBalance = it.totalBalance,
                                    sparklinePoints = displayPoints,
                                    currencyCode = defaultCurrencyCode
                                )
                            }
                        }

                        // 1b. Side-by-side Financial Summary Card
                        item {
                            summary?.let {
                                FinancialSummaryRow(
                                    totalIncome = it.totalIncome,
                                    totalExpense = it.totalExpense,
                                    savingsRate = it.savingsRate,
                                    currencyCode = defaultCurrencyCode
                                )
                            }
                        }

                        // 1c. Exchange Rates Sync Status Text
                        item {
                            if (state.ratesUpdateTimestamp > 0L) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Rates updated: ${formatRelativeTime(state.ratesUpdateTimestamp)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // 1d. Monthly Trend Bar Chart
                        item {
                            if (state.monthlyTrend.isNotEmpty()) {
                                MonthlyTrendBarChart(
                                    trends = state.monthlyTrend,
                                    currencyCode = defaultCurrencyCode
                                )
                            }
                        }

                        // 2. Budget Health Card
                        item {
                            BudgetHealthCard(budgets = state.budgets, currencyCode = defaultCurrencyCode)
                        }

                        // 3. Financial Health score
                        item {
                            summary?.let {
                                FinancialHealthCard(savingsRate = it.savingsRate)
                            }
                        }

                        // 4. Smart Insights card
                        item {
                            summary?.let {
                                SmartInsightsCard(savingsRate = it.savingsRate, expenseBreakdown = it.expenseBreakdown)
                            }
                        }

                        // 5. Accounts (Sleek Vertical Card List)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Your Accounts",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (summary?.accounts.isNullOrEmpty()) {
                                    EmptyStateView(message = "No accounts registered yet.")
                                } else {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            summary!!.accounts.forEachIndexed { index, account ->
                                                DashboardAccountItem(account = account)
                                                if (index < summary.accounts.lastIndex) {
                                                    HorizontalDivider(
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                        modifier = Modifier.padding(horizontal = 16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Category Expenses Breakdown
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Monthly Breakdown",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (summary?.expenseBreakdown.isNullOrEmpty()) {
                                    EmptyStateView(message = "No expenses recorded this month.")
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                            .padding(16.dp)
                                    ) {
                                        DonutChart(
                                            breakdownList = summary!!.expenseBreakdown,
                                            currencyCode = defaultCurrencyCode,
                                            modifier = Modifier
                                                .size(180.dp)
                                                .padding(vertical = 8.dp)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        summary.expenseBreakdown.forEach { breakdown ->
                                            BreakdownItem(breakdown = breakdown, currencyCode = defaultCurrencyCode)
                                        }
                                    }
                                }
                            }
                        }

                        // 7. Recent Transactions (Grouped in Card)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Recent Transactions",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (summary?.recentTransactions.isNullOrEmpty()) {
                                    EmptyStateView(message = "No transactions found.")
                                } else {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            summary!!.recentTransactions.take(5).forEachIndexed { index, tx ->
                                                val txCurrencyCode = summary.accounts.find { it.accountId == tx.accountId }?.currencyCode ?: defaultCurrencyCode
                                                val categoryInfo = summary.expenseBreakdown.find { it.categoryId == tx.categoryId }
                                                TransactionItem(
                                                    transaction = tx,
                                                    currencyCode = txCurrencyCode,
                                                    categoryName = categoryInfo?.categoryName ?: "General"
                                                )
                                                if (index < 4 && index < summary.recentTransactions.lastIndex) {
                                                    HorizontalDivider(
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                        modifier = Modifier.padding(horizontal = 16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }

    // Sync Bottom Sheet Overlay
    if (showSyncSheet) {
        SyncCenterSheet(
            summary = summary,
            onDismissRequest = { showSyncSheet = false },
            isSyncing = isSyncingLocal,
            onSyncExchangeRates = { viewModel.onEvent(DashboardEvent.SyncExchangeRates) },
            ratesTimestamp = state.ratesUpdateTimestamp,
            onForceSync = {
                coroutineScope.launch {
                    isSyncingLocal = true
                    try {
                        val constraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                        val syncCategoriesRequest = OneTimeWorkRequestBuilder<SyncCategoriesWorker>()
                            .setConstraints(constraints)
                            .build()
                        val syncAccountsRequest = OneTimeWorkRequestBuilder<SyncAccountsWorker>()
                            .setConstraints(constraints)
                            .build()
                        val syncTransactionsRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                            .setConstraints(constraints)
                            .build()
                        val syncBudgetsRequest = OneTimeWorkRequestBuilder<SyncBudgetsWorker>()
                            .setConstraints(constraints)
                            .build()
                        val syncRecurringTransactionsRequest = OneTimeWorkRequestBuilder<SyncRecurringTransactionsWorker>()
                            .setConstraints(constraints)
                            .build()

                        WorkManager.getInstance(context)
                            .beginWith(listOf(syncCategoriesRequest, syncAccountsRequest))
                            .then(syncTransactionsRequest)
                            .then(listOf(syncBudgetsRequest, syncRecurringTransactionsRequest))
                            .enqueue()

                        kotlinx.coroutines.delay(1500)
                    } catch (_: Exception) {
                    } finally {
                        isSyncingLocal = false
                        showSyncSheet = false
                        Toast.makeText(context, "Sync enqueued successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Onboarding Guide Dialog
    if (state.showOnboarding || showHelpDialog) {
        OnboardingTourDialog(
            onDismiss = {
                showHelpDialog = false
                if (state.showOnboarding) {
                    viewModel.dismissOnboarding()
                }
            }
        )
    }
}

@Composable
fun NetWorthOverviewCard(
    totalBalance: Double,
    sparklinePoints: List<Double>,
    currencyCode: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Net Worth",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCurrency(totalBalance, currencyCode),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        NetWorthSparkline(
            points = sparklinePoints,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
    }
}

@Composable
fun FinancialSummaryRow(
    totalIncome: Double,
    totalExpense: Double,
    savingsRate: Double,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Income Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(BrandIncome.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = BrandIncome,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                    Text(text = "Income", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(totalIncome, currencyCode),
                    color = BrandIncome,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            VerticalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.height(32.dp)
            )

            // Expense Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(BrandExpense.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = BrandExpense,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                    Text(text = "Expenses", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(totalExpense, currencyCode),
                    color = BrandExpense,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            VerticalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.height(32.dp)
            )

            // Savings Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Percent,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(text = "Savings", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%.1f%%", savingsRate),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DashboardAccountItem(account: AccountSummary) {
    val accentColor = when (account.accountType.uppercase(Locale.US)) {
        "CASH" -> Color(0xFF2196F3) // Blue
        "CARD" -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF4CAF50) // Green (BANK)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Accent circle showing Type indicator
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accentColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = account.accountName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Sync status indicator badge
                val (statusIcon, statusLabel, badgeColor) = when (account.syncStatus) {
                    SyncStatus.PENDING -> Triple(Icons.Default.Schedule, "Pending", Color(0xFFFFC107))
                    SyncStatus.FAILED -> Triple(Icons.Default.Warning, "Failed", Color(0xFFF44336))
                    else -> Triple(Icons.Default.Cloud, "Synced", Color(0xFF00C2A0))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusLabel,
                        tint = badgeColor,
                        modifier = Modifier.size(8.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = statusLabel,
                        color = badgeColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = account.accountType.uppercase(Locale.US),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatCurrency(account.balance, account.currencyCode),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NetWorthSparkline(
    points: List<Double>,
    modifier: Modifier = Modifier
) {
    val trendColor = BrandIncome

    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: 1.0
        val range = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal

        val width = size.width
        val height = size.height

        val path = Path()
        val fillPath = Path()

        val stepX = width / (points.size - 1).coerceAtLeast(1)

        points.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = ((value - minVal) / range).toFloat()
            val y = height - (normalizedY * (height - 24f)) - 12f

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (index == points.lastIndex) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Fill gradient under path
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    trendColor.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )

        // Line stroke
        drawPath(
            path = path,
            color = trendColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun BudgetHealthCard(
    budgets: List<com.xentoryx.expensey.feature.budget.domain.model.Budget>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val currencyConverter: CurrencyConverter = koinInject()
    val convertedBudgets = remember(budgets, currencyCode) {
        budgets.map { budget ->
            val limit = currencyConverter.convert(budget.amountLimit, "BDT", currencyCode)
            val spent = currencyConverter.convert(budget.spent, "BDT", currencyCode)
            budget.copy(amountLimit = limit, spent = spent)
        }
    }

    val totalLimit = convertedBudgets.sumOf { it.amountLimit }
    val totalSpent = convertedBudgets.sumOf { it.spent }
    val remainingPercentage = if (totalLimit > 0) {
        ((totalLimit - totalSpent) / totalLimit * 100.0).coerceIn(0.0, 100.0)
    } else {
        78.0 // default/mock visual health if no budgets
    }

    val spentPercentage = 100f - remainingPercentage.toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Health",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = if (spentPercentage > 85f) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.0f%% Safe", remainingPercentage),
                        color = if (spentPercentage > 85f) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { spentPercentage / 100f },
                color = if (spentPercentage > 85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (totalLimit > 0) {
                    String.format(
                        Locale.US,
                        "%s remaining of %s monthly limit",
                        formatCurrency(totalLimit - totalSpent, currencyCode),
                        formatCurrency(totalLimit, currencyCode)
                    )
                } else {
                    "No budgets defined yet. Setup budget limits in Budgets tab."
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FinancialHealthCard(
    savingsRate: Double,
    modifier: Modifier = Modifier
) {
    val score = remember(savingsRate) {
        if (savingsRate <= 0.0) 45 else ((savingsRate * 0.5) + 60).coerceIn(30.0, 99.0).toInt()
    }

    val (status, statusColor) = when {
        score >= 85 -> "Excellent" to BrandSuccess
        score >= 70 -> "Good" to MaterialTheme.colorScheme.primary
        score >= 50 -> "Fair" to BrandWarning
        else -> "Needs Work" to MaterialTheme.colorScheme.error
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Financial Health Score",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your score is $status based on your savings rate.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Score circular gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(70.dp)
            ) {
                val gaugeBgColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = gaugeBgColor,
                        startAngle = -220f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val sweep = (score / 100f) * 260f
                    drawArc(
                        color = statusColor,
                        startAngle = -220f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "pts",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SmartInsightsCard(
    savingsRate: Double,
    expenseBreakdown: List<CategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    val insightMessage = remember(savingsRate, expenseBreakdown) {
        val topCategory = expenseBreakdown.maxByOrNull { it.total }
        when {
            savingsRate > 30.0 -> {
                "Awesome! Your savings rate is ${String.format(Locale.US, "%.1f%%", savingsRate)}. You're building wealth efficiently."
            }
            savingsRate in 0.0..10.0 -> {
                "Alert: Your savings rate is low (${String.format(Locale.US, "%.1f%%", savingsRate)}). Try pausing non-essential subscriptions."
            }
            topCategory != null && topCategory.percentage > 35.0 -> {
                "Insight: You spent ${String.format(Locale.US, "%.1f%%", topCategory.percentage)} of your budget on ${topCategory.categoryName}. Set a budget limit to curb this!"
            }
            else -> {
                "Tip: Grouping transactions with detailed notes helps trace subscription leaks."
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = insightMessage,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AccountCard(account: AccountSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(160.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.accountName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = account.accountType.take(4).uppercase(Locale.US),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Balance",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatCurrency(account.balance, account.currencyCode),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BreakdownItem(
    breakdown: CategoryBreakdown,
    currencyCode: String
) {
    val catColor = remember(breakdown.categoryColor) {
        parseHexColor(breakdown.categoryColor, CrushLavender = Color(0xFFA594FD))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(catColor, CircleShape)
                )
                Text(
                    text = breakdown.categoryName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = formatCurrency(breakdown.total, currencyCode),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LinearProgressIndicator(
                progress = { (breakdown.percentage / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = catColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = String.format(Locale.US, "%.1f%%", breakdown.percentage),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    currencyCode: String,
    categoryName: String
) {
    val isExpense = transaction.type.uppercase(Locale.US) == "EXPENSE"
    val tintColor = getResponsiveTransactionColor(transaction.type)
    val indicatorIcon = if (isExpense) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(tintColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = indicatorIcon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.note ?: "Transaction",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$categoryName • ${transaction.transactionDate.substringBefore("T")}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = (if (isExpense) "-" else "+") + formatCurrency(transaction.amount, currencyCode),
            color = tintColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun parseHexColor(hexString: String?, CrushLavender: Color): Color {
    if (hexString.isNullOrBlank()) return CrushLavender
    return try {
        val cleanHex = hexString.trim().removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        CrushLavender
    }
}

@Composable
fun DonutChart(
    breakdownList: List<CategoryBreakdown>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val totalAmount = breakdownList.sumOf { it.total }
    if (totalAmount <= 0) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .padding(12.dp)
        ) {
            var startAngle = -90f
            breakdownList.forEach { breakdown ->
                val sweepAngle = (breakdown.percentage / 100.0 * 360.0).toFloat()
                val color = parseHexColor(breakdown.categoryColor, CrushLavender = Color(0xFFA594FD))
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Spent",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatCurrency(totalAmount, currencyCode),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatCurrency(amount: Double, currencyCode: String): String {
    val symbol = AppCurrency.fromCode(currencyCode).symbol
    return String.format(Locale.US, "%s%,.2f", symbol, amount)
}

@Composable
fun OfflineBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Offline",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "No internet connection. Changes will sync when online.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun rememberConnectivityState(): State<Boolean> {
    val context = LocalContext.current
    return produceState(initialValue = true) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                value = true
            }
            override fun onLost(network: Network) {
                value = false
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        // Initial state check
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        awaitDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

@Composable
fun MonthlyTrendBarChart(
    trends: List<MonthlyTrendUiModel>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val incomeColor = getResponsiveTransactionColor("INCOME")
    val expenseColor = getResponsiveTransactionColor("EXPENSE")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Column
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Monthly Trends",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Income vs Expense (Last 6 Months)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(incomeColor, CircleShape))
                    Text(
                        text = "Income",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(expenseColor, CircleShape))
                    Text(
                        text = "Expense",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val maxVal = remember(trends) {
                val values = trends.flatMap { listOf(it.income, it.expense) }
                val rawMax = values.maxOrNull() ?: 100.0
                if (rawMax == 0.0) 100.0 else rawMax * 1.15 // 15% padding
            }

            val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

            // Touch input tracking area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(trends) {
                        detectTapGestures(
                            onPress = { offset ->
                                val width = size.width.toFloat()
                                val barGroupWidth = width / trends.size
                                val index = (offset.x / barGroupWidth).toInt().coerceIn(0, trends.lastIndex)
                                selectedIndex = index
                                tryAwaitRelease()
                                selectedIndex = -1
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val barGroupWidth = width / trends.size
                    val spacing = barGroupWidth * 0.2f // Proportional spacing
                    val barWidth = (barGroupWidth - spacing * 2) / 2.2f // Proportional bar width

                    // Draw baseline
                    drawLine(
                        color = onSurfaceVariantColor.copy(alpha = 0.3f),
                        start = androidx.compose.ui.geometry.Offset(0f, height),
                        end = androidx.compose.ui.geometry.Offset(width, height),
                        strokeWidth = 1.dp.toPx()
                    )

                    trends.forEachIndexed { index, model ->
                        val groupStartX = index * barGroupWidth + spacing
                        
                        // Draw Income Bar (Green)
                        val incomeHeight = ((model.income / maxVal) * height).toFloat().coerceIn(0f, height)
                        val incomeTop = height - incomeHeight
                        drawRoundRect(
                            color = incomeColor,
                            topLeft = androidx.compose.ui.geometry.Offset(groupStartX, incomeTop),
                            size = androidx.compose.ui.geometry.Size(barWidth, incomeHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Draw Expense Bar (Red)
                        val expenseHeight = ((model.expense / maxVal) * height).toFloat().coerceIn(0f, height)
                        val expenseTop = height - expenseHeight
                        val expenseStartX = groupStartX + barWidth + (barGroupWidth * 0.05f) // Proportional gap
                        drawRoundRect(
                            color = expenseColor,
                            topLeft = androidx.compose.ui.geometry.Offset(expenseStartX, expenseTop),
                            size = androidx.compose.ui.geometry.Size(barWidth, expenseHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // If selected, draw highlight background
                        if (index == selectedIndex) {
                            drawRect(
                                color = onSurfaceVariantColor.copy(alpha = 0.05f),
                                topLeft = androidx.compose.ui.geometry.Offset(index * barGroupWidth, 0f),
                                size = androidx.compose.ui.geometry.Size(barGroupWidth, height)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                trends.forEach { model ->
                    Text(
                        text = model.monthLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Interactive Tooltip Overlay inside the Card
            if (selectedIndex in trends.indices) {
                val selectedModel = trends[selectedIndex]
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${selectedModel.monthLabel} Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Income:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatCurrency(selectedModel.income, currencyCode),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = incomeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Expense:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatCurrency(selectedModel.expense, currencyCode),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = expenseColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    val minutes = diff / (60 * 1000)
    if (minutes < 1) return "Just now"
    if (minutes < 60) return "$minutes min ago"
    val hours = minutes / 60
    if (hours < 24) return "$hours hr ago"
    val days = hours / 24
    return "$days days ago"
}

@Composable
fun getResponsiveTransactionColor(type: String): Color {
    val isDark = isSystemInDarkTheme()
    return when (type.uppercase(Locale.US)) {
        "INCOME" -> if (isDark) Color(0xFF00C2A0) else Color(0xFF00897B)
        "EXPENSE" -> if (isDark) Color(0xFFFF5C8A) else Color(0xFFD32F2F)
        "TRANSFER" -> if (isDark) Color(0xFF2EE5C5) else Color(0xFF0288D1)
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
fun OnboardingTourDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Welcome to Expensey! 🚀",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Here is a quick tour of what you can do with your financial engine:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                OnboardingFeatureItem(
                    icon = "➕",
                    title = "Unified Transaction FAB",
                    desc = "Tap the central bottom '+' button to log expenses, income, or transfers between accounts."
                )

                OnboardingFeatureItem(
                    icon = "💱",
                    title = "Live Multi-Currency Conversion",
                    desc = "Create accounts in BDT, USD, EUR, etc. Dashboard aggregates everything into your default currency automatically."
                )

                OnboardingFeatureItem(
                    icon = "📱",
                    title = "Country Bank/MFS Suggestions",
                    desc = "Set your country in Settings. Tapping Bank or Mobile Account will recommend popular wallets (bKash, Nagad, Venmo, etc.) to autofill names."
                )

                OnboardingFeatureItem(
                    icon = "⚠️",
                    title = "Danger Zone Purging",
                    desc = "Wipe all transaction history or permanently delete your account securely inside settings."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Let's Go!", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OnboardingFeatureItem(
    icon: String,
    title: String,
    desc: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
