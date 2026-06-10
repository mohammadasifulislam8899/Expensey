package com.xentoryx.expensey.feature.dashboard.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountSummaryResponseDto(
    val accountId: String,
    val accountName: String,
    val accountType: String,
    val balance: Double,
    val currencyCode: String
)
