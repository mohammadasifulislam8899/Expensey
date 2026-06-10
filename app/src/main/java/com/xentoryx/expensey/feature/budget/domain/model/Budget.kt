package com.xentoryx.expensey.feature.budget.domain.model

data class Budget(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: String?,
    val amountLimit: Double,
    val period: String,
    val startDate: String,
    val endDate: String,
    val spent: Double,
    val remaining: Double,
    val percentage: Double,
    val isExceeded: Boolean
)
