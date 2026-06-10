package com.xentoryx.expensey.feature.pdf_export.data.repository

import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.pdf_export.data.remote.api.ExportApiService
import com.xentoryx.expensey.feature.pdf_export.domain.repository.ExportRepository

class ExportRepositoryImpl(
    private val apiService: ExportApiService
) : ExportRepository {
    override suspend fun getPdfReport(from: String, to: String): Result<ByteArray, DataError> {
        return safeCall<ByteArray> {
            apiService.downloadPdfReport(from, to)
        }
    }

    override suspend fun getCsvReport(from: String, to: String): Result<ByteArray, DataError> {
        return safeCall<ByteArray> {
            apiService.downloadCsvReport(from, to)
        }
    }
}
