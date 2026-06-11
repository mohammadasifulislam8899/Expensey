package com.xentoryx.expensey.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xentoryx.expensey.core.data.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE isDeleted = 0")
    fun getBudgetsFlow(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isDeleted = 0")
    suspend fun getBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id AND isDeleted = 0")
    suspend fun getBudgetById(id: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE isDeleted = 1")
    suspend fun getUnsyncedDeletions(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Query("UPDATE budgets SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markBudgetDeleted(id: String)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM budgets WHERE isSynced = 1 AND isDeleted = 0")
    suspend fun deleteSyncedBudgets()

    @Transaction
    suspend fun replaceBudgets(budgets: List<BudgetEntity>) {
        deleteSyncedBudgets()
        insertBudgets(budgets)
    }

    @Query("UPDATE budgets SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun updateBudgetCategoryId(oldCategoryId: String, newCategoryId: String)
}
