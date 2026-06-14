package com.xentoryx.expensey.feature.accounts.presentation.util

data class FinancialInstitutionRecommendation(
    val name: String,
    val isMfs: Boolean
)

object PredefinedFinancialInstitutions {
    private val recommendations = mapOf(
        "BD" to listOf(
            FinancialInstitutionRecommendation("bKash", isMfs = true),
            FinancialInstitutionRecommendation("Nagad", isMfs = true),
            FinancialInstitutionRecommendation("Rocket", isMfs = true),
            FinancialInstitutionRecommendation("Upay", isMfs = true),
            FinancialInstitutionRecommendation("CellFin", isMfs = true),
            FinancialInstitutionRecommendation("Dutch-Bangla Bank", isMfs = false),
            FinancialInstitutionRecommendation("BRAC Bank", isMfs = false),
            FinancialInstitutionRecommendation("City Bank", isMfs = false),
            FinancialInstitutionRecommendation("Sonali Bank", isMfs = false),
            FinancialInstitutionRecommendation("Prime Bank", isMfs = false)
        ),
        "US" to listOf(
            FinancialInstitutionRecommendation("PayPal", isMfs = true),
            FinancialInstitutionRecommendation("Venmo", isMfs = true),
            FinancialInstitutionRecommendation("Cash App", isMfs = true),
            FinancialInstitutionRecommendation("Chase", isMfs = false),
            FinancialInstitutionRecommendation("Bank of America", isMfs = false),
            FinancialInstitutionRecommendation("Wells Fargo", isMfs = false),
            FinancialInstitutionRecommendation("Citibank", isMfs = false),
            FinancialInstitutionRecommendation("Capital One", isMfs = false)
        ),
        "IN" to listOf(
            FinancialInstitutionRecommendation("Paytm", isMfs = true),
            FinancialInstitutionRecommendation("PhonePe", isMfs = true),
            FinancialInstitutionRecommendation("Google Pay", isMfs = true),
            FinancialInstitutionRecommendation("SBI", isMfs = false),
            FinancialInstitutionRecommendation("HDFC Bank", isMfs = false),
            FinancialInstitutionRecommendation("ICICI Bank", isMfs = false),
            FinancialInstitutionRecommendation("Axis Bank", isMfs = false)
        ),
        "SA" to listOf(
            FinancialInstitutionRecommendation("STC Pay", isMfs = true),
            FinancialInstitutionRecommendation("Urpay", isMfs = true),
            FinancialInstitutionRecommendation("Mobily Pay", isMfs = true),
            FinancialInstitutionRecommendation("Al Rajhi Bank", isMfs = false),
            FinancialInstitutionRecommendation("SNB", isMfs = false),
            FinancialInstitutionRecommendation("Riyad Bank", isMfs = false)
        ),
        "AE" to listOf(
            FinancialInstitutionRecommendation("e& money", isMfs = true),
            FinancialInstitutionRecommendation("Payit", isMfs = true),
            FinancialInstitutionRecommendation("Careem Pay", isMfs = true),
            FinancialInstitutionRecommendation("Emirates NBD", isMfs = false),
            FinancialInstitutionRecommendation("ADCB", isMfs = false),
            FinancialInstitutionRecommendation("FAB", isMfs = false),
            FinancialInstitutionRecommendation("Mashreq", isMfs = false)
        )
    )

    fun getRecommendations(countryCode: String, getMfs: Boolean): List<String> {
        val countryRecs = recommendations[countryCode.uppercase()] ?: recommendations["BD"]!!
        return countryRecs.filter { it.isMfs == getMfs }.map { it.name }
    }
}
