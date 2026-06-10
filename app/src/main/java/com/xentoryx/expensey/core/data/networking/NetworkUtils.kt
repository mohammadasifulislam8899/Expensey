package com.xentoryx.expensey.core.data.networking

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.NetworkError
import com.xentoryx.expensey.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.network.UnresolvedAddressException
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.auth.data.remote.dto.RefreshTokenRequestDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.TokenResponseDto
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.koin.core.context.GlobalContext
import kotlin.coroutines.coroutineContext

//- private const val BASE_URL = "https://your.api.com/"
private const val BASE_URL = "https://expensey-backend-pxef.onrender.com/"

fun constructUrl(url: String): String = when {
    url.contains(BASE_URL) -> url
    url.startsWith("/")    -> BASE_URL + url.drop(1)
    else                   -> BASE_URL + url
}

@Serializable
data class ErrorBody(
    val message: String,
    val error: String? = null,
    val userId: String? = null,
    val email: String? = null
)

suspend fun tryToRefreshToken(): Boolean {
    return try {
        val koin = GlobalContext.get()
        val tokenManager = koin.get<TokenManager>()
        val httpClient = koin.get<HttpClient>()
        val refreshToken = tokenManager.getRefreshToken() ?: return false

        val response = httpClient.post(constructUrl("/auth/refresh")) {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequestDto(refreshToken))
        }
        if (response.status.value in 200..299) {
            val tokenResponse = response.body<TokenResponseDto>()
            tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
            true
        } else {
            tokenManager.clearAll()
            false
        }
    } catch (e: Exception) {
        false
    }
}

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, DataError> {
    var response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        return Result.Error(DataError.Network(NetworkError.NO_INTERNET))
    } catch (e: SerializationException) {
        return Result.Error(DataError.Network(NetworkError.SERIALIZATION))
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        return Result.Error(DataError.Network(NetworkError.UNKNOWN))
    }

    if (response.status.value == 401) {
        val refreshed = tryToRefreshToken()
        if (refreshed) {
            response = try {
                execute()
            } catch (e: UnresolvedAddressException) {
                return Result.Error(DataError.Network(NetworkError.NO_INTERNET))
            } catch (e: SerializationException) {
                return Result.Error(DataError.Network(NetworkError.SERIALIZATION))
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                return Result.Error(DataError.Network(NetworkError.UNKNOWN))
            }
        }
    }

    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, DataError> = when (response.status.value) {
    in 200..299 -> {
        try {
            Result.Success(response.body<T>())
        } catch (e: NoTransformationFoundException) {
            Result.Error(DataError.Network(NetworkError.SERIALIZATION))
        }
    }
    408 -> Result.Error(DataError.Network(NetworkError.REQUEST_TIMEOUT))
    429 -> Result.Error(DataError.Network(NetworkError.TOO_MANY_REQUESTS))
    in 400..499 -> {
        val errorBody = try {
            response.body<ErrorBody>()
        } catch (_: Exception) {
            null
        }
        if (errorBody?.error == "EmailNotVerified" && errorBody.userId != null && errorBody.email != null) {
            Result.Error(DataError.EmailNotVerified(errorBody.userId, errorBody.email))
        } else {
            Result.Error(DataError.Api(errorBody?.message ?: "Something went wrong"))
        }
    }
    in 500..599 -> Result.Error(DataError.Network(NetworkError.SERVER_ERROR))
    else -> Result.Error(DataError.Network(NetworkError.UNKNOWN))
}
