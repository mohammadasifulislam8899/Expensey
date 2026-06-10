package com.xentoryx.expensey.feature.transaction.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.data.database.dao.AccountDao
import com.xentoryx.expensey.core.data.database.dao.CategoryDao
import com.xentoryx.expensey.core.data.database.dao.AttachmentDao
import com.xentoryx.expensey.core.data.database.entity.AttachmentEntity
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.dashboard.data.mapper.toDomain
import com.xentoryx.expensey.feature.dashboard.domain.model.CategoryBreakdown
import com.xentoryx.expensey.feature.transaction.domain.usecase.CreateTransactionUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.UpdateTransactionUseCase
import com.xentoryx.expensey.feature.transaction.domain.usecase.DeleteTransactionUseCase
import com.xentoryx.expensey.feature.transaction.domain.repository.TransactionRepository
import com.xentoryx.expensey.core.domain.util.DataError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Instant
import java.io.File
import java.util.UUID
import android.content.Context
import android.net.Uri

class AddTransactionViewModel(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val attachmentDao: AttachmentDao
) : ViewModel() {

    private val _state = MutableStateFlow(
        AddTransactionState(
            dateString = LocalDate.now().toString()
        )
    )
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    init {
        loadAccountsAndCategories()
    }

    private fun loadAccountsAndCategories() {
        viewModelScope.launch {
            try {
                val dbAccounts = accountDao.getAccounts().map { it.toDomain() }
                val dbCategories = categoryDao.getCategories().map { entity ->
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
                _state.update {
                    it.copy(
                        accounts = dbAccounts,
                        categories = dbCategories,
                        selectedAccountId = it.selectedAccountId ?: dbAccounts.firstOrNull()?.accountId,
                        selectedCategoryId = it.selectedCategoryId ?: dbCategories.firstOrNull()?.categoryId
                    )
                }
            } catch (e: Exception) {
                // Ignore DB read errors
            }
        }
    }

    fun setEditTransaction(transactionId: String?) {
        if (transactionId == null) {
            _state.update { AddTransactionState(dateString = LocalDate.now().toString()) }
            loadAccountsAndCategories()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val tx = transactionRepository.getTransactionById(transactionId)
            if (tx != null) {
                _state.update {
                    it.copy(
                        transactionId = tx.id,
                        amount = tx.amount.toString(),
                        note = tx.note ?: "",
                        type = tx.type,
                        selectedAccountId = tx.accountId,
                        selectedCategoryId = tx.categoryId,
                        transferToAccountId = tx.transferToAccountId,
                        dateString = tx.transactionDate,
                        isLoading = false
                    )
                }
                loadAccountsAndCategories()
                loadAttachments(tx.id)
            } else {
                _state.update { it.copy(isLoading = false, errorMessage = "Failed to load transaction") }
            }
        }
    }

    private fun loadAttachments(transactionId: String) {
        viewModelScope.launch {
            try {
                val list = attachmentDao.getAttachmentsForTransaction(transactionId)
                _state.update { it.copy(savedAttachments = list) }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun addAttachment(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
                
                var fileName = "attachment_${System.currentTimeMillis()}.$extension"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
                val destFile = File(attachmentsDir, "file_${UUID.randomUUID()}_$fileName")
                
                contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val txId = _state.value.transactionId
                if (txId != null) {
                    val attachment = AttachmentEntity(
                        id = UUID.randomUUID().toString(),
                        transactionId = txId,
                        localFilePath = destFile.absolutePath,
                        fileName = fileName,
                        fileType = mimeType,
                        createdAt = Instant.now().toString()
                    )
                    attachmentDao.insertAttachment(attachment)
                    loadAttachments(txId)
                } else {
                    val temp = TempAttachment(
                        localFilePath = destFile.absolutePath,
                        fileName = fileName,
                        fileType = mimeType
                    )
                    _state.update {
                        it.copy(tempAttachments = it.tempAttachments + temp)
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to add attachment: ${e.message}") }
            }
        }
    }

    fun deleteAttachment(attachmentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val attachment = attachmentDao.getAttachmentById(attachmentId)
                if (attachment != null) {
                    val file = File(attachment.localFilePath)
                    if (file.exists()) {
                        file.delete()
                    }
                    attachmentDao.deleteAttachmentById(attachmentId)
                    _state.value.transactionId?.let { loadAttachments(it) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Failed to delete attachment: ${e.message}") }
            }
        }
    }

    fun deleteTempAttachment(temp: TempAttachment) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(temp.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
                _state.update {
                    it.copy(tempAttachments = it.tempAttachments - temp)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun onAmountChange(amount: String) {
        _state.update { it.copy(amount = amount) }
    }

    fun onNoteChange(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun onTypeChange(type: String) {
        _state.update { it.copy(type = type) }
    }

    fun onAccountSelected(accountId: String) {
        _state.update { it.copy(selectedAccountId = accountId) }
    }

    fun onCategorySelected(categoryId: String) {
        _state.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onTransferToAccountSelected(accountId: String?) {
        _state.update { it.copy(transferToAccountId = accountId) }
    }

    fun onDateChange(dateString: String) {
        _state.update { it.copy(dateString = dateString) }
    }

    fun saveTransaction() {
        val currentState = _state.value
        val amountVal = currentState.amount.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0.0) {
            _state.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }
        if (currentState.selectedAccountId == null) {
            _state.update { it.copy(errorMessage = "Please select an account") }
            return
        }
        if (currentState.selectedCategoryId == null && currentState.type != "TRANSFER") {
            _state.update { it.copy(errorMessage = "Please select a category") }
            return
        }
        if (currentState.type == "TRANSFER" && currentState.transferToAccountId == null) {
            _state.update { it.copy(errorMessage = "Please select target account for transfer") }
            return
        }
        if (currentState.type == "TRANSFER" && currentState.selectedAccountId == currentState.transferToAccountId) {
            _state.update { it.copy(errorMessage = "Source and target accounts must be different") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (currentState.transactionId == null) {
                createTransactionUseCase(
                    accountId = currentState.selectedAccountId,
                    categoryId = if (currentState.type == "TRANSFER") "" else (currentState.selectedCategoryId ?: ""),
                    transferToAccountId = if (currentState.type == "TRANSFER") currentState.transferToAccountId else null,
                    amount = amountVal,
                    type = currentState.type,
                    note = currentState.note.ifBlank { null },
                    transactionDate = currentState.dateString
                )
            } else {
                updateTransactionUseCase(
                    id = currentState.transactionId,
                    accountId = currentState.selectedAccountId,
                    categoryId = if (currentState.type == "TRANSFER") "" else (currentState.selectedCategoryId ?: ""),
                    transferToAccountId = if (currentState.type == "TRANSFER") currentState.transferToAccountId else null,
                    amount = amountVal,
                    type = currentState.type,
                    note = currentState.note.ifBlank { null },
                    transactionDate = currentState.dateString
                )
            }

            when (result) {
                is Result.Success -> {
                    if (currentState.transactionId == null) {
                        val newTxId = result.data.id
                        currentState.tempAttachments.forEach { temp ->
                            val attachment = AttachmentEntity(
                                id = UUID.randomUUID().toString(),
                                transactionId = newTxId,
                                localFilePath = temp.localFilePath,
                                fileName = temp.fileName,
                                fileType = temp.fileType,
                                createdAt = Instant.now().toString()
                            )
                            attachmentDao.insertAttachment(attachment)
                        }
                    }
                    _state.update { it.copy(isLoading = false, isSuccess = true, tempAttachments = emptyList()) }
                }
                is Result.Error -> {
                    val msg = when (val err = result.error) {
                        is DataError.Api -> err.message
                        is DataError.Network -> "Network error"
                        is DataError.EmailNotVerified -> "Email not verified"
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            }
        }
    }

    fun deleteTransaction() {
        val txId = _state.value.transactionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Delete physical attachment files before dropping DB records
            try {
                val list = attachmentDao.getAttachmentsForTransaction(txId)
                list.forEach { attachment ->
                    val file = File(attachment.localFilePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // ignore file deletion failures
            }

            when (val result = deleteTransactionUseCase(txId)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false, isDeleteSuccess = true) }
                }
                is Result.Error -> {
                    val msg = when (val err = result.error) {
                        is DataError.Api -> err.message
                        is DataError.Network -> "Network error"
                        is DataError.EmailNotVerified -> "Email not verified"
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = msg) }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
