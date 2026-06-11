package com.xentoryx.expensey.feature.auth.data.remote.api

import com.xentoryx.expensey.core.data.networking.constructUrl
import com.xentoryx.expensey.core.storage.TokenManager
import com.xentoryx.expensey.feature.auth.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse

class AuthApiService(
    private val client: HttpClient,
    private val tokenManager: TokenManager
) {

    // ---- Public endpoints (no auth needed) ----

    suspend fun register(request: RegisterRequestDto): HttpResponse {
        return client.post(constructUrl("/auth/register")) {
            setBody(request)
        }
    }

    suspend fun login(request: LoginRequestDto): HttpResponse {
        return client.post(constructUrl("/auth/login")) {
            setBody(request)
        }
    }

    suspend fun verifyEmail(request: VerifyEmailRequestDto): HttpResponse {
        return client.post(constructUrl("/auth/verify-email")) {
            setBody(request)
        }
    }

    suspend fun resendOtp(request: ResendOtpRequestDto): HttpResponse {
        return client.post(constructUrl("/auth/resend-otp")) {
            setBody(request)
        }
    }

    suspend fun refreshToken(request: RefreshTokenRequestDto): HttpResponse {
        return client.post(constructUrl("/auth/refresh")) {
            setBody(request)
        }
    }

    suspend fun forgotPassword(request: ForgotPasswordRequestDto): HttpResponse {
        return client.post(constructUrl("/auth/forgot-password")) {
            setBody(request)
        }
    }

    suspend fun resetPassword(request: ResetPasswordRequestDto): HttpResponse {
        return client.post(constructUrl("/auth/reset-password")) {
            setBody(request)
        }
    }

    // ---- Authenticated endpoints (Bearer token) ----

    suspend fun getProfile(): HttpResponse {
        return client.get(constructUrl("/auth/me")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequestDto): HttpResponse {
        return client.put(constructUrl("/auth/me")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            setBody(request)
        }
    }

    suspend fun changePassword(request: ChangePasswordRequestDto): HttpResponse {
        return client.post(constructUrl("/auth/change-password")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
            setBody(request)
        }
    }

    suspend fun logout(): HttpResponse {
        return client.post(constructUrl("/auth/logout")) {
            bearerAuth(tokenManager.getAccessToken() ?: "")
        }
    }
}