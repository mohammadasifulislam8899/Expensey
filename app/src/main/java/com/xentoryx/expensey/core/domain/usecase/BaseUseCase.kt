package com.xentoryx.expensey.core.domain.usecase

import com.xentoryx.expensey.core.domain.util.Error
import com.xentoryx.expensey.core.domain.util.Result

interface BaseUseCase<in Params, out D, out E : Error> {
    suspend operator fun invoke(params: Params): Result<D, E>
}

interface NoParamsUseCase<out D, out E : Error> {
    suspend operator fun invoke(): Result<D, E>
}