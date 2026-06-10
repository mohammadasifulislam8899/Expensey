package com.xentoryx.expensey.feature.category.data.remote.api

import com.xentoryx.expensey.core.data.networking.constructUrl
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.category.data.remote.dto.CreateCategoryRequestDto
import com.xentoryx.expensey.feature.category.data.remote.dto.UpdateCategoryRequestDto
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

class CategoryApiService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {
    suspend fun createCategory(request: CreateCategoryRequestDto): HttpResponse {
        return client.post(constructUrl("/categories")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getCategories(): HttpResponse {
        return client.get(constructUrl("/categories")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }

    suspend fun updateCategory(id: String, request: UpdateCategoryRequestDto): HttpResponse {
        return client.put(constructUrl("/categories/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteCategory(id: String): HttpResponse {
        return client.delete(constructUrl("/categories/$id")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }
}
