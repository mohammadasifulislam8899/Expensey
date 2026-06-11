package com.xentoryx.expensey.feature.recurring_transaction.data.repository

import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.api.RecurringApiService
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto.CreateRecurringTransactionRequestDto
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto.RecurringTransactionResponseDto
import com.xentoryx.expensey.feature.recurring_transaction.data.remote.dto.UpdateRecurringTransactionRequestDto
import com.xentoryx.expensey.feature.recurring_transaction.domain.model.RecurringTransaction
import com.xentoryx.expensey.feature.recurring_transaction.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class RecurringRepositoryImpl(
    private val apiService: RecurringApiService
) : RecurringRepository {

    private val _recurringFlow = MutableStateFlow<List<RecurringTransaction>>(emptyList())

    override fun getRecurringTransactionsFlow(): Flow<List<RecurringTransaction>> = _recurringFlow.asStateFlow()

    override suspend fun createRecurringTransaction(
        accountId: String,
        categoryId: String,
        amount: Double,
        type: String,
        frequency: String,
        note: String?,
        startDate: String?,
        endDate: String?
    ): Result<RecurringTransaction, DataError> {
        val result = safeCall<RecurringTransactionResponseDto> {
            apiService.createRecurringTransaction(
                CreateRecurringTransactionRequestDto(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type,
                    frequency = frequency,
                    note = note,
                    startDate = startDate ?: LocalDate.now().toString(),
                    endDate = endDate
                )
            )
        }
        return when (result) {
            is Result.Success -> {
                val recurring = result.data.toDomain()
                _recurringFlow.value = listOf(recurring) + _recurringFlow.value
                Result.Success(recurring)
            }
            is Result.Error -> result
        }
    }

    override suspend fun updateRecurringTransaction(
        id: String,
        accountId: String,
        categoryId: String,
        amount: Double,
        type: String,
        frequency: String,
        note: String?,
        startDate: String?,
        endDate: String?
    ): Result<RecurringTransaction, DataError> {
        val result = safeCall<RecurringTransactionResponseDto> {
            apiService.updateRecurringTransaction(
                id,
                UpdateRecurringTransactionRequestDto(
                    accountId = accountId,
                    categoryId = categoryId,
                    amount = amount,
                    type = type,
                    frequency = frequency,
                    note = note,
                    startDate = startDate,
                    endDate = endDate
                )
            )
        }
        return when (result) {
            is Result.Success -> {
                val updated = result.data.toDomain()
                _recurringFlow.value = _recurringFlow.value.map { if (it.id == id) updated else it }
                Result.Success(updated)
            }
            is Result.Error -> result
        }
    }

    override suspend fun deleteRecurringTransaction(id: String): Result<Unit, DataError> {
        val result = safeCall<Unit> { apiService.deleteRecurringTransaction(id) }
        return when (result) {
            is Result.Success -> {
                _recurringFlow.value = _recurringFlow.value.filter { it.id != id }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }

    override suspend fun syncRecurringTransactions(): Result<Unit, DataError> {
        val result = safeCall<List<RecurringTransactionResponseDto>> { apiService.getRecurringTransactions() }
        return when (result) {
            is Result.Success -> {
                _recurringFlow.value = result.data.map { it.toDomain() }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }
}

fun RecurringTransactionResponseDto.toDomain() = RecurringTransaction(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amount = amount,
    type = type,
    frequency = frequency,
    note = note,
    startDate = startDate,
    endDate = endDate,
    nextRunDate = nextRunDate,
    isActive = isActive,
    createdAt = createdAt
)
