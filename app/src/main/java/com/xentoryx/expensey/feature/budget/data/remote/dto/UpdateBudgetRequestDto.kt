package com.xentoryx.expensey.feature.budget.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateBudgetRequestDto(
    val amountLimit: Double,
    val period: String,
    val startDate: String? = null,
    val endDate: String? = null
)
