package com.xentoryx.expensey.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xentoryx.expensey.core.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY transactionDate DESC, createdAt DESC")
    fun getRecentTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY transactionDate DESC, createdAt DESC")
    suspend fun getRecentTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 1")
    suspend fun getUnsyncedDeletions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE transactions SET isDeleted = 1, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun markTransactionDeleted(id: String)

    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun updateTransactionCategoryId(oldCategoryId: String, newCategoryId: String)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND syncStatus != 'SYNCED' AND isDeleted = 0 AND transactionDate >= :startDate AND transactionDate <= :endDate")
    suspend fun getUnsyncedTransactionSumByType(type: String, startDate: String, endDate: String): Double?

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND transactionDate LIKE :dateString || '%'")
    suspend fun getTransactionsByDate(dateString: String): List<TransactionEntity>
}
