package com.xentoryx.expensey.feature.accounts.presentation.list

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xentoryx.expensey.core.data.database.entity.SyncStatus
import com.xentoryx.expensey.feature.accounts.presentation.add.AddAccountScreen
import com.xentoryx.expensey.feature.accounts.presentation.add.AddAccountViewModel
import com.xentoryx.expensey.feature.accounts.presentation.detail.AccountDetailScreen
import com.xentoryx.expensey.feature.accounts.presentation.detail.AccountDetailViewModel
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.category.presentation.CategoriesScreen
import com.xentoryx.expensey.feature.category.presentation.CategoriesViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import com.xentoryx.expensey.core.presentation.components.CrushCanvasDecoration
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.core.storage.CurrencyConverter
import org.koin.compose.koinInject

sealed interface AccountsNavigation {
    data object List : AccountsNavigation
    data object AddAccount : AccountsNavigation
    data class AccountDetail(val account: AccountSummary) : AccountsNavigation
    data class EditAccount(val account: AccountSummary) : AccountsNavigation
    data object Categories : AccountsNavigation
}

@Composable
fun AccountsListScreen(
    viewModel: AccountsListViewModel,
    modifier: Modifier = Modifier
) {
    var navigationStack by remember { mutableStateOf<List<AccountsNavigation>>(listOf(AccountsNavigation.List)) }
    val currentScreen = navigationStack.last()

    val navigateTo: (AccountsNavigation) -> Unit = { screen ->
        navigationStack = navigationStack + screen
    }

    val navigateBack: () -> Unit = {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
        }
    }

    BackHandler(enabled = navigationStack.size > 1) {
        navigateBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (currentScreen) {
            is AccountsNavigation.List -> {
                AccountsListContent(
                    viewModel = viewModel,
                    onAddAccountClick = { navigateTo(AccountsNavigation.AddAccount) },
                    onAccountClick = { account -> navigateTo(AccountsNavigation.AccountDetail(account)) },
                    onBackClick = {},
                    onCategoriesClick = { navigateTo(AccountsNavigation.Categories) },
                    isRoot = true
                )
            }
            is AccountsNavigation.AddAccount -> {
                AddAccountScreen(
                    viewModel = koinViewModel<AddAccountViewModel>(),
                    onBackClick = navigateBack
                )
            }
            is AccountsNavigation.AccountDetail -> {
                AccountDetailScreen(
                    account = currentScreen.account,
                    viewModel = koinViewModel<AccountDetailViewModel>(),
                    onBackClick = navigateBack,
                    onEditAccountClick = { account -> navigateTo(AccountsNavigation.EditAccount(account)) },
                    onAccountDeleted = {
                        navigateBack()
                        viewModel.refreshAccounts()
                    }
                )
            }
            is AccountsNavigation.EditAccount -> {
                AddAccountScreen(
                    viewModel = koinViewModel<AddAccountViewModel>(),
                    accountId = currentScreen.account.accountId,
                    onBackClick = {
                        navigationStack = listOf(AccountsNavigation.List)
                        viewModel.refreshAccounts()
                    }
                )
            }
            is AccountsNavigation.Categories -> {
                CategoriesScreen(
                    viewModel = koinViewModel<CategoriesViewModel>(),
                    onBackClick = navigateBack
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsListContent(
    viewModel: AccountsListViewModel,
    onAddAccountClick: () -> Unit,
    onAccountClick: (AccountSummary) -> Unit,
    onBackClick: () -> Unit,
    onCategoriesClick: () -> Unit = {},
    isRoot: Boolean = false,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val tokenManager: TokenManager = koinInject()
    val userCurrency by tokenManager.userCurrency.collectAsState(initial = "BDT")
    val currencyConverter: CurrencyConverter = koinInject()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val totalAssets = remember(state.accounts, userCurrency) {
        state.accounts.sumOf { account ->
            currencyConverter.convert(account.balance, account.currencyCode, userCurrency)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAccountClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Account")
            }
        },
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CrushCanvasDecoration(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isRoot) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Accounts",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Manage your cash, cards, and banks",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isRoot) {
                            IconButton(
                                onClick = onCategoriesClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Category, contentDescription = "Manage Categories")
                            }
                        }
                        IconButton(
                            onClick = { viewModel.refreshAccounts() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }
                }

                // Total Assets Banner Card (Borderless centered banner)
                val defaultCurrency = userCurrency
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Assets",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatCurrency(totalAssets, defaultCurrency),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accounts List or Empty State
                if (state.accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No accounts found",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap + to add your cash, bank, or card accounts.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    state.accounts.forEachIndexed { index, account ->
                                        AccountItem(
                                            account = account,
                                            onClick = { onAccountClick(account) }
                                        )
                                        if (index < state.accounts.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) } // spacing for FAB
                    }
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: AccountSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (account.accountType.uppercase(Locale.US)) {
        "CASH" -> Color(0xFF2196F3) // Blue
        "CARD" -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF4CAF50) // Green (BANK)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Accent circle showing Type indicator
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
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
                    fontSize = 15.sp,
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
                        modifier = Modifier.size(9.dp)
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatCurrency(amount: Double, currencyCode: String): String {
    val symbol = when (currencyCode.uppercase(Locale.US)) {
        "BDT" -> "৳"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "INR" -> "₹"
        "CAD" -> "C$"
        "AUD" -> "A$"
        "JPY" -> "¥"
        "SAR" -> "SR "
        "AED" -> "DH "
        else -> "$"
    }
    return String.format(Locale.US, "%s%,.2f", symbol, amount)
}
