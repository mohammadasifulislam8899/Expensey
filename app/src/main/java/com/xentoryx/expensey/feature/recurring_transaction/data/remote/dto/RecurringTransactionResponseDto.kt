package com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecurringTransactionResponseDto(
    val id: String,
    val accountId: String,
    val categoryId: String,
    val amount: Double,
    val type: String,
    val frequency: String,
    val note: String?,
    val startDate: String,
    val endDate: String?,
    val nextRunDate: String,
    val isActive: Boolean,
    val createdAt: String
)
