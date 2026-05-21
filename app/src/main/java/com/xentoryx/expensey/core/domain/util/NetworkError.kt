package com.xentoryx.expensey.core.domain.util

enum class NetworkError : Error {
    REQUEST_TIMEOUT,    // 408
    TOO_MANY_REQUESTS,  // 429
    NO_INTERNET,        // DNS fail / no connection
    SERVER_ERROR,       // 5xx
    SERIALIZATION,      // JSON parse error
    UNKNOWN
}