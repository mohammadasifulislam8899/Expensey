package com.xentoryx.expensey.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val DASHBOARD_CACHE_KEY = stringPreferencesKey("dashboard_cache")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("notification_enabled")
        private val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")
        private val USER_CURRENCY_KEY = stringPreferencesKey("user_currency")
        private val USER_COUNTRY_KEY = stringPreferencesKey("user_country")
        private val EXCHANGE_RATES_KEY = stringPreferencesKey("exchange_rates")
        private val EXCHANGE_RATES_TIMESTAMP_KEY = longPreferencesKey("exchange_rates_timestamp")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
        }
    }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.first()[ACCESS_TOKEN_KEY]

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.first()[REFRESH_TOKEN_KEY]

    suspend fun getUserId(): String? =
        context.dataStore.data.first()[USER_ID_KEY]

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[ACCESS_TOKEN_KEY].isNullOrEmpty()
    }.distinctUntilChanged()

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode
        }
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE_KEY] ?: "system"
    }

    suspend fun saveDashboardCache(json: String) {
        context.dataStore.edit { prefs ->
            prefs[DASHBOARD_CACHE_KEY] = json
        }
    }

    suspend fun getDashboardCache(): String? =
        context.dataStore.data.first()[DASHBOARD_CACHE_KEY]

    suspend fun saveBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED_KEY] ?: false
    }

    suspend fun saveNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATION_ENABLED_KEY] = enabled
        }
    }

    val notificationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATION_ENABLED_KEY] ?: false
    }

    suspend fun saveNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATION_HOUR_KEY] = hour
            prefs[NOTIFICATION_MINUTE_KEY] = minute
        }
    }

    val notificationHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATION_HOUR_KEY] ?: 21
    }

    val notificationMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATION_MINUTE_KEY] ?: 0
    }

    suspend fun saveUserCurrency(currencyCode: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_CURRENCY_KEY] = currencyCode
        }
    }

    val userCurrency: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_CURRENCY_KEY] ?: "BDT"
    }

    suspend fun saveUserCountry(countryCode: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_COUNTRY_KEY] = countryCode
        }
    }

    val userCountry: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_COUNTRY_KEY] ?: "BD"
    }

    suspend fun saveExchangeRates(json: String, timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[EXCHANGE_RATES_KEY] = json
            prefs[EXCHANGE_RATES_TIMESTAMP_KEY] = timestamp
        }
    }

    suspend fun getExchangeRates(): String? =
        context.dataStore.data.first()[EXCHANGE_RATES_KEY]

    suspend fun getExchangeRatesTimestamp(): Long =
        context.dataStore.data.first()[EXCHANGE_RATES_TIMESTAMP_KEY] ?: 0L

    val exchangeRatesTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[EXCHANGE_RATES_TIMESTAMP_KEY] ?: 0L
    }.distinctUntilChanged()

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}