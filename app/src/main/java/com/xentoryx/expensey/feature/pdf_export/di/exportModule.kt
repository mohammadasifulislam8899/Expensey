package com.xentoryx.expensey.feature.pdf_export.di

import com.xentoryx.expensey.feature.pdf_export.data.remote.api.ExportApiService
import com.xentoryx.expensey.feature.pdf_export.data.repository.ExportRepositoryImpl
import com.xentoryx.expensey.feature.pdf_export.domain.repository.ExportRepository
import com.xentoryx.expensey.feature.pdf_export.domain.usecase.ExportPdfReportUseCase
import com.xentoryx.expensey.feature.pdf_export.presentation.PdfExportViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val exportModule = module {
    single { ExportApiService(get(), get()) }
    single<ExportRepository> { ExportRepositoryImpl(get()) }
    factory { ExportPdfReportUseCase(get()) }
    viewModelOf(::PdfExportViewModel)
}
