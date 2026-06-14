package com.xentoryx.expensey.feature.transaction.domain.model

import java.time.LocalDate

data class TransactionFilter(
    val searchQuery: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val selectedAccounts: Set<String> = emptySet(),
    val selectedCategories: Set<String> = emptySet(),
    val selectedTypes: Set<String> = emptySet(),
    val recurrenceFilter: String = "ALL" // "ALL" | "ONETIME" | "RECURRING"
)
