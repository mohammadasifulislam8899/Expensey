package com.xentoryx.expensey.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val parentId: String?,
    val icon: String?,
    val color: String?,
    val isSystem: Boolean,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isNewLocal: Boolean = true,
    val isDeleted: Boolean = false
)
