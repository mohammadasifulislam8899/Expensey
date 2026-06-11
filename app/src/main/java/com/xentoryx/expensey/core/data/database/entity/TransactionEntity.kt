package com.xentoryx.expensey.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val accountId: String,
    val categoryId: String,
    val transferToAccountId: String?,
    val amount: Double,
    val type: String,
    val note: String?,
    val transactionDate: String,
    val createdAt: String,
    val isSynced: Boolean = false
)
