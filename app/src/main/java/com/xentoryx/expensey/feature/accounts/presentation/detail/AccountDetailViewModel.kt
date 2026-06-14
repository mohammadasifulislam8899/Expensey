package com.xentoryx.expensey.feature.accounts.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.dashboard.domain.model.Transaction
import com.xentoryx.expensey.feature.transaction.domain.usecase.GetTransactionsUseCase
import com.xentoryx.expensey.feature.accounts.domain.usecase.DeleteAccountUseCase
import com.xentoryx.expensey.feature.transaction.presentation.list.TransactionUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountDetailViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _accountId = MutableStateFlow<String?>(null)

    val transactions: StateFlow<List<TransactionUiModel>> = combine(
        getTransactionsUseCase.getLocalTransactions(),
        accountDao.getAccountsFlow(),
        categoryDao.getCategoriesFlow(),
        _accountId
    ) { allTransactions, dbAccounts, dbCategories, accountId ->
        if (accountId == null) {
            emptyList()
        } else {
            val accounts = dbAccounts.map { it.toDomain() }
            val categories = dbCategories.map { entity ->
                CategoryBreakdown(
                    categoryId = entity.id,
                    categoryName = entity.name,
                    categoryIcon = entity.icon,
                    categoryColor = entity.color,
                    type = entity.type,
                    total = 0.0,
                    percentage = 0.0
                )
            }
            allTransactions
                .filter { it.accountId == accountId }
                .map { txn ->
                    val accountName = accounts.find { it.accountId == txn.accountId }?.accountName ?: "Unknown"
                    val category = categories.find { it.categoryId == txn.categoryId }
                    TransactionUiModel(
                        id = txn.id,
                        accountId = txn.accountId,
                        accountName = accountName,
                        categoryId = txn.categoryId,
                        categoryName = category?.categoryName ?: "Unknown",
                        categoryIcon = category?.categoryIcon,
                        categoryColor = category?.categoryColor,
                        transferToAccountId = txn.transferToAccountId,
                        amount = txn.amount,
                        type = txn.type,
                        note = txn.note,
                        date = txn.transactionDate,
                        createdAt = txn.createdAt,
                        isRecurring = false,
                        isActive = true,
                        syncStatus = txn.syncStatus
                    )
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setAccountId(accountId: String) {
        _accountId.value = accountId
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val id = _accountId.value ?: return
        viewModelScope.launch {
            when (val result = deleteAccountUseCase(id)) {
                is com.xentoryx.expensey.core.domain.util.Result.Success -> onSuccess()
                is com.xentoryx.expensey.core.domain.util.Result.Error -> {
                    val msg = when (val err = result.error) {
                        is com.xentoryx.expensey.core.domain.util.DataError.Api -> err.message
                        is com.xentoryx.expensey.core.domain.util.DataError.Network -> "Network error"
                        is com.xentoryx.expensey.core.domain.util.DataError.EmailNotVerified -> "Email not verified"
                    }
                    onError(msg)
                }
            }
        }
    }
}
