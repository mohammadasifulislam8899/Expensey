package com.xentoryx.expensey.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_breakdowns")
data class CategoryBreakdownEntity(
    @PrimaryKey val categoryId: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: String?,
    val type: String,
    val total: Double,
    val percentage: Double
)
