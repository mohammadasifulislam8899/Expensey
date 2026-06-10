package com.xentoryx.expensey.feature.pdf_export.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xentoryx.expensey.core.domain.util.Result
import com.xentoryx.expensey.feature.pdf_export.domain.usecase.ExportPdfReportUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PdfExportViewModel(
    private val exportPdfReportUseCase: ExportPdfReportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PdfExportState())
    val state = _state.asStateFlow()

    private var loadingMessageJob: Job? = null

    private val loadingMessages = listOf(
        "Preparing financial data...",
        "Gathering transaction records...",
        "Analyzing account summaries...",
        "Applying Crush theme styles...",
        "Formatting charts and tables...",
        "Generating PDF document..."
    )

    fun onDateRangeSelected(start: Long?, end: Long?) {
        _state.update {
            it.copy(
                startDateMillis = start,
                endDateMillis = end,
                pdfBytes = null,
                error = null
            )
        }
    }

    fun clearPdf() {
        _state.update { it.copy(pdfBytes = null, error = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun generateReport() {
        val startMillis = _state.value.startDateMillis
        val endMillis = _state.value.endDateMillis

        if (startMillis == null || endMillis == null) {
            _state.update { it.copy(error = "Please select a valid date range") }
            return
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startLocalDate = Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val endLocalDate = Instant.ofEpochMilli(endMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val fromStr = startLocalDate.format(formatter)
        val toStr = endLocalDate.format(formatter)

        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                pdfBytes = null,
                loadingMessage = loadingMessages.first()
            )
        }

        // Cycle through loading messages
        loadingMessageJob?.cancel()
        loadingMessageJob = viewModelScope.launch {
            var index = 0
            while (true) {
                delay(2000)
                index = (index + 1) % loadingMessages.size
                _state.update { it.copy(loadingMessage = loadingMessages[index]) }
            }
        }

        viewModelScope.launch {
            val result = exportPdfReportUseCase(fromStr, toStr)
            loadingMessageJob?.cancel()
            
            _state.update {
                when (result) {
                    is Result.Success -> {
                        it.copy(
                            isLoading = false,
                            pdfBytes = result.data,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        it.copy(
                            isLoading = false,
                            pdfBytes = null,
                            error = "Failed to export PDF: ${result.error}"
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadingMessageJob?.cancel()
    }
}
