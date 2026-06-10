package com.xentoryx.expensey.feature.pdf_export.domain.repository

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result

interface ExportRepository {
    suspend fun getPdfReport(from: String, to: String): Result<ByteArray, DataError>
}
