package com.xentoryx.expensey.feature.category.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.category.domain.model.Category
import com.xentoryx.expensey.feature.category.domain.usecase.CreateCategoryUseCase
import com.xentoryx.expensey.feature.category.domain.usecase.DeleteCategoryUseCase
import com.xentoryx.expensey.feature.category.domain.usecase.GetCategoriesUseCase
import com.xentoryx.expensey.feature.category.domain.usecase.UpdateCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isSuccess = MutableStateFlow(false)
    private val _formState = MutableStateFlow(CategoriesState())

    val state: StateFlow<CategoriesState> = combine(
        getCategoriesUseCase(),
        _isLoading,
        _errorMessage,
        _isSuccess,
        _formState
    ) { categories, isLoading, errorMessage, isSuccess, formState ->
        CategoriesState(
            categories = categories,
            isLoading = isLoading,
            errorMessage = errorMessage,
            isSuccess = isSuccess,
            selectedCategory = formState.selectedCategory,
            name = formState.name,
            type = formState.type,
            color = formState.color,
            icon = formState.icon,
            isFormOpen = formState.isFormOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesState()
    )

    init {
        refreshCategories()
    }

    fun refreshCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (getCategoriesUseCase.sync()) {
                is Result.Success -> {}
                is Result.Error -> {
                    _errorMessage.value = "Failed to sync categories from server"
                }
            }
            _isLoading.value = false
        }
    }

    fun openForm(category: Category? = null) {
        _formState.update {
            if (category != null) {
                it.copy(
                    selectedCategory = category,
                    name = category.name,
                    type = category.type,
                    color = category.color ?: "#7C67E6",
                    icon = category.icon ?: "💰",
                    isFormOpen = true
                )
            } else {
                it.copy(
                    selectedCategory = null,
                    name = "",
                    type = "EXPENSE",
                    color = "#7C67E6",
                    icon = "💰",
                    isFormOpen = true
                )
            }
        }
        _isSuccess.value = false
        _errorMessage.value = null
    }

    fun closeForm() {
        _formState.update { it.copy(isFormOpen = false) }
    }

    fun onNameChange(name: String) {
        _formState.update { it.copy(name = name) }
    }

    fun onTypeChange(type: String) {
        _formState.update { it.copy(type = type) }
    }

    fun onColorChange(color: String) {
        _formState.update { it.copy(color = color) }
    }

    fun onIconChange(icon: String) {
        _formState.update { it.copy(icon = icon) }
    }

    fun saveCategory() {
        val form = _formState.value
        val name = form.name.trim()

        if (name.isBlank()) {
            _errorMessage.value = "Category name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = if (form.selectedCategory != null) {
                updateCategoryUseCase(
                    id = form.selectedCategory.id,
                    name = name,
                    icon = form.icon,
                    color = form.color
                )
            } else {
                createCategoryUseCase(
                    name = name,
                    type = form.type,
                    icon = form.icon,
                    color = form.color
                )
            }

            when (result) {
                is Result.Success -> {
                    _isSuccess.value = true
                    closeForm()
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to save category"
                }
            }
            _isLoading.value = false
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (deleteCategoryUseCase(categoryId)) {
                is Result.Success -> {
                    _isSuccess.value = true
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to delete category"
                }
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
