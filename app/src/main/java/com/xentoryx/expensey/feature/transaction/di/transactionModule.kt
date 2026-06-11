package com.xentoryx.expensey.feature.transaction.di

import com.xentoryx.expensey.feature.transaction.data.remote.api.TransactionApiService
import com.xentoryx.expensey.feature.transaction.data.repository.TransactionRepositoryImpl
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository
import com.xentoryx.expensey.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.GetTransactionsUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.UpdateTransactionUseCase
import com.xentoryx.expensey.feature.transaction.presentation.add.AddTransactionViewModel
import com.xentoryx.expensey.feature.transaction.presentation.list.TransactionsListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val transactionModule = module {
    single { TransactionApiService(get(), get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }

    factory { CreateTransactionUseCase(get()) }
    factory { GetTransactionsUseCase(get()) }
    factory { UpdateTransactionUseCase(get()) }
    factory { DeleteTransactionUseCase(get()) }

    // TransactionsListViewModel needs AccountRepository + CategoryRepository
    viewModel {
        TransactionsListViewModel(
            getTransactionsUseCase = get(),
            accountRepository = get(),
            categoryRepository = get()
        )
    }

    // AddTransactionViewModel needs AccountRepository + CategoryRepository
    viewModel {
        AddTransactionViewModel(
            createTransactionUseCase = get(),
            updateTransactionUseCase = get(),
            deleteTransactionUseCase = get(),
            transactionRepository = get(),
            accountRepository = get(),
            categoryRepository = get()
        )
    }
}
