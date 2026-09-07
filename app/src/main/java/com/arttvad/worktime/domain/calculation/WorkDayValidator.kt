package com.arttvad.worktime.domain.calculation

object WorkDayValidator {
    const val MAX_DAY_MINUTES = 24 * 60

    fun validate(workedMinutes: Int, overtimeMinutes: Int): WorkDayValidationError? = when {
        workedMinutes !in 1..MAX_DAY_MINUTES -> WorkDayValidationError.INVALID_WORKED_TIME
        overtimeMinutes !in 0..MAX_DAY_MINUTES -> WorkDayValidationError.INVALID_OVERTIME
        overtimeMinutes > workedMinutes -> WorkDayValidationError.OVERTIME_EXCEEDS_WORKED
        else -> null
    }
}

enum class WorkDayValidationError {
    INVALID_WORKED_TIME,
    INVALID_OVERTIME,
    OVERTIME_EXCEEDS_WORKED,
}
