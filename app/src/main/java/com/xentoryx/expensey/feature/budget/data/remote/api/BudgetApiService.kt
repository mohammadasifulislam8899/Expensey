package com.xentoryx.expensey.feature.budget.data.remote.api

import com.xentoryx.expensey.core.data.networking.constructUrl
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.budget.data.remote.dto.CreateBudgetRequestDto
import com.xentoryx.expensey.feature.budget.data.remote.dto.UpdateBudgetRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

class BudgetApiService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    suspend fun createBudget(request: CreateBudgetRequestDto): HttpResponse {
        return client.post(constructUrl("/budgets")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getBudgets(): HttpResponse {
        return client.get(constructUrl("/budgets")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }

    suspend fun updateBudget(id: String, request: UpdateBudgetRequestDto): HttpResponse {
        return client.put(constructUrl("/budgets/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteBudget(id: String): HttpResponse {
        return client.delete(constructUrl("/budgets/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }
}
