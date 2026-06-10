package com.xentoryx.expensey.core.domain.util

sealed interface DataError : Error {
    data class Network(val error: NetworkError) : DataError
    data class Api(val message: String) : DataError
}