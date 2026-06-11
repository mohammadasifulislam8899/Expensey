package com.xentoryx.expensey.feature.recurring_transaction.di

import com.xentoryx.expensey.feature.recurring_transaction.data.remote.api.RecurringApiService
import com.xentoryx.expensey.feature.recurring_transaction.data.repository.RecurringRepositoryImpl
import com.xentoryx.expensey.feature.recurring_transaction.domain.repository.RecurringRepository
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.CreateRecurringTransactionUseCase
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.DeleteRecurringTransactionUseCase
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.GetRecurringTransactionsUseCase
import com.xentoryx.expensey.feature.recurring_transaction.domain.usecase.UpdateRecurringTransactionUseCase
import com.xentoryx.expensey.feature.recurring_transaction.presentation.form.RecurringFormViewModel
import com.xentoryx.expensey.feature.recurring_transaction.presentation.list.RecurringListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val recurringModule = module {
    single { RecurringApiService(get(), get()) }
    single<RecurringRepository> { RecurringRepositoryImpl(get()) }

    factory { CreateRecurringTransactionUseCase(get()) }
    factory { GetRecurringTransactionsUseCase(get()) }
    factory { UpdateRecurringTransactionUseCase(get()) }
    factory { DeleteRecurringTransactionUseCase(get()) }

    // RecurringListViewModel needs AccountRepository + CategoryRepository
    viewModel {
        RecurringListViewModel(
            getRecurringTransactionsUseCase = get(),
            deleteRecurringTransactionUseCase = get(),
            accountRepository = get(),
            categoryRepository = get()
        )
    }

    // RecurringFormViewModel needs AccountRepository + CategoryRepository + RecurringRepository
    viewModel {
        RecurringFormViewModel(
            createRecurringTransactionUseCase = get(),
            updateRecurringTransactionUseCase = get(),
            recurringRepository = get(),
            accountRepository = get(),
            categoryRepository = get()
        )
    }
}
