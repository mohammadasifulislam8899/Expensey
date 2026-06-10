package com.xentoryx.expensey.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xentoryx.expensey.core.data.database.entity.DashboardOverviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query("SELECT * FROM dashboard_overview WHERE id = 1 LIMIT 1")
    fun getOverviewFlow(): Flow<DashboardOverviewEntity?>

    @Query("SELECT * FROM dashboard_overview WHERE id = 1 LIMIT 1")
    suspend fun getOverview(): DashboardOverviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverview(overview: DashboardOverviewEntity)
}
