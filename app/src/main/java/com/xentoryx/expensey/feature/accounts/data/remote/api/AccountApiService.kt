package com.xentoryx.expensey.feature.accounts.data.remote.api

import com.xentoryx.expensey.core.data.networking.constructUrl
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.accounts.data.remote.dto.CreateAccountRequestDto
import com.xentoryx.expensey.feature.accounts.data.remote.dto.UpdateAccountRequestDto
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

class AccountApiService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    suspend fun createAccount(request: CreateAccountRequestDto): HttpResponse {
        return client.post(constructUrl("/accounts")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getAccounts(): HttpResponse {
        return client.get(constructUrl("/accounts")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }

    suspend fun updateAccount(id: String, request: UpdateAccountRequestDto): HttpResponse {
        return client.put(constructUrl("/accounts/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteAccount(id: String): HttpResponse {
        return client.delete(constructUrl("/accounts/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }
}
