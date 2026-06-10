package com.xentoryx.expensey.feature.category.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCategoryRequestDto(
    val name: String,
    val icon: String? = null,
    val color: String? = null
)
