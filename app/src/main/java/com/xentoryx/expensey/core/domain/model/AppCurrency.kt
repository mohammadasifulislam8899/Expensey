package com.xentoryx.expensey.core.domain.model

enum class AppCurrency(val code: String, val displayName: String, val symbol: String) {
    BDT("BDT", "Bangladeshi Taka (৳)", "৳"),
    USD("USD", "US Dollar ($)", "$"),
    EUR("EUR", "Euro (€)", "€"),
    GBP("GBP", "British Pound (£)", "£"),
    INR("INR", "Indian Rupee (₹)", "₹"),
    CAD("CAD", "Canadian Dollar (C$)", "C$"),
    AUD("AUD", "Australian Dollar (A$)", "A$"),
    JPY("JPY", "Japanese Yen (¥)", "¥"),
    SAR("SAR", "Saudi Riyal (SR)", "SR"),
    AED("AED", "UAE Dirham (DH)", "DH");

    companion object {
        fun fromCode(code: String): AppCurrency {
            return values().find { it.code.uppercase() == code.uppercase() } ?: BDT
        }
    }
}
