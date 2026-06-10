package com.xentoryx.expensey.feature.transaction.data.remote.dto

import com.xentoryx.expensey.feature.dashboard.data.remote.dto.TransactionResponseDto
import kotlinx.serialization.Serializable

@Serializable
data class TransactionListResponseDto(
    val data: List<TransactionResponseDto>,
    val page: Int,
    val limit: Int,
    val total: Int
)
