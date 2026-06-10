package com.xentoryx.expensey.feature.auth.di

import com.xentoryx.expensey.feature.auth.data.remote.api.AuthApiService
import com.xentoryx.expensey.feature.auth.data.repository.AuthRepositoryImpl
import com.xentoryx.expensey.feature.auth.domain.repository.AuthRepository
import com.xentoryx.expensey.feature.auth.domain.usecase.*
import com.xentoryx.expensey.feature.auth.presentation.forgot_password.ForgotPasswordViewModel
import com.xentoryx.expensey.feature.auth.presentation.login.LoginViewModel
import com.xentoryx.expensey.feature.auth.presentation.register.RegisterViewModel
import com.xentoryx.expensey.feature.auth.presentation.reset_password.ResetPasswordViewModel
import com.xentoryx.expensey.feature.auth.presentation.verify_email.VerifyEmailViewModel
import com.xentoryx.expensey.core.storage.TokenManager
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {

    // Core
    single { TokenManager(get()) }

    // Data
    single { AuthApiService(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    // Use Cases
    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { VerifyEmailUseCase(get()) }
    factory { ResendOtpUseCase(get()) }
    factory { ForgotPasswordUseCase(get()) }
    factory { ResetPasswordUseCase(get()) }
    factory { GetProfileUseCase(get()) }
    factory { LogoutUseCase(get()) }

    // ViewModels
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::VerifyEmailViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::ResetPasswordViewModel)
}