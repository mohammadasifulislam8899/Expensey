package com.xentoryx.expensey.feature.budget.di

import com.xentoryx.expensey.feature.budget.data.remote.api.BudgetApiService
import com.xentoryx.expensey.feature.budget.data.repository.BudgetRepositoryImpl
import com.xentoryx.expensey.feature.budget.domain.repository.BudgetRepository
import com.xentoryx.expensey.feature.budget.domain.usecase.CreateBudgetUseCase
import com.xentoryx.expensey.feature.budget.domain.usecase.DeleteBudgetUseCase
import com.xentoryx.expensey.feature.budget.domain.usecase.GetBudgetsUseCase
import com.xentoryx.expensey.feature.budget.domain.usecase.UpdateBudgetUseCase
import com.xentoryx.expensey.feature.budget.presentation.form.BudgetFormViewModel
import com.xentoryx.expensey.feature.budget.presentation.list.BudgetsListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val budgetModule = module {
    single { BudgetApiService(get(), get()) }
    single<BudgetRepository> { BudgetRepositoryImpl(get()) }

    factory { CreateBudgetUseCase(get()) }
    factory { GetBudgetsUseCase(get()) }
    factory { UpdateBudgetUseCase(get()) }
    factory { DeleteBudgetUseCase(get()) }

    viewModelOf(::BudgetsListViewModel)

    // BudgetFormViewModel needs CategoryRepository
    viewModel {
        BudgetFormViewModel(
            createBudgetUseCase = get(),
            updateBudgetUseCase = get(),
            deleteBudgetUseCase = get(),
            categoryRepository = get()
        )
    }
}
