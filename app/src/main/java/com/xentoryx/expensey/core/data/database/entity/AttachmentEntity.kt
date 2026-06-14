package com.xentoryx.expensey.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"])]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val localFilePath: String,
    val fileName: String,
    val fileType: String,
    val createdAt: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isNewLocal: Boolean = true,
    val isDeleted: Boolean = false
)
