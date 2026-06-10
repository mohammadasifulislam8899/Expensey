package com.xentoryx.expensey.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_overview")
data class DashboardOverviewEntity(
    @PrimaryKey val id: Int = 1,
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val savingsRate: Double
)
