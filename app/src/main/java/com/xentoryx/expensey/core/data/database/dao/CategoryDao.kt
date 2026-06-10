package com.xentoryx.expensey.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xentoryx.expensey.core.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0")
    fun getCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isDeleted = 0")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id AND isDeleted = 0")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE isDeleted = 1")
    suspend fun getUnsyncedDeletions(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun markCategoryDeleted(id: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM categories WHERE isSynced = 1 AND isDeleted = 0")
    suspend fun deleteSyncedCategories()

    @Transaction
    suspend fun replaceCategories(categories: List<CategoryEntity>) {
        deleteSyncedCategories()
        insertCategories(categories)
    }
}
