package com.xentoryx.expensey.feature.transaction.presentation.list

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.domain.model.TransactionFilter
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    viewModel: TransactionsListViewModel,
    onAddTransactionClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
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
                        containerColor = if (state.filter != TransactionFilter(searchQuery = state.filter.searchQuery))
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (state.filter != TransactionFilter(searchQuery = state.filter.searchQuery))
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

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    null to "All",
                    "INCOME" to "Income",
                    "EXPENSE" to "Expense",
                    "TRANSFER" to "Transfer"
                )

                filters.forEach { (type, label) ->
                    val isSelected = state.selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectType(type) },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
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
                            text = "Try clearing filters or add a new transaction.",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredTransactions) { transaction ->
                        TransactionRowItem(transaction = transaction, onClick = { onTransactionClick(transaction) })
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onClick: () -> Unit = {}
) {
    val isExpense = transaction.type.uppercase(Locale.US) == "EXPENSE"
    val isTransfer = transaction.type.uppercase(Locale.US) == "TRANSFER"
    val tintColor = when {
        isExpense -> MaterialTheme.colorScheme.tertiary
        isTransfer -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    val indicatorIcon = when {
        isExpense -> Icons.AutoMirrored.Filled.CallMade
        isTransfer -> Icons.AutoMirrored.Filled.CompareArrows
        else -> Icons.AutoMirrored.Filled.CallReceived
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(tintColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = indicatorIcon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(18.dp)
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
                text = transaction.transactionDate.substringBefore("T"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = (if (isExpense) "-" else if (isTransfer) "" else "+") + String.format(Locale.US, "$%,.2f", transaction.amount),
            color = tintColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
