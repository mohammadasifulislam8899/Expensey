package com.xentoryx.expensey.feature.accounts.presentation.list

import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary

data class AccountsListState(
    val accounts: List<AccountSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
