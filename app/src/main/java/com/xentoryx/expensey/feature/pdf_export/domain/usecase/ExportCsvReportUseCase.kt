package com.xentoryx.expensey.feature.pdf_export.domain.usecase

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.pdf_export.domain.repository.ExportRepository

class ExportCsvReportUseCase(
    private val repository: ExportRepository
) {
    suspend operator fun invoke(from: String, to: String): Result<ByteArray, DataError> {
        if (from.isBlank() || to.isBlank()) {
            return Result.Error(DataError.Api("Start and end dates are required"))
        }
        return repository.getCsvReport(from, to)
    }
}
