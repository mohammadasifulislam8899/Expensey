package com.xentoryx.expensey.feature.dashboard.di

import com.xentoryx.expensey.feature.dashboard.data.remote.api.DashboardApiService
import com.xentoryx.expensey.feature.dashboard.data.repository.DashboardRepositoryImpl
import com.xentoryx.expensey.feature.dashboard.domain.repository.DashboardRepository
import com.xentoryx.expensey.feature.dashboard.domain.usecase.GetDashboardUseCase
import com.xentoryx.expensey.feature.dashboard.presentation.dashboard.DashboardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dashboardModule = module {
    single { DashboardApiService(get(), get()) }
    single<DashboardRepository> { DashboardRepositoryImpl(get()) }
    factory { GetDashboardUseCase(get()) }
    viewModelOf(::DashboardViewModel)
}
