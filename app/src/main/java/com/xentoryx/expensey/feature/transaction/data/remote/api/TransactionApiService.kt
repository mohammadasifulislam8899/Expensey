package com.xentoryx.expensey.feature.transaction.data.remote.api

import com.xentoryx.expensey.core.data.networking.constructUrl
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.transaction.data.remote.dto.CreateTransactionRequestDto
import com.xentoryx.expensey.feature.transaction.data.remote.dto.UpdateTransactionRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

class TransactionApiService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    suspend fun createTransaction(request: CreateTransactionRequestDto): HttpResponse {
        return client.post(constructUrl("/transactions")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getTransactions(page: Int, limit: Int): HttpResponse {
        return client.get(constructUrl("/transactions")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            parameter("page", page)
            parameter("limit", limit)
        }
    }

    suspend fun updateTransaction(id: String, request: UpdateTransactionRequestDto): HttpResponse {
        return client.put(constructUrl("/transactions/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteTransaction(id: String): HttpResponse {
        return client.delete(constructUrl("/transactions/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }
}
