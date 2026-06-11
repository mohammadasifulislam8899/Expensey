package com.xentoryx.expensey.feature.pdf_export.presentation

data class PdfExportState(
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val isLoading: Boolean = false,
    val pdfBytes: ByteArray? = null,
    val csvBytes: ByteArray? = null,
    val exportFormat: String = "PDF", // "PDF" or "CSV"
    val error: String? = null,
    val loadingMessage: String = "Preparing data..."
)
