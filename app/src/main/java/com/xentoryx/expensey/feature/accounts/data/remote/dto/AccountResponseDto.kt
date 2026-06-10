package com.xentoryx.expensey.feature.accounts.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountResponseDto(
    val id: String,
    val name: String,
    val type: String,
    val balance: Double,
    val currencyCode: String,
    val isActive: Boolean,
    val createdAt: String
)
