package com.xentoryx.expensey.feature.transaction.di

import com.xentoryx.expensey.feature.transaction.data.remote.api.TransactionApiService
import com.xentoryx.expensey.feature.transaction.data.repository.TransactionRepositoryImpl
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository
import com.xentoryx.expensey.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.GetTransactionsUseCase
import com.xentoryx.expensey.feature.transaction.presentation.add.AddTransactionViewModel
import com.xentoryx.expensey.feature.transaction.presentation.list.TransactionsListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val transactionModule = module {
    single { TransactionApiService(get(), get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get(), get(), get(), get(), get(), get()) }

    factory { CreateTransactionUseCase(get()) }
    factory { GetTransactionsUseCase(get()) }
    factory { com.xentoryx.expensey.feature.transaction.domain.usecase.UpdateTransactionUseCase(get()) }
    factory { com.xentoryx.expensey.feature.transaction.domain.usecase.DeleteTransactionUseCase(get()) }

    viewModelOf(::TransactionsListViewModel)
    viewModelOf(::AddTransactionViewModel)
}
