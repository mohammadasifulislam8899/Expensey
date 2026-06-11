package com.xentoryx.expensey.feature.dashboard.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryBreakdownResponseDto(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: String?,
    val type: String,
    val total: Double,
    val percentage: Double
)
