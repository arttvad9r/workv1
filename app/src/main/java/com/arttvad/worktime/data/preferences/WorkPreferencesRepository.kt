package com.arttvad.worktime.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.time.LocalDate
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.workTimeDataStore by preferencesDataStore(name = "worktime_preferences")

const val DEFAULT_REMINDER_HOUR = 20
const val DEFAULT_REMINDER_MINUTE = 0

data class ActiveShiftSession(
    val date: LocalDate,
    val startedAtEpochMillis: Long,
)

data class WorkPreferences(
    val hourlyRateMinor: Long? = null,
    val currencyCode: String = defaultCurrencyCode(),
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    val reminderMinute: Int = DEFAULT_REMINDER_MINUTE,
    val activeShift: ActiveShiftSession? = null,
)

class WorkPreferencesRepository(
    private val context: Context,
) {
    private object Keys {
        val hourlyRateMinor = longPreferencesKey("hourly_rate_minor")
        val currencyCode = stringPreferencesKey("currency_code")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val activeShiftDate = stringPreferencesKey("active_shift_date")
        val activeShiftStartedAt = longPreferencesKey("active_shift_started_at")
    }

    val preferences: Flow<WorkPreferences> = context.workTimeDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            val reminderHour = values[Keys.reminderHour]
                ?.takeIf { value -> value in 0..23 }
                ?: DEFAULT_REMINDER_HOUR
            val reminderMinute = values[Keys.reminderMinute]
                ?.takeIf { value -> value in 0..59 }
                ?: DEFAULT_REMINDER_MINUTE
            val activeShiftDate = values[Keys.activeShiftDate]
                ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
            val activeShiftStartedAt = values[Keys.activeShiftStartedAt]
                ?.takeIf { value -> value >= 0L }
            val activeShift = if (activeShiftDate != null && activeShiftStartedAt != null) {
                ActiveShiftSession(
                    date = activeShiftDate,
                    startedAtEpochMillis = activeShiftStartedAt,
                )
            } else {
                null
            }

            WorkPreferences(
                hourlyRateMinor = values[Keys.hourlyRateMinor],
                currencyCode = values[Keys.currencyCode] ?: defaultCurrencyCode(),
                reminderEnabled = values[Keys.reminderEnabled] ?: false,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                activeShift = activeShift,
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

    suspend fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        require(hour in 0..23) { "Reminder hour must be between 0 and 23" }
        require(minute in 0..59) { "Reminder minute must be between 0 and 59" }
        context.workTimeDataStore.edit { values ->
            values[Keys.reminderEnabled] = enabled
            values[Keys.reminderHour] = hour
            values[Keys.reminderMinute] = minute
        }
    }

    suspend fun startShift(date: LocalDate, startedAtEpochMillis: Long = System.currentTimeMillis()) {
        require(startedAtEpochMillis >= 0L) { "Shift start timestamp must be non-negative" }
        context.workTimeDataStore.edit { values ->
            val existingDate = values[Keys.activeShiftDate]
                ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
            val existingStartedAt = values[Keys.activeShiftStartedAt]
                ?.takeIf { value -> value >= 0L }
            check(existingDate == null || existingStartedAt == null) {
                "A shift session is already active"
            }
            values[Keys.activeShiftDate] = date.toString()
            values[Keys.activeShiftStartedAt] = startedAtEpochMillis
        }
    }

    suspend fun clearActiveShift() {
        context.workTimeDataStore.edit { values ->
            values.remove(Keys.activeShiftDate)
            values.remove(Keys.activeShiftStartedAt)
        }
    }
}

private fun defaultCurrencyCode(): String = runCatching {
    Currency.getInstance(Locale.getDefault()).currencyCode
}.getOrDefault("USD")
