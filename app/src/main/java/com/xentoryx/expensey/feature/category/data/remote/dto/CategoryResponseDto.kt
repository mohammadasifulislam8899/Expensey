package com.xentoryx.expensey.feature.category.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponseDto(
    val id: String,
    val name: String,
    val type: String,
    val parentId: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val isSystem: Boolean
)
