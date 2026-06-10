package com.xentoryx.expensey.feature.transaction.presentation.list

import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction

data class TransactionsListState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val selectedType: String? = null,
    val errorMessage: String? = null
)
