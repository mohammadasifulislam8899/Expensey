package com.xentoryx.expensey.feature.pdf_export.data.remote.api

import com.xentoryx.expensey.core.data.networking.constructUrl
import com.xentoryx.expensey.core.storage.TokenManager
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse

class ExportApiService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    suspend fun downloadPdfReport(from: String, to: String): HttpResponse {
        return client.get(constructUrl("/export/pdf")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            parameter("from", from)
            parameter("to", to)
        }
    }

    suspend fun downloadCsvReport(from: String, to: String): HttpResponse {
        return client.get(constructUrl("/export/csv")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            parameter("from", from)
            parameter("to", to)
        }
    }
}
