package com.xentoryx.expensey.feature.budget.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xentoryx.expensey.core.data.database.dao.BudgetDao
import com.xentoryx.expensey.core.data.database.dao.CategoryBreakdownDao
import com.xentoryx.expensey.core.data.database.entity.BudgetEntity
import com.xentoryx.expensey.core.data.networking.safeCall
import com.xentoryx.expensey.core.domain.util.DataError
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.core.sync.SyncBudgetsWorker
import com.xentoryx.expensey.feature.budget.data.remote.api.BudgetApiService
import com.xentoryx.expensey.feature.budget.data.remote.dto.BudgetResponseDto
import com.xentoryx.expensey.feature.budget.domain.model.Budget
import com.xentoryx.expensey.feature.budget.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class BudgetRepositoryImpl(
    private val context: Context,
    private val budgetDao: BudgetDao,
    private val categoryBreakdownDao: CategoryBreakdownDao,
    private val apiService: BudgetApiService
) : BudgetRepository {

    override fun getBudgetsFlow(): Flow<List<Budget>> {
        return budgetDao.getBudgetsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createBudget(
        categoryId: String,
        amountLimit: Double,
        period: String,
        startDate: String?,
        endDate: String?
    ): Result<Budget, DataError> {
        val budgetId = UUID.randomUUID().toString()

        var categoryName = "Category"
        var categoryIcon: String? = null
        var categoryColor: String? = null
        try {
            val category = categoryBreakdownDao.getBreakdowns().find { it.categoryId == categoryId }
            if (category != null) {
                categoryName = category.categoryName
                categoryIcon = category.categoryIcon
                categoryColor = category.categoryColor
            }
        } catch (e: Exception) {
            // Ignore DB errors
        }

        val resolvedStartDate = startDate ?: LocalDate.now().withDayOfMonth(1).toString()
        val parsedStart = LocalDate.parse(resolvedStartDate)
        val resolvedEndDate = endDate ?: when (period.uppercase()) {
            "WEEKLY" -> parsedStart.plusWeeks(1).minusDays(1).toString()
            "YEARLY" -> parsedStart.withDayOfYear(parsedStart.lengthOfYear()).toString()
            else -> parsedStart.withDayOfMonth(parsedStart.lengthOfMonth()).toString() // MONTHLY
        }

        val entity = BudgetEntity(
            id = budgetId,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            amountLimit = amountLimit,
            period = period,
            startDate = resolvedStartDate,
            endDate = resolvedEndDate,
            spent = 0.0,
            remaining = amountLimit,
            percentage = 0.0,
            isExceeded = false,
            isSynced = false,
            isNewLocal = true,
            isDeleted = false
        )

        try {
            budgetDao.insertBudget(entity)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to save budget locally"))
        }

        triggerSync()

        return Result.Success(entity.toDomain())
    }

    override suspend fun updateBudget(
        id: String,
        amountLimit: Double,
        period: String,
        startDate: String?,
        endDate: String?
    ): Result<Budget, DataError> {
        val existing = try {
            budgetDao.getBudgetById(id)
        } catch (e: Exception) {
            null
        } ?: return Result.Error(DataError.Api("Budget not found"))

        val resolvedStartDate = startDate ?: existing.startDate
        val parsedStart = LocalDate.parse(resolvedStartDate)
        val resolvedEndDate = endDate ?: when (period.uppercase()) {
            "WEEKLY" -> parsedStart.plusWeeks(1).minusDays(1).toString()
            "YEARLY" -> parsedStart.withDayOfYear(parsedStart.lengthOfYear()).toString()
            else -> parsedStart.withDayOfMonth(parsedStart.lengthOfMonth()).toString() // MONTHLY
        }

        val updatedEntity = existing.copy(
            amountLimit = amountLimit,
            period = period,
            startDate = resolvedStartDate,
            endDate = resolvedEndDate,
            remaining = amountLimit - existing.spent,
            percentage = if (amountLimit > 0) (existing.spent / amountLimit) * 100.0 else 0.0,
            isExceeded = existing.spent > amountLimit,
            isSynced = false
        )

        try {
            budgetDao.insertBudget(updatedEntity)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to update budget locally"))
        }

        triggerSync()

        return Result.Success(updatedEntity.toDomain())
    }

    override suspend fun deleteBudget(id: String): Result<Unit, DataError> {
        try {
            budgetDao.markBudgetDeleted(id)
        } catch (e: Exception) {
            return Result.Error(DataError.Api("Failed to delete budget locally"))
        }

        triggerSync()

        return Result.Success(Unit)
    }

    override suspend fun syncBudgets(): Result<Unit, DataError> {
        val responseResult = safeCall<List<BudgetResponseDto>> {
            apiService.getBudgets()
        }

        return when (responseResult) {
            is Result.Success -> {
                try {
                    val networkEntities = responseResult.data.map { dto ->
                        BudgetEntity(
                            id = dto.id,
                            categoryId = dto.categoryId,
                            categoryName = dto.categoryName,
                            categoryIcon = dto.categoryIcon,
                            categoryColor = dto.categoryColor,
                            amountLimit = dto.amountLimit,
                            period = dto.period,
                            startDate = dto.startDate,
                            endDate = dto.endDate,
                            spent = dto.spent,
                            remaining = dto.remaining,
                            percentage = dto.percentage,
                            isExceeded = dto.isExceeded,
                            isSynced = true,
                            isNewLocal = false,
                            isDeleted = false
                        )
                    }
                    budgetDao.replaceBudgets(networkEntities)
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Result.Error(DataError.Api("Failed to save synced budgets locally"))
                }
            }
            is Result.Error -> {
                Result.Error(responseResult.error)
            }
        }
    }

    private fun triggerSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncBudgetsWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        } catch (e: Exception) {
            // Ignore WorkManager setup failures
        }
    }
}

fun BudgetEntity.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColor = categoryColor,
    amountLimit = amountLimit,
    period = period,
    startDate = startDate,
    endDate = endDate,
    spent = spent,
    remaining = remaining,
    percentage = percentage,
    isExceeded = isExceeded
)
