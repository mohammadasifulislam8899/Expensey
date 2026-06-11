package com.xentoryx.expensey.feature.category.di

import com.xentoryx.expensey.feature.category.data.remote.api.CategoryApiService
import com.xentoryx.expensey.feature.category.data.repository.CategoryRepositoryImpl
import com.xentoryx.expensey.feature.category.domain.repository.CategoryRepository
import com.xentoryx.expensey.feature.category.domain.usecase.CreateCategoryUseCase
import com.xentoryx.expensey.feature.category.domain.usecase.DeleteCategoryUseCase
import com.xentoryx.expensey.feature.category.domain.usecase.GetCategoriesUseCase
import com.xentoryx.expensey.feature.category.domain.usecase.UpdateCategoryUseCase
import com.xentoryx.expensey.feature.category.presentation.CategoriesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val categoryModule = module {
    single { CategoryApiService(get(), get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get()) }

    factory { CreateCategoryUseCase(get()) }
    factory { GetCategoriesUseCase(get()) }
    factory { UpdateCategoryUseCase(get()) }
    factory { DeleteCategoryUseCase(get()) }

    viewModelOf(::CategoriesViewModel)
}
