package com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateRecurringTransactionRequestDto(
    val accountId: String,
    val categoryId: String,
    val amount: Double,
    val type: String,
    val frequency: String,
    val note: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)
