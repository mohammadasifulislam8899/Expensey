package com.xentoryx.expensey.feature.dashboard.data.remote.api

import com.xentoryx.expensey.core.data.networking.constructUrl
import com.xentoryx.expensey.core.storage.TokenManager
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class DashboardApiService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    suspend fun getDashboardSummary(): HttpResponse {
        return client.get(constructUrl("/dashboard")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }
}
