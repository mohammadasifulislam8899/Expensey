package com.xentoryx.expensey.feature.transaction.presentation.add

import com.xentoryx.expensey.core.data.database.entity.AttachmentEntity
import com.xentoryx.expensey.feature.dashboard.domain.model.AccountSummary
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown

data class TempAttachment(
    val localFilePath: String,
    val fileName: String,
    val fileType: String
)

data class AddTransactionState(
    val transactionId: String? = null,
    val accounts: List<AccountSummary> = emptyList(),
    val categories: List<CategoryBreakdown> = emptyList(),
    val amount: String = "",
    val note: String = "",
    val type: String = "EXPENSE", // "INCOME" | "EXPENSE" | "TRANSFER"
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val transferToAccountId: String? = null,
    val dateString: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isDeleteSuccess: Boolean = false,
    val errorMessage: String? = null,
    val savedAttachments: List<AttachmentEntity> = emptyList(),
    val tempAttachments: List<TempAttachment> = emptyList(),
    
    // Recurrence Fields
    val isRecurring: Boolean = false,
    val frequency: String = "MONTHLY", // "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY"
    val startDate: String = "",
    val endDate: String? = null,
    val isActive: Boolean = true,
    val isRecurringEdit: Boolean = false,
    
    // User profile settings cache
    val userCurrencyCode: String = "BDT",

    // Multi-currency transfer support
    val isInputInTargetCurrency: Boolean = false,
    val convertedPreviewAmount: Double = 0.0
)
