package com.xentoryx.expensey.feature.recurring_transaction.presentation.form

import com.xentoryx.expensey.feature.category.domain.model.Category
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.recurring_transaction.domain.model.RecurringTransaction

data class RecurringFormState(
    val recurringTransactionId: String? = null,
    val accounts: List<AccountSummary> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val amount: String = "",
    val type: String = "EXPENSE",
    val frequency: String = "MONTHLY",
    val note: String = "",
    val startDate: String = "",
    val endDate: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
