package com.xentoryx.expensey.feature.transaction.presentation.list

import com.xentoryx.expensey.core.data.database.entity.SyncStatus

data class TransactionUiModel(
    val id: String,
    val accountId: String,
    val accountName: String,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String?,
    val categoryColor: String?,
    val transferToAccountId: String?,
    val amount: Double,
    val type: String, // "INCOME" | "EXPENSE" | "TRANSFER"
    val note: String?,
    val date: String, // transactionDate or nextRunDate
    val createdAt: String,
    val isRecurring: Boolean,
    val isActive: Boolean = true,
    val frequency: String? = null,
    val syncStatus: SyncStatus? = null
)
