package com.xentoryx.expensey.feature.recurring_transaction.data.remote.api

import com.xentoryx.expensey.core.data.networking.constructUrl
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto.CreateRecurringTransactionRequestDto
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto.UpdateRecurringTransactionRequestDto
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

class RecurringApiService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    suspend fun createRecurringTransaction(request: CreateRecurringTransactionRequestDto): HttpResponse {
        return client.post(constructUrl("/recurring-transactions")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getRecurringTransactions(): HttpResponse {
        return client.get(constructUrl("/recurring-transactions")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }

    suspend fun updateRecurringTransaction(id: String, request: UpdateRecurringTransactionRequestDto): HttpResponse {
        return client.put(constructUrl("/recurring-transactions/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteRecurringTransaction(id: String): HttpResponse {
        return client.delete(constructUrl("/recurring-transactions/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }
}
