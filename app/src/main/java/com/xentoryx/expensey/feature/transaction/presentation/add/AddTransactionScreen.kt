package com.xentoryx.expensey.feature.transaction.presentation.add

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onBackClick: () -> Unit,
    transactionId: String? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(transactionId) {
        viewModel.setEditTransaction(transactionId)
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
                title = { Text(if (transactionId == null) "Add Transaction" else "Edit Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (transactionId != null) {
                        var showDeleteConfirmation by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Transaction",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        if (showDeleteConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmation = false },
                                title = { Text("Delete Transaction") },
                                text = { Text("Are you sure you want to delete this transaction?") },
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Transaction Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                val types = listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer")
                types.forEach { (type, label) ->
                    val isSelected = state.type == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { viewModel.onTypeChange(type) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 2. Large Amount Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TextField(
                            value = state.amount,
                            onValueChange = { viewModel.onAmountChange(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            ),
                            maxLines = 1,
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            }

            // 3. Dropdowns and Fields Group
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Account (Source) Selector
                var accountExpanded by remember { mutableStateOf(false) }
                val selectedAccountName = state.accounts.find { it.accountId == state.selectedAccountId }?.accountName ?: "Select Account"
                
                Column {
                    Text("Account", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { accountExpanded = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedAccountName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = accountExpanded,
                            onDismissRequest = { accountExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f).background(MaterialTheme.colorScheme.surface)
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
                }

                // Target Account (Visible only for TRANSFER)
                if (state.type == "TRANSFER") {
                    var targetExpanded by remember { mutableStateOf(false) }
                    val selectedTargetName = state.accounts.find { it.accountId == state.transferToAccountId }?.accountName ?: "Select Target Account"

                    Column {
                        Text("Transfer To", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { targetExpanded = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedTargetName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = targetExpanded,
                                onDismissRequest = { targetExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f).background(MaterialTheme.colorScheme.surface)
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
                    }
                } else {
                    // Category Selector (Visible only for INCOME/EXPENSE)
                    var categoryExpanded by remember { mutableStateOf(false) }
                    val selectedCategoryName = state.categories.find { it.categoryId == state.selectedCategoryId }?.categoryName ?: "Select Category"

                    Column {
                        Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { categoryExpanded = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedCategoryName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f).background(MaterialTheme.colorScheme.surface)
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
                    }
                }

                // Date Picker field
                val date = LocalDate.parse(state.dateString)
                val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                val formattedDate = date.format(formatter)

                Column {
                    Text("Date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                val dpd = DatePickerDialog(
                                    context,
                                    { _, year, monthOfYear, dayOfMonth ->
                                        val selectedDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
                                        viewModel.onDateChange(selectedDate.toString())
                                    },
                                    date.year,
                                    date.monthValue - 1,
                                    date.dayOfMonth
                                )
                                dpd.show()
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(formattedDate, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Note Input
                Column {
                    Text("Note", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = state.note,
                        onValueChange = { viewModel.onNoteChange(it) },
                        placeholder = { Text("E.g. Lunch with friends", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3.5. Attachments Card

            // 4. Save Button
            Button(
                onClick = { viewModel.saveTransaction() },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.background, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Transaction", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

