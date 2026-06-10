package com.xentoryx.expensey.core.data.networking

import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.NetworkError
import com.xentoryx.expensey.core.domain.util.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.coroutines.coroutineContext

//- private const val BASE_URL = "https://your.api.com/"
private const val BASE_URL = "https://expensey-backend-pxef.onrender.com/"

fun constructUrl(url: String): String = when {
    url.contains(BASE_URL) -> url
    url.startsWith("/")    -> BASE_URL + url.drop(1)
    else                   -> BASE_URL + url
}

@Serializable
data class ErrorBody(val message: String)

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, DataError> {
    val response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        return Result.Error(DataError.Network(NetworkError.NO_INTERNET))
    } catch (e: SerializationException) {
        return Result.Error(DataError.Network(NetworkError.SERIALIZATION))
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        return Result.Error(DataError.Network(NetworkError.UNKNOWN))
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
        val message = try {
            response.body<ErrorBody>().message
        } catch (_: Exception) {
            "Something went wrong"
        }
        Result.Error(DataError.Api(message))
    }
    in 500..599 -> Result.Error(DataError.Network(NetworkError.SERVER_ERROR))
    else -> Result.Error(DataError.Network(NetworkError.UNKNOWN))
}
