package com.xentoryx.expensey.core.presentation.util

import android.content.Context
import com.xentoryx.expensey.R
import com.xentoryx.expensey.core.domain.util.NetworkError

fun NetworkError.toUserMessage(context: Context): String {
    val resId = when (this) {
        NetworkError.REQUEST_TIMEOUT   -> R.string.error_request_timeout
        NetworkError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
        NetworkError.NO_INTERNET       -> R.string.error_no_internet
        NetworkError.SERVER_ERROR      -> R.string.error_unknown
        NetworkError.SERIALIZATION     -> R.string.error_serialization
        NetworkError.UNKNOWN           -> R.string.error_unknown
    }
    return context.getString(resId)
}