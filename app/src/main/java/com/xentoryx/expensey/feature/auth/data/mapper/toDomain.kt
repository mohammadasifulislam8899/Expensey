package com.xentoryx.expensey.feature.auth.data.mapper

import com.xentoryx.expensey.feature.auth.data.remote.dto.AuthResponseDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.RegisterResponseDto
import com.xentoryx.expensey.feature.auth.data.remote.dto.UserResponseDto
import com.xentoryx.expensey.feature.auth.domain.model.AuthResult
import com.xentoryx.expensey.feature.auth.domain.model.RegisterResult
import com.xentoryx.expensey.feature.auth.domain.model.User


fun UserResponseDto.toDomain(): User = User(
    id = id,
    email = email,
    fullName = fullName,
    currencyCode = currencyCode,
    countryCode = countryCode,
    isEmailVerified = isEmailVerified,
    isActive = isActive
)

fun AuthResponseDto.toDomain(): AuthResult = AuthResult(
    user = user.toDomain(),
    accessToken = accessToken,
    refreshToken = refreshToken
)

fun RegisterResponseDto.toDomain(): RegisterResult = RegisterResult(
    user = user.toDomain(),
    message = message
)