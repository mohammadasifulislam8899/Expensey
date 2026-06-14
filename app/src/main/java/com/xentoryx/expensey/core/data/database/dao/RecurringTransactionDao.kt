package com.xentoryx.expensey.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xentoryx.expensey.core.data.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0")
    fun getRecurringTransactionsFlow(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0")
    suspend fun getRecurringTransactions(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getRecurringTransactionById(id: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedRecurringTransactions(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 1")
    suspend fun getUnsyncedDeletions(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransactions(recurringTransactions: List<RecurringTransactionEntity>)

    @Query("UPDATE recurring_transactions SET isDeleted = 1, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun markRecurringTransactionDeleted(id: String)

    @Query("UPDATE recurring_transactions SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markRecurringTransactionSynced(id: String)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransactionById(id: String)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAllRecurringTransactions()

    @Query("DELETE FROM recurring_transactions WHERE syncStatus = 'SYNCED' AND isDeleted = 0")
    suspend fun deleteSyncedRecurringTransactions()

    @Transaction
    suspend fun replaceRecurringTransactions(recurringTransactions: List<RecurringTransactionEntity>) {
        deleteSyncedRecurringTransactions()
        insertRecurringTransactions(recurringTransactions)
    }

    @Query("UPDATE recurring_transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun updateRecurringTransactionCategoryId(oldCategoryId: String, newCategoryId: String)
}
