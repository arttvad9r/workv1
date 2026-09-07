package com.arttvad.worktime.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.workTimeDataStore by preferencesDataStore(name = "worktime_preferences")

data class WorkPreferences(
    val hourlyRateMinor: Long? = null,
    val currencyCode: String = defaultCurrencyCode(),
)

class WorkPreferencesRepository(
    private val context: Context,
) {
    private object Keys {
        val hourlyRateMinor = longPreferencesKey("hourly_rate_minor")
        val currencyCode = stringPreferencesKey("currency_code")
    }

    val preferences: Flow<WorkPreferences> = context.workTimeDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            WorkPreferences(
                hourlyRateMinor = values[Keys.hourlyRateMinor],
                currencyCode = values[Keys.currencyCode] ?: defaultCurrencyCode(),
            )
        }

    suspend fun updatePayment(hourlyRateMinor: Long?, currencyCode: String) {
        context.workTimeDataStore.edit { values ->
            if (hourlyRateMinor == null) {
                values.remove(Keys.hourlyRateMinor)
            } else {
                values[Keys.hourlyRateMinor] = hourlyRateMinor
            }
            values[Keys.currencyCode] = currencyCode.uppercase(Locale.ROOT)
        }
    }
}

private fun defaultCurrencyCode(): String = runCatching {
    Currency.getInstance(Locale.getDefault()).currencyCode
}.getOrDefault("USD")
