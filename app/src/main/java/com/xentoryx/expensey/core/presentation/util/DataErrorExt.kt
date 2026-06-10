package com.xentoryx.expensey.core.presentation.util

import android.content.Context
import com.xentoryx.expensey.core.domain.util.DataError

fun DataError.toUserMessage(context: Context): String {
    return when (this) {
        is DataError.Api -> message
        is DataError.Network -> error.toUserMessage(context)
    }
}