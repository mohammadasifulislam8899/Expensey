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
        _selectedType,
        _filter,
        _isLoading,
        _errorMessage,
        _accounts,
        _categories
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[0] as List<Transaction>
        val selectedType = flows[1] as String?
        val filter = flows[2] as TransactionFilter
        val isLoading = flows[3] as Boolean
        val errorMessage = flows[4] as String?
        @Suppress("UNCHECKED_CAST")
        val accounts = flows[5] as List<AccountSummary>
        @Suppress("UNCHECKED_CAST")
        val categories = flows[6] as List<CategoryBreakdown>

        val filtered = transactions.filter { txn ->
            // 1. Filter by Quick Type tab (All, INCOME, EXPENSE, TRANSFER)
            if (selectedType != null && txn.type != selectedType) return@filter false

            // 2. Filter by search query (note/category/account)
            if (filter.searchQuery.isNotEmpty()) {
                val query = filter.searchQuery.lowercase()
                val noteMatch = txn.note?.lowercase()?.contains(query) == true
                val categoryName = categories.find { it.categoryId == txn.categoryId }?.categoryName?.lowercase()
                val accountName = accounts.find { it.accountId == txn.accountId }?.accountName?.lowercase()
                val categoryMatch = categoryName?.contains(query) == true
                val accountMatch = accountName?.contains(query) == true
                if (!noteMatch && !categoryMatch && !accountMatch) return@filter false
            }

            // 3. Filter by date range
            if (filter.startDate != null) {
                val txnDate = runCatching { LocalDate.parse(txn.transactionDate.substringBefore("T")) }.getOrNull()
                if (txnDate == null || txnDate.isBefore(filter.startDate)) return@filter false
            }
            if (filter.endDate != null) {
                val txnDate = runCatching { LocalDate.parse(txn.transactionDate.substringBefore("T")) }.getOrNull()
                if (txnDate == null || txnDate.isAfter(filter.endDate)) return@filter false
            }

            // 4. Filter by amount range
            if (filter.minAmount != null && txn.amount < filter.minAmount) return@filter false
            if (filter.maxAmount != null && txn.amount > filter.maxAmount) return@filter false

            // 5. Filter by selected accounts
            if (filter.selectedAccounts.isNotEmpty() && !filter.selectedAccounts.contains(txn.accountId)) return@filter false

            // 6. Filter by selected categories
            if (filter.selectedCategories.isNotEmpty() && !filter.selectedCategories.contains(txn.categoryId)) return@filter false

            true
        }

        TransactionsListState(
            transactions = transactions,
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
            when (val result = getTransactionsUseCase.syncTransactions()) {
                is Result.Success -> {
                    // Cache updated, Room automatically triggers flow update
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to sync transactions"
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
