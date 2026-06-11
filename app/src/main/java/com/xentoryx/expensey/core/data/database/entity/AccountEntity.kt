package com.xentoryx.expensey.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val accountId: String,
    val accountName: String,
    val accountType: String,
    val balance: Double,
    val currencyCode: String,
    val isSynced: Boolean = false,
    val isNewLocal: Boolean = true,
    val isDeleted: Boolean = false
)
