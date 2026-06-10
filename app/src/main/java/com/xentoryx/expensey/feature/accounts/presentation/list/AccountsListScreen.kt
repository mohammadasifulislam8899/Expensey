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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xentoryx.expensey.feature.accounts.presentation.add.AddAccountScreen
import com.xentoryx.expensey.feature.accounts.presentation.add.AddAccountViewModel
import com.xentoryx.expensey.feature.accounts.presentation.detail.AccountDetailScreen
import com.xentoryx.expensey.feature.accounts.presentation.detail.AccountDetailViewModel
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

sealed interface AccountsNavigation {
    data object List : AccountsNavigation
    data object Add : AccountsNavigation
    data class Detail(val account: AccountSummary) : AccountsNavigation
    data class Edit(val account: AccountSummary) : AccountsNavigation
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
                    onAddAccountClick = { navigateTo(AccountsNavigation.Add) },
                    onAccountClick = { account -> navigateTo(AccountsNavigation.Detail(account)) }
                )
            }
            is AccountsNavigation.Add -> {
                AddAccountScreen(
                    viewModel = koinViewModel<AddAccountViewModel>(),
                    onBackClick = navigateBack
                )
            }
            is AccountsNavigation.Detail -> {
                AccountDetailScreen(
                    account = currentScreen.account,
                    viewModel = koinViewModel<AccountDetailViewModel>(),
                    onBackClick = navigateBack,
                    onEditAccountClick = { account -> navigateTo(AccountsNavigation.Edit(account)) },
                    onAccountDeleted = {
                        navigateBack()
                        viewModel.refreshAccounts()
                    }
                )
            }
            is AccountsNavigation.Edit -> {
                AddAccountScreen(
                    viewModel = koinViewModel<AddAccountViewModel>(),
                    accountId = currentScreen.account.accountId,
                    onBackClick = {
                        navigationStack = listOf(AccountsNavigation.List)
                        viewModel.refreshAccounts()
                    }
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
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val totalAssets = remember(state.accounts) {
        state.accounts.sumOf { it.balance }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header with Refresh Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Accounts",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Manage your offline-first cash flows",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshAccounts() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
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

            // Total Assets Banner Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "TOTAL ASSETS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "$%,.2f", totalAssets),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
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
                    items(state.accounts) { account ->
                        AccountItem(
                            account = account,
                            onClick = { onAccountClick(account) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) } // spacing for FAB
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
    val gradientBrush = when (account.accountType.uppercase(Locale.US)) {
        "CASH" -> Brush.horizontalGradient(listOf(Color(0xFF00B4DB), Color(0xFF0083B0)))
        "CARD" -> Brush.horizontalGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
        else -> Brush.horizontalGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d))) // BANK
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradientBrush, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = account.accountType.uppercase(Locale.US),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = account.accountName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = String.format(Locale.US, "$%,.2f", account.balance),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
