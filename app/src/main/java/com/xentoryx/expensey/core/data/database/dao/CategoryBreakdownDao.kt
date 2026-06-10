package com.xentoryx.expensey.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xentoryx.expensey.core.data.database.entity.CategoryBreakdownEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBreakdownDao {
    @Query("SELECT * FROM category_breakdowns")
    fun getBreakdownsFlow(): Flow<List<CategoryBreakdownEntity>>

    @Query("SELECT * FROM category_breakdowns")
    suspend fun getBreakdowns(): List<CategoryBreakdownEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakdowns(breakdowns: List<CategoryBreakdownEntity>)

    @Query("DELETE FROM category_breakdowns")
    suspend fun deleteAllBreakdowns()

    @Transaction
    suspend fun replaceBreakdowns(breakdowns: List<CategoryBreakdownEntity>) {
        deleteAllBreakdowns()
        insertBreakdowns(breakdowns)
    }
}
