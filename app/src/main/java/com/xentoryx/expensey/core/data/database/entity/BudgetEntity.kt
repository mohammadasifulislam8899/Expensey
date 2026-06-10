package com.xentoryx.expensey.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
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
    val isExceeded: Boolean,
    val isSynced: Boolean = false,
    val isNewLocal: Boolean = true,
    val isDeleted: Boolean = false
)
