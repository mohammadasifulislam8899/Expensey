package com.xentoryx.expensey.feature.recurring_transaction.domain.model

import com.xentoryx.expensey.core.data.database.entity.SyncStatus

data class RecurringTransaction(
    val id: String,
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
    val syncStatus: SyncStatus? = null
)
