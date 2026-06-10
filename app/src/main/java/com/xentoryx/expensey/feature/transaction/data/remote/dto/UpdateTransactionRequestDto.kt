package com.xentoryx.expensey.feature.transaction.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateTransactionRequestDto(
    val accountId: String,
    val categoryId: String,
    val transferToAccountId: String? = null,
    val amount: Double,
    val type: String,
    val note: String? = null,
    val transactionDate: String
)
