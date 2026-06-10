package com.xentoryx.expensey.feature.accounts.di

import com.xentoryx.expensey.feature.accounts.data.remote.api.AccountApiService
import com.xentoryx.expensey.feature.accounts.data.repository.AccountRepositoryImpl
import com.xentoryx.expensey.feature.accounts.domain.repository.AccountRepository
import com.xentoryx.expensey.feature.accounts.domain.usecase.CreateAccountUseCase
import com.xentoryx.expensey.feature.accounts.domain.usecase.GetAccountsUseCase
import com.xentoryx.expensey.feature.accounts.presentation.add.AddAccountViewModel
import com.xentoryx.expensey.feature.accounts.presentation.detail.AccountDetailViewModel
import com.xentoryx.expensey.feature.accounts.presentation.list.AccountsListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountsModule = module {
    single { AccountApiService(get(), get()) }
    single<AccountRepository> { AccountRepositoryImpl(get(), get(), get()) }

    factory { CreateAccountUseCase(get()) }
    factory { GetAccountsUseCase(get()) }
    factory { com.xentoryx.expensey.feature.accounts.domain.usecase.UpdateAccountUseCase(get()) }
    factory { com.xentoryx.expensey.feature.accounts.domain.usecase.DeleteAccountUseCase(get()) }

    viewModelOf(::AccountsListViewModel)
    viewModelOf(::AddAccountViewModel)
    viewModelOf(::AccountDetailViewModel)
}
