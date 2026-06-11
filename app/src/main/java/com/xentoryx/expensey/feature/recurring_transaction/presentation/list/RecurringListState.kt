package com.xentoryx.expensey.feature.recurring_transaction.presentation.list

import com.xentoryx.expensey.feature.recurring_transaction.domain.model.RecurringTransaction
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.category.domain.model.Category

data class RecurringListState(
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val accounts: List<AccountSummary> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
