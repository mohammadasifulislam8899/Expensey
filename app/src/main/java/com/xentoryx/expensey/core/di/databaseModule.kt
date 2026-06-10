package com.xentoryx.expensey.core.di

import androidx.room.Room
import com.xentoryx.expensey.core.data.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "expensey.db"
        ).fallbackToDestructiveMigration(dropAllTables = true)
         .build()
    }

    single { get<AppDatabase>().dashboardDao }
    single { get<AppDatabase>().accountDao }
    single { get<AppDatabase>().budgetDao }
    single { get<AppDatabase>().categoryBreakdownDao }
    single { get<AppDatabase>().transactionDao }
    single { get<AppDatabase>().categoryDao }
    single { get<AppDatabase>().recurringTransactionDao }
    single { get<AppDatabase>().attachmentDao }
}
