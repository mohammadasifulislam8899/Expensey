package com.xentoryx.expensey.feature.auth.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val currencyCode: String,
    val countryCode: String,
    val isEmailVerified: Boolean,
    val isActive: Boolean
)