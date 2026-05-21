package com.xentoryx.expensey.core.data.networking

import com.xentoryx.expensey.core.domain.util.NetworkError

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import kotlin.coroutines.coroutineContext
import com.xentoryx.expensey.core.domain.util.Result

private const val BASE_URL = "https://your.api.com/"

fun constructUrl(url: String): String = when {
    url.contains(BASE_URL) -> url
    url.startsWith("/")    -> BASE_URL + url.drop(1)
    else                   -> BASE_URL + url
}

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, NetworkError> {
    val response = try {
        execute()
    } catch (e: UnresolvedAddressException) {
        return Result.Error(NetworkError.NO_INTERNET)
    } catch (e: SerializationException) {
        return Result.Error(NetworkError.SERIALIZATION)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        return Result.Error(NetworkError.UNKNOWN)
    }
    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, NetworkError> = when (response.status.value) {
    in 200..299 -> try {
        Result.Success(response.body<T>())
    } catch (e: NoTransformationFoundException) {
        Result.Error(NetworkError.SERIALIZATION)
    }
    408         -> Result.Error(NetworkError.REQUEST_TIMEOUT)
    429         -> Result.Error(NetworkError.TOO_MANY_REQUESTS)
    in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
    else        -> Result.Error(NetworkError.UNKNOWN)
}