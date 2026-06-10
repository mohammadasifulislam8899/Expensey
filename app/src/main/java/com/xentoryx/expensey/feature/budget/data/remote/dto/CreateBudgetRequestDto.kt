package com.xentoryx.expensey.feature.budget.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateBudgetRequestDto(
    val categoryId: String,
    val amountLimit: Double,
    val period: String,
    val startDate: String? = null,
    val endDate: String? = null
)
