package com.xentoryx.expensey.feature.accounts.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequestDto(
    val name: String,
    val type: String,
    val initialBalance: Double? = 0.0,
    val currencyCode: String? = "BDT"
)
