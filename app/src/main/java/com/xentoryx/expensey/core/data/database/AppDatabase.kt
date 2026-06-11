package com.xentoryx.expensey.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.BudgetDao
import com.xentoryx.expensey.core.data.database.dao.CategoryBreakdownDao
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.data.database.dao.DashboardDao
import com.xentoryx.expensey.core.data.database.dao.RecurringTransactionDao
import com.xentoryx.expensey.core.data.database.dao.TransactionDao
import com.xentoryx.expensey.core.data.database.dao.AttachmentDao
import com.xentoryx.expensey.core.data.database.entity.AccountEntity
import com.xentoryx.expensey.core.data.database.entity.BudgetEntity
import com.xentoryx.expensey.core.data.database.entity.CategoryBreakdownEntity
import com.xentoryx.expensey.core.data.database.entity.CategoryEntity
import com.xentoryx.expensey.core.data.database.entity.DashboardOverviewEntity
import com.xentoryx.expensey.core.data.database.entity.RecurringTransactionEntity
import com.xentoryx.expensey.core.data.database.entity.TransactionEntity
import com.xentoryx.expensey.core.data.database.entity.AttachmentEntity

@Database(
    entities = [
        DashboardOverviewEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        CategoryBreakdownEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        RecurringTransactionEntity::class,
        AttachmentEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val dashboardDao: DashboardDao
    abstract val accountDao: AccountDao
    abstract val budgetDao: BudgetDao
    abstract val categoryBreakdownDao: CategoryBreakdownDao
    abstract val transactionDao: TransactionDao
    abstract val categoryDao: CategoryDao
    abstract val recurringTransactionDao: RecurringTransactionDao
    abstract val attachmentDao: AttachmentDao
}
