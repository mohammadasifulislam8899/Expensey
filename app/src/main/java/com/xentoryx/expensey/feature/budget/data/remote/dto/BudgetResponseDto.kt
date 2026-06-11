package com.xentoryx.expensey.feature.budget.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class BudgetResponseDto(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String? = null,
    val categoryColor: String? = null,
    val amountLimit: Double,
    val period: String,
    val startDate: String,
    val endDate: String,
    val spent: Double,
    val remaining: Double,
    val percentage: Double,
    val isExceeded: Boolean
)
