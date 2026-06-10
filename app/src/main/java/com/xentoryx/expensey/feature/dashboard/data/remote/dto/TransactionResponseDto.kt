package com.xentoryx.expensey.feature.dashboard.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponseDto(
    val id: String,
    val userId: String,
    val accountId: String,
    val categoryId: String,
    val transferToAccountId: String?,
    val amount: Double,
    val type: String,
    val note: String?,
    val transactionDate: String,
    val createdAt: String
)
