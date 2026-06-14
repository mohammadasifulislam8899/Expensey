package com.xentoryx.expensey.feature.transaction.presentation.list

import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.transaction.domain.model.TransactionFilter

data class TransactionsListState(
    val transactions: List<TransactionUiModel> = emptyList(),
    val filteredTransactions: List<TransactionUiModel> = emptyList(),
    val accounts: List<AccountSummary> = emptyList(),
    val categories: List<CategoryBreakdown> = emptyList(),
    val filter: TransactionFilter = TransactionFilter(),
    val isLoading: Boolean = false,
    val selectedType: String? = null,
    val errorMessage: String? = null
)
