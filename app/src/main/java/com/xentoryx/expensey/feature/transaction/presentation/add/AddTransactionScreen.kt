package com.xentoryx.expensey.feature.transaction.presentation.add

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.xentoryx.expensey.core.domain.model.AppCurrency
import com.xentoryx.expensey.core.presentation.components.CrushCanvasDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onBackClick: () -> Unit,
    transactionId: String? = null,
    transactionType: String? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(transactionId, transactionType) {
        viewModel.setEditTransaction(transactionId, transactionType)
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            Toast.makeText(context, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
            onBackClick()
        }
    }

    LaunchedEffect(state.isDeleteSuccess) {
        if (state.isDeleteSuccess) {
            Toast.makeText(context, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
            onBackClick()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (state.transactionId == null) {
                            "Add Transaction"
                        } else if (state.isRecurringEdit) {
                            "Edit Schedule"
                        } else {
                            "Edit Transaction"
                        }, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.transactionId != null) {
                        var showDeleteConfirmation by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        if (showDeleteConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmation = false },
                                title = { Text("Delete Item") },
                                text = { 
                                    Text(
                                        if (state.isRecurringEdit) "Are you sure you want to delete this recurring schedule?"
                                        else "Are you sure you want to delete this transaction?"
                                    ) 
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteConfirmation = false
                                            viewModel.deleteTransaction()
                                        }
                                    ) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirmation = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // 1. Segmented Control Type Selector
                if (!state.isRecurringEdit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
                            .padding(4.dp)
                    ) {
                        val types = listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer")
                        types.forEach { (type, label) ->
                            val isSelected = state.type == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable { viewModel.onTypeChange(type) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // 2. Large Minimalist Amount Input
                val sourceAcc = state.accounts.find { it.accountId == state.selectedAccountId }
                val targetAcc = state.accounts.find { it.accountId == state.transferToAccountId }
                val sourceCurrency = sourceAcc?.currencyCode ?: state.userCurrencyCode
                val targetCurrency = targetAcc?.currencyCode ?: "BDT"
                val isMultiCurrencyTransfer = state.type == "TRANSFER" && targetAcc != null && sourceCurrency != targetCurrency

                val inputCurrencyCode = if (state.type == "TRANSFER") {
                    if (state.isInputInTargetCurrency) targetCurrency else sourceCurrency
                } else {
                    state.userCurrencyCode
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AMOUNT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currencySymbol = remember(inputCurrencyCode) {
                            AppCurrency.fromCode(inputCurrencyCode).symbol
                        }
                        Text(
                            text = currencySymbol,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        BasicTextField(
                            value = state.amount,
                            onValueChange = { viewModel.onAmountChange(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .widthIn(min = 100.dp, max = 260.dp)
                                .focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (state.amount.isEmpty()) {
                                        Text(
                                            text = "0.00",
                                            style = LocalTextStyle.current.copy(
                                                fontSize = 52.sp,
                                                fontWeight = FontWeight.Light,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    if (isMultiCurrencyTransfer) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                                .clickable { viewModel.onInputCurrencyToggle() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Input in ${if (state.isInputInTargetCurrency) targetCurrency else sourceCurrency}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Switch Input Currency",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        if (state.convertedPreviewAmount > 0.0) {
                            val previewSymbol = AppCurrency.fromCode(if (state.isInputInTargetCurrency) sourceCurrency else targetCurrency).symbol
                            val previewText = if (state.isInputInTargetCurrency) {
                                "= $previewSymbol${String.format(Locale.US, "%,.2f", state.convertedPreviewAmount)} will be deducted"
                            } else {
                                "= $previewSymbol${String.format(Locale.US, "%,.2f", state.convertedPreviewAmount)} will be received"
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = previewText,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 3. Unified Selectors Card
                val selectedAccountName = state.accounts.find { it.accountId == state.selectedAccountId }?.accountName ?: "Select Account"
                val selectedTargetName = state.accounts.find { it.accountId == state.transferToAccountId }?.accountName ?: "Select Target Account"
                val selectedCategoryName = state.categories.find { it.categoryId == state.selectedCategoryId }?.categoryName ?: "Select Category"

                val date = try { LocalDate.parse(state.dateString) } catch (e: Exception) { LocalDate.now() }
                val formattedDate = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                var accountExpanded by remember { mutableStateOf(false) }
                var targetExpanded by remember { mutableStateOf(false) }
                var categoryExpanded by remember { mutableStateOf(false) }
                var showDatePicker by remember { mutableStateOf(false) }

                if (showDatePicker) {
                    DatePickerDialog(
                        context,
                        { _, year, monthOfYear, dayOfMonth ->
                            val selectedDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                            viewModel.onDateChange(selectedDate.toString())
                            showDatePicker = false
                        },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).apply {
                        setOnDismissListener { showDatePicker = false }
                        show()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(vertical = 4.dp)
                ) {
                    // Account Row
                    Box {
                        SelectorRow(
                            label = "Account",
                            value = selectedAccountName,
                            onClick = { accountExpanded = true }
                        )
                        DropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            state.accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.accountName) },
                                    onClick = {
                                        viewModel.onAccountSelected(account.accountId)
                                        accountExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Transfer Target Account Row
                    if (state.type == "TRANSFER") {
                        Box {
                            SelectorRow(
                                label = "Transfer To",
                                value = selectedTargetName,
                                onClick = { targetExpanded = true }
                            )
                            DropdownMenu(
                                expanded = targetExpanded,
                                onDismissRequest = { targetExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                state.accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = { Text(account.accountName) },
                                        onClick = {
                                            viewModel.onTransferToAccountSelected(account.accountId)
                                            targetExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // Category Row
                    if (state.type != "TRANSFER") {
                        Box {
                            SelectorRow(
                                label = "Category",
                                value = selectedCategoryName,
                                onClick = { categoryExpanded = true }
                            )
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                state.categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.categoryName) },
                                        onClick = {
                                            viewModel.onCategorySelected(category.categoryId)
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    // Date Row
                    SelectorRow(
                        label = if (state.isRecurring) "Start Date" else "Date",
                        value = formattedDate,
                        onClick = { showDatePicker = true }
                    )
                }

                // 4. Quick Category chips
                if (state.type != "TRANSFER" && state.categories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Quick Categories",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.categories) { category ->
                                val isSelected = category.categoryId == state.selectedCategoryId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onCategorySelected(category.categoryId) },
                                    label = { Text(category.categoryName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }

                // 5. Note Input Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Note",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = state.note,
                        onValueChange = { viewModel.onNoteChange(it) },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (state.note.isEmpty()) {
                                    Text(
                                        text = "Describe this transaction...",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // 6. Collapsible Repeat Configuration
                if (state.type != "TRANSFER") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Repeat Transaction",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Setup a scheduled repeating template",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Switch(
                                checked = state.isRecurring,
                                onCheckedChange = { viewModel.onRecurringChange(it) },
                                enabled = !state.isRecurringEdit
                            )
                        }

                        AnimatedVisibility(
                            visible = state.isRecurring,
                            enter = expandVertically(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                // Frequency selection row
                                Column {
                                    Text(
                                        text = "Frequency",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val frequencies = listOf("DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "YEARLY" to "Yearly")
                                        frequencies.forEach { (code, label) ->
                                            val isSelected = state.frequency == code
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { viewModel.onFrequencyChange(code) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // End Date Picker
                                val resolvedEndDateStr = state.endDate?.let {
                                    val localDate = LocalDate.parse(it)
                                    localDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                                } ?: "No End Date (Indefinite)"

                                Column {
                                    Text(
                                        text = "End Date (Optional)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .clickable {
                                                val endLimit = state.endDate?.let { LocalDate.parse(it) } ?: LocalDate.now().plusMonths(1)
                                                DatePickerDialog(
                                                    context,
                                                    { _, year, monthOfYear, dayOfMonth ->
                                                        val selectedDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                                                        viewModel.onEndDateChange(selectedDate.toString())
                                                    },
                                                    endLimit.year,
                                                    endLimit.monthValue - 1,
                                                    endLimit.dayOfMonth
                                                ).show()
                                            }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(resolvedEndDateStr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    if (state.endDate != null) {
                                        TextButton(onClick = { viewModel.onEndDateChange(null) }) {
                                            Text("Clear", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Save Button
            Button(
                onClick = { viewModel.saveTransaction() },
                enabled = !state.isLoading && state.note.isNotBlank() && state.amount.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.background, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (state.transactionId == null) {
                            "Save Transaction"
                        } else if (state.isRecurringEdit) {
                            "Save Schedule"
                        } else {
                            "Save Changes"
                        }, 
                        color = MaterialTheme.colorScheme.background, 
                        fontWeight = FontWeight.Black, 
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}

@Composable
fun SelectorRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
