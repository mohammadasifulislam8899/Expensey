package com.xentoryx.expensey.feature.transaction.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.domain.model.TransactionFilter
import com.xentoryx.expensey.feature.transaction.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class TransactionsListViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedType = MutableStateFlow<String?>(null)
    private val _filter = MutableStateFlow(TransactionFilter())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Map CategoryRepository Flow to CategoryBreakdown list
    private val categoriesFlow = categoryRepository.getCategoriesFlow().map { cats ->
        cats.map { cat ->
            CategoryBreakdown(
                categoryId = cat.id,
                categoryName = cat.name,
                categoryIcon = cat.icon,
                categoryColor = cat.color,
                type = cat.type,
                total = 0.0,
                percentage = 0.0
            )
        }
    }

    val state: StateFlow<TransactionsListState> = combine(
        getTransactionsUseCase.getLocalTransactions(),
        _selectedType,
        _filter,
        _isLoading,
        _errorMessage,
        accountRepository.getAccountsFlow(),
        categoriesFlow
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
            if (selectedType != null && txn.type != selectedType) return@filter false
            if (filter.searchQuery.isNotEmpty()) {
                val query = filter.searchQuery.lowercase()
                val noteMatch = txn.note?.lowercase()?.contains(query) == true
                val categoryName = categories.find { it.categoryId == txn.categoryId }?.categoryName?.lowercase()
                val accountName = accounts.find { it.accountId == txn.accountId }?.accountName?.lowercase()
                val categoryMatch = categoryName?.contains(query) == true
                val accountMatch = accountName?.contains(query) == true
                if (!noteMatch && !categoryMatch && !accountMatch) return@filter false
            }
            if (filter.startDate != null) {
                val txnDate = runCatching { LocalDate.parse(txn.transactionDate.substringBefore("T")) }.getOrNull()
                if (txnDate == null || txnDate.isBefore(filter.startDate)) return@filter false
            }
            if (filter.endDate != null) {
                val txnDate = runCatching { LocalDate.parse(txn.transactionDate.substringBefore("T")) }.getOrNull()
                if (txnDate == null || txnDate.isAfter(filter.endDate)) return@filter false
            }
            if (filter.minAmount != null && txn.amount < filter.minAmount) return@filter false
            if (filter.maxAmount != null && txn.amount > filter.maxAmount) return@filter false
            if (filter.selectedAccounts.isNotEmpty() && !filter.selectedAccounts.contains(txn.accountId)) return@filter false
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
    }

    fun selectType(type: String?) { _selectedType.value = type }
    fun updateFilter(filter: TransactionFilter) { _filter.value = filter }
    fun updateSearchQuery(query: String) { _filter.update { it.copy(searchQuery = query) } }
    fun clearFilter() { _filter.value = TransactionFilter() }
    fun clearError() { _errorMessage.value = null }

    fun refreshTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (getTransactionsUseCase.syncTransactions()) {
                is Result.Success -> {}
                is Result.Error -> _errorMessage.value = "Failed to sync transactions"
            }
            _isLoading.value = false
        }
    }
}
