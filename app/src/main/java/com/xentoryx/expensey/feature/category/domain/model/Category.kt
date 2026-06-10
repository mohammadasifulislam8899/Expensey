package com.xentoryx.expensey.feature.category.domain.model

data class Category(
    val id: String,
    val name: String,
    val type: String,
    val parentId: String?,
    val icon: String?,
    val color: String?,
    val isSystem: Boolean
)
