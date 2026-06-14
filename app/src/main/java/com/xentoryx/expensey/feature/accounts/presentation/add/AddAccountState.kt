package com.xentoryx.expensey.feature.accounts.presentation.add

data class AddAccountState(
    val accountId: String? = null,
    val name: String = "",
    val type: String = "CASH", // Default account type
    val initialBalance: String = "",
    val currencyCode: String = "USD",
    val countryCode: String = "BD",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
