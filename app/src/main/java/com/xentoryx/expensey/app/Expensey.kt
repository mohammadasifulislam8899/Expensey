package com.xentoryx.expensey.app

import android.app.Application
import com.xentoryx.expensey.core.di.databaseModule
import com.xentoryx.expensey.core.di.networkModule
import com.xentoryx.expensey.feature.accounts.di.accountsModule
import com.xentoryx.expensey.feature.auth.di.authModule
import com.xentoryx.expensey.feature.budget.di.budgetModule
import com.xentoryx.expensey.feature.category.di.categoryModule
import com.xentoryx.expensey.feature.dashboard.di.dashboardModule
import com.xentoryx.expensey.feature.pdf_export.di.exportModule
import com.xentoryx.expensey.feature.recurring_transaction.di.recurringModule
import com.xentoryx.expensey.feature.transaction.di.transactionModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class Expensey: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@Expensey)
            modules(
                networkModule,
                databaseModule,
                authModule,
                dashboardModule,
                transactionModule,
                accountsModule,
                budgetModule,
                categoryModule,
                recurringModule,
                exportModule
            )
        }
    }
}