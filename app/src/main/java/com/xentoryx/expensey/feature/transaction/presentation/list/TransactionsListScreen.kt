package com.xentoryx.expensey.feature.transaction.presentation.list

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.transaction.domain.model.TransactionFilter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.xentoryx.expensey.core.presentation.components.CrushCanvasDecoration
import com.xentoryx.expensey.core.domain.model.AppCurrency
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.core.storage.CurrencyConverter
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    viewModel: TransactionsListViewModel,
    onAddTransactionClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onTransactionClick: (TransactionUiModel) -> Unit,
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

    Scaffold(
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
                    Column {
                        Text(
                            text = "Transactions",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Keep track of your cash flows",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export PDF Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Search & Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextField(
                        value = state.filter.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search transactions...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (state.filter.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    )

                    var showFilterDialog by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showFilterDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (state.filter != TransactionFilter(searchQuery = state.filter.searchQuery, recurrenceFilter = state.filter.recurrenceFilter))
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (state.filter != TransactionFilter(searchQuery = state.filter.searchQuery, recurrenceFilter = state.filter.recurrenceFilter))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Advanced Filter"
                        )
                    }

                    if (showFilterDialog) {
                        var showStartDatePicker by remember { mutableStateOf(false) }
                        var showEndDatePicker by remember { mutableStateOf(false) }

                        AlertDialog(
                            onDismissRequest = { showFilterDialog = false },
                            title = { Text("Advanced Filter", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Dates
                                    Column {
                                        Text("Date Range", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { showStartDatePicker = true },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(state.filter.startDate?.toString() ?: "Start Date", fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { showEndDatePicker = true },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(state.filter.endDate?.toString() ?: "End Date", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    // Min / Max Amount
                                    Column {
                                        Text("Amount Range", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            var minAmountText by remember { mutableStateOf(state.filter.minAmount?.toString() ?: "") }
                                            var maxAmountText by remember { mutableStateOf(state.filter.maxAmount?.toString() ?: "") }

                                            OutlinedTextField(
                                                value = minAmountText,
                                                onValueChange = {
                                                    minAmountText = it
                                                    val amt = it.toDoubleOrNull()
                                                    viewModel.updateFilter(state.filter.copy(minAmount = amt))
                                                },
                                                placeholder = { Text("Min") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = maxAmountText,
                                                onValueChange = {
                                                    maxAmountText = it
                                                    val amt = it.toDoubleOrNull()
                                                    viewModel.updateFilter(state.filter.copy(maxAmount = amt))
                                                },
                                                placeholder = { Text("Max") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Accounts checkboxes
                                    if (state.accounts.isNotEmpty()) {
                                        Column {
                                            Text("Filter by Accounts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            state.accounts.forEach { account ->
                                                val isSelected = state.filter.selectedAccounts.contains(account.accountId)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val newSet = if (isSelected) {
                                                                state.filter.selectedAccounts - account.accountId
                                                            } else {
                                                                state.filter.selectedAccounts + account.accountId
                                                            }
                                                            viewModel.updateFilter(state.filter.copy(selectedAccounts = newSet))
                                                        }
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = {
                                                            val newSet = if (isSelected) {
                                                                state.filter.selectedAccounts - account.accountId
                                                            } else {
                                                                state.filter.selectedAccounts + account.accountId
                                                            }
                                                            viewModel.updateFilter(state.filter.copy(selectedAccounts = newSet))
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(account.accountName, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }

                                    // Categories checkboxes
                                    if (state.categories.isNotEmpty()) {
                                        Column {
                                            Text("Filter by Categories", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            state.categories.forEach { category ->
                                                val isSelected = state.filter.selectedCategories.contains(category.categoryId)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val newSet = if (isSelected) {
                                                                state.filter.selectedCategories - category.categoryId
                                                            } else {
                                                                state.filter.selectedCategories + category.categoryId
                                                            }
                                                            viewModel.updateFilter(state.filter.copy(selectedCategories = newSet))
                                                        }
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = {
                                                            val newSet = if (isSelected) {
                                                                state.filter.selectedCategories - category.categoryId
                                                            } else {
                                                                state.filter.selectedCategories + category.categoryId
                                                            }
                                                            viewModel.updateFilter(state.filter.copy(selectedCategories = newSet))
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(category.categoryName, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = { showFilterDialog = false }) {
                                    Text("Apply")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.clearFilter()
                                        showFilterDialog = false
                                    }
                                ) {
                                    Text("Clear All")
                                }
                            }
                        )

                        if (showStartDatePicker) {
                            val date = state.filter.startDate ?: LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _, year, monthOfYear, dayOfMonth ->
                                    val selectedDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                                    viewModel.updateFilter(state.filter.copy(startDate = selectedDate))
                                    showStartDatePicker = false
                                },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth
                            ).apply {
                                setOnDismissListener { showStartDatePicker = false }
                                show()
                            }
                        }

                        if (showEndDatePicker) {
                            val date = state.filter.endDate ?: LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _, year, monthOfYear, dayOfMonth ->
                                    val selectedDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                                    viewModel.updateFilter(state.filter.copy(endDate = selectedDate))
                                    showEndDatePicker = false
                                },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth
                            ).apply {
                                setOnDismissListener { showEndDatePicker = false }
                                show()
                            }
                        }
                    }
                }

                // Row 1: Recurrence filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val recurrenceFilters = listOf(
                        "ALL" to "All Trans",
                        "ONETIME" to "One-time Only",
                        "RECURRING" to "Schedules (Recurring)"
                    )
                    recurrenceFilters.forEach { (value, label) ->
                        val isSelected = state.filter.recurrenceFilter == value
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateFilter(state.filter.copy(recurrenceFilter = value)) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }

                // Row 2: Type filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        null to "All Types",
                        "INCOME" to "Income",
                        "EXPENSE" to "Expense",
                        "TRANSFER" to "Transfer"
                    )

                    filters.forEach { (type, label) ->
                        val isSelected = state.selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectType(type) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }

                // List or Empty state
                if (state.isLoading && state.filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (state.filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No transactions found",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Try changing filters or add a new transaction.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                } else {
                    val groupedTransactions = remember(state.filteredTransactions) {
                        state.filteredTransactions.groupBy { transaction ->
                            try {
                                val dateStr = transaction.date.substringBefore("T")
                                LocalDate.parse(dateStr)
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedTransactions.forEach { (date, txs) ->
                            // Merged daily total calculation for non-recurring only
                            val dailyTotal = txs.filter { !it.isRecurring }.sumOf { tx ->
                                val account = state.accounts.find { it.accountId == tx.accountId }
                                val txCurrency = account?.currencyCode ?: "BDT"
                                val convertedAmount = currencyConverter.convert(tx.amount, txCurrency, userCurrency)
                                val isExpense = tx.type.uppercase(Locale.US) == "EXPENSE"
                                if (isExpense) -convertedAmount else convertedAmount
                            }

                            val dateLabel = when (date) {
                                LocalDate.now() -> "Today"
                                LocalDate.now().minusDays(1) -> "Yesterday"
                                else -> {
                                    val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US)
                                    date.format(formatter)
                                }
                            }

                            item(key = "header_${date}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val currencyCode = userCurrency
                                    val formattedDailyTotal = (if (dailyTotal < 0) "-" else "+") + formatCurrency(kotlin.math.abs(dailyTotal), currencyCode)
                                    
                                    // Show total if there are non-recurring items
                                    if (txs.any { !it.isRecurring }) {
                                        Text(
                                            text = formattedDailyTotal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (dailyTotal < 0) getResponsiveTransactionColor("EXPENSE") else getResponsiveTransactionColor("INCOME")
                                        )
                                    }
                                }
                            }

                            item(key = "card_${date}") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        txs.forEachIndexed { index, transaction ->
                                            SwipeToDismissBox(
                                                state = rememberSwipeToDismissBoxState(
                                                    confirmValueChange = {
                                                        if (it == SwipeToDismissBoxValue.EndToStart) {
                                                            viewModel.deleteTransaction(transaction.id, transaction.isRecurring)
                                                            true
                                                        } else false
                                                    }
                                                ),
                                                backgroundContent = {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(20.dp))
                                                            .padding(horizontal = 20.dp),
                                                        contentAlignment = Alignment.CenterEnd
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = MaterialTheme.colorScheme.onError
                                                        )
                                                    }
                                                },
                                                content = {
                                                    TransactionRowItem(
                                                        transaction = transaction,
                                                        accounts = state.accounts,
                                                        onActiveToggle = { checked ->
                                                            viewModel.toggleRecurringActive(transaction.id, checked)
                                                        },
                                                        onClick = { onTransactionClick(transaction) }
                                                    )
                                                },
                                                enableDismissFromStartToEnd = false
                                            )
                                            if (index < txs.lastIndex) {
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
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
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

fun formatTransactionUiAmount(
    amount: Double,
    type: String,
    currencyCode: String
): String {
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
    val isExpense = type.uppercase(Locale.US) == "EXPENSE"
    val isTransfer = type.uppercase(Locale.US) == "TRANSFER"
    val sign = when {
        isExpense -> "-"
        isTransfer -> ""
        else -> "+"
    }
    return String.format(Locale.US, "%s %s%,.2f", sign, symbol, amount)
}

@Composable
fun TransactionRowItem(
    transaction: TransactionUiModel,
    accounts: List<AccountSummary>,
    onActiveToggle: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val isExpense = transaction.type.uppercase(Locale.US) == "EXPENSE"
    val isTransfer = transaction.type.uppercase(Locale.US) == "TRANSFER"
    val tintColor = getResponsiveTransactionColor(transaction.type)
    val indicatorIcon = when {
        isExpense -> Icons.AutoMirrored.Filled.CallMade
        isTransfer -> Icons.AutoMirrored.Filled.CompareArrows
        else -> Icons.AutoMirrored.Filled.CallReceived
    }

    val account = accounts.find { it.accountId == transaction.accountId }
    val currencyCode = account?.currencyCode ?: "BDT"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (transaction.isRecurring) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else tintColor.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (transaction.isRecurring) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Recurring",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = indicatorIcon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info Column
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = transaction.note ?: "Transaction",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (transaction.isRecurring) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Recurring Schedule",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = transaction.frequency?.lowercase(Locale.US)?.replaceFirstChar { it.titlecase() } ?: "",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val dateLabel = if (transaction.isRecurring) "Next: ${transaction.date.substringBefore("T")}" else transaction.date.substringBefore("T")
            Text(
                text = "${transaction.categoryName} • $dateLabel",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Amount & Switch Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = formatTransactionUiAmount(transaction.amount, transaction.type, currencyCode),
                color = tintColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (transaction.isRecurring) {
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = transaction.isActive,
                    onCheckedChange = { onActiveToggle(it) },
                    modifier = Modifier.scale(0.7f)
                )
            }
        }
    }
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
