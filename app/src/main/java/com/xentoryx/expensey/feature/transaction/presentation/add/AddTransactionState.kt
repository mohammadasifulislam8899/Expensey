package com.xentoryx.expensey.feature.transaction.presentation.add

import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown

data class AddTransactionState(
    val transactionId: String? = null,
    val accounts: List<AccountSummary> = emptyList(),
    val categories: List<CategoryBreakdown> = emptyList(),
    val amount: String = "",
    val note: String = "",
    val type: String = "EXPENSE", // "INCOME" | "EXPENSE" | "TRANSFER"
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val transferToAccountId: String? = null,
    val dateString: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isDeleteSuccess: Boolean = false,
    val errorMessage: String? = null
)
