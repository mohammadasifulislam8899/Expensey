package com.xentoryx.expensey.feature.accounts.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateAccountRequestDto(
    val name: String,
    val type: String,
    val currencyCode: String? = null
)
