package com.xentoryx.expensey.feature.transaction.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.domain.model.TransactionFilter
import com.xentoryx.expensey.feature.transaction.domain.usecase.GetTransactionsUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.xentoryx.expensey.feature.recurring_transaction.domain.model.RecurringTransaction
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.GetRecurringTransactionsUseCase
import com.xentoryx.expensey.feature.recurring_transaction.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class TransactionsListViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase,
    private val recurringRepository: RecurringRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _selectedType = MutableStateFlow<String?>(null)
    private val _filter = MutableStateFlow(TransactionFilter())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _accounts = MutableStateFlow<List<AccountSummary>>(emptyList())
    private val _categories = MutableStateFlow<List<CategoryBreakdown>>(emptyList())

    val state: StateFlow<TransactionsListState> = combine(
        getTransactionsUseCase.getLocalTransactions(),
        getRecurringTransactionsUseCase(),
        _selectedType,
        _filter,
        _isLoading,
        _errorMessage,
        _accounts,
        _categories
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[0] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val recurringTransactions = flows[1] as List<RecurringTransaction>
        val selectedType = flows[2] as String?
        val filter = flows[3] as TransactionFilter
        val isLoading = flows[4] as Boolean
        val errorMessage = flows[5] as String?
        @Suppress("UNCHECKED_CAST")
        val accounts = flows[6] as List<AccountSummary>
        @Suppress("UNCHECKED_CAST")
        val categories = flows[7] as List<CategoryBreakdown>

        val uiModels = mutableListOf<TransactionUiModel>()

        // Map normal transactions
        transactions.forEach { txn ->
            val accountName = accounts.find { it.accountId == txn.accountId }?.accountName ?: "Unknown"
            val category = categories.find { it.categoryId == txn.categoryId }
            uiModels.add(
                TransactionUiModel(
                    id = txn.id,
                    accountId = txn.accountId,
                    accountName = accountName,
                    categoryId = txn.categoryId,
                    categoryName = category?.categoryName ?: "Unknown",
                    categoryIcon = category?.categoryIcon,
                    categoryColor = category?.categoryColor,
                    transferToAccountId = txn.transferToAccountId,
                    amount = txn.amount,
                    type = txn.type,
                    note = txn.note,
                    date = txn.transactionDate,
                    createdAt = txn.createdAt,
                    isRecurring = false,
                    isActive = true,
                    syncStatus = txn.syncStatus
                )
            )
        }

        // Map recurring transactions
        recurringTransactions.forEach { rx ->
            val accountName = accounts.find { it.accountId == rx.accountId }?.accountName ?: "Unknown"
            val category = categories.find { it.categoryId == rx.categoryId }
            uiModels.add(
                TransactionUiModel(
                    id = rx.id,
                    accountId = rx.accountId,
                    accountName = accountName,
                    categoryId = rx.categoryId,
                    categoryName = category?.categoryName ?: "Unknown",
                    categoryIcon = category?.categoryIcon,
                    categoryColor = category?.categoryColor,
                    transferToAccountId = null,
                    amount = rx.amount,
                    type = rx.type,
                    note = rx.note,
                    date = rx.nextRunDate,
                    createdAt = rx.createdAt,
                    isRecurring = true,
                    isActive = rx.isActive,
                    frequency = rx.frequency,
                    syncStatus = rx.syncStatus
                )
            )
        }

        // Sort all by date desc, then by createdAt desc
        val sortedUiModels = uiModels.sortedWith(
            compareByDescending<TransactionUiModel> { it.date }
                .thenByDescending { it.createdAt }
        )

        val filtered = sortedUiModels.filter { txn ->
            // 1. Filter by Quick Type tab (All, INCOME, EXPENSE, TRANSFER)
            if (selectedType != null && txn.type != selectedType) return@filter false

            // 2. Filter by search query (note/category/account)
            if (filter.searchQuery.isNotEmpty()) {
                val query = filter.searchQuery.lowercase()
                val noteMatch = txn.note?.lowercase()?.contains(query) == true
                val categoryMatch = txn.categoryName.lowercase().contains(query)
                val accountMatch = txn.accountName.lowercase().contains(query)
                if (!noteMatch && !categoryMatch && !accountMatch) return@filter false
            }

            // 3. Filter by date range
            if (filter.startDate != null) {
                val txnDate = runCatching { LocalDate.parse(txn.date.substringBefore("T")) }.getOrNull()
                if (txnDate == null || txnDate.isBefore(filter.startDate)) return@filter false
            }
            if (filter.endDate != null) {
                val txnDate = runCatching { LocalDate.parse(txn.date.substringBefore("T")) }.getOrNull()
                if (txnDate == null || txnDate.isAfter(filter.endDate)) return@filter false
            }

            // 4. Filter by amount range
            if (filter.minAmount != null && txn.amount < filter.minAmount) return@filter false
            if (filter.maxAmount != null && txn.amount > filter.maxAmount) return@filter false

            // 5. Filter by selected accounts
            if (filter.selectedAccounts.isNotEmpty() && !filter.selectedAccounts.contains(txn.accountId)) return@filter false

            // 6. Filter by selected categories
            if (filter.selectedCategories.isNotEmpty() && !filter.selectedCategories.contains(txn.categoryId)) return@filter false

            // 7. Filter by recurrence type
            when (filter.recurrenceFilter) {
                "ONETIME" -> if (txn.isRecurring) return@filter false
                "RECURRING" -> if (!txn.isRecurring) return@filter false
            }

            true
        }

        TransactionsListState(
            transactions = sortedUiModels,
            filteredTransactions = filtered,
            accounts = accounts,
            categories = categories,
            filter = filter,
            isLoading = isLoading,
            selectedType = selectedType,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsListState()
    )

    init {
        refreshTransactions()
        loadAccountsAndCategories()
    }

    private fun loadAccountsAndCategories() {
        viewModelScope.launch {
            try {
                val dbAccounts = accountDao.getAccounts().map { it.toDomain() }
                val dbCategories = categoryDao.getCategories().map { entity ->
                    CategoryBreakdown(
                        categoryId = entity.id,
                        categoryName = entity.name,
                        categoryIcon = entity.icon,
                        categoryColor = entity.color,
                        type = entity.type,
                        total = 0.0,
                        percentage = 0.0
                    )
                }
                _accounts.value = dbAccounts
                _categories.value = dbCategories
            } catch (e: Exception) {
                // Ignore DB read errors
            }
        }
    }

    fun selectType(type: String?) {
        _selectedType.value = type
    }

    fun updateFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _filter.update { it.copy(searchQuery = query) }
    }

    fun clearFilter() {
        _filter.value = TransactionFilter()
    }

    fun refreshTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val normalResult = getTransactionsUseCase.syncTransactions()
            val recurringResult = getRecurringTransactionsUseCase.sync()
            
            if (normalResult is Result.Error || recurringResult is Result.Error) {
                _errorMessage.value = "Failed to sync transactions"
            }
            _isLoading.value = false
        }
    }

    fun deleteTransaction(id: String, isRecurring: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = if (isRecurring) {
                recurringRepository.deleteRecurringTransaction(id)
            } else {
                deleteTransactionUseCase(id)
            }
            when (result) {
                is Result.Success -> {
                    // Success, Room auto-emits
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to delete item"
                }
            }
            _isLoading.value = false
        }
    }

    fun toggleRecurringActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            recurringRepository.toggleActive(id, isActive)
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
