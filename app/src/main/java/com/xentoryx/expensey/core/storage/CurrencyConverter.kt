package com.xentoryx.expensey.core.storage

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

@Serializable
data class ExchangeRateResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)

class CurrencyConverter(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var cachedRates: Map<String, Double> = defaultRates()

    init {
        loadCachedRates()
    }

    private fun loadCachedRates() {
        scope.launch {
            try {
                val json = tokenManager.getExchangeRates()
                if (json != null) {
                    cachedRates = Json.decodeFromString(json)
                }
                syncRatesIfNeeded()
            } catch (e: Exception) {
                Log.e("CurrencyConverter", "Failed to load cached rates: ${e.message}")
            }
        }
    }

    suspend fun syncRatesIfNeeded() {
        val lastFetched = tokenManager.getExchangeRatesTimestamp()
        val now = System.currentTimeMillis()
        if (now - lastFetched > 12 * 60 * 60 * 1000 || cachedRates.size <= 2) {
            fetchRates()
        }
    }

    private suspend fun fetchRates() {
        try {
            val response: ExchangeRateResponse = httpClient.get("https://open.er-api.com/v6/latest/USD").body()
            if (response.result.lowercase(Locale.US) == "success") {
                cachedRates = response.rates
                tokenManager.saveExchangeRates(Json.encodeToString(response.rates), System.currentTimeMillis())
                Log.d("CurrencyConverter", "Successfully synced exchange rates from API")
            }
        } catch (e: Exception) {
            Log.e("CurrencyConverter", "Failed to sync exchange rates from API: ${e.message}")
        }
    }

    fun convert(amount: Double, fromCode: String, toCode: String): Double {
        val rates = cachedRates
        val fromRate = rates[fromCode.uppercase(Locale.US)] ?: defaultRates()[fromCode.uppercase(Locale.US)] ?: 1.0
        val toRate = rates[toCode.uppercase(Locale.US)] ?: defaultRates()[toCode.uppercase(Locale.US)] ?: 1.0

        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }

    companion object {
        fun defaultRates(): Map<String, Double> = mapOf(
            "USD" to 1.0,
            "BDT" to 117.50,
            "EUR" to 0.93,
            "GBP" to 0.79,
            "INR" to 83.50,
            "CAD" to 1.37,
            "AUD" to 1.51,
            "JPY" to 157.0,
            "SAR" to 3.75,
            "AED" to 3.67
        )
    }
}
