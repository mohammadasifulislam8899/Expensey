package com.xentoryx.expensey.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val categoryId: String,
    val amount: Double,
    val type: String,
    val frequency: String,
    val note: String?,
    val startDate: String,
    val endDate: String?,
    val nextRunDate: String,
    val isActive: Boolean,
    val createdAt: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isNewLocal: Boolean = true,
    val isDeleted: Boolean = false
)
