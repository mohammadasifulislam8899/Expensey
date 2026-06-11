package com.xentoryx.expensey.feature.category.presentation

import com.xentoryx.expensey.feature.category.domain.model.Category

data class CategoriesState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    
    // Form fields
    val selectedCategory: Category? = null,
    val name: String = "",
    val type: String = "EXPENSE",
    val color: String = "#7C67E6",
    val icon: String = "💰",
    val isFormOpen: Boolean = false
)
