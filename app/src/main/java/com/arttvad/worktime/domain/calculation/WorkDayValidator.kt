package com.arttvad.worktime.domain.calculation

import com.arttvad.worktime.domain.model.WorkDayType

object WorkDayValidator {
    const val MAX_DAY_MINUTES = 24 * 60

    fun validate(workedMinutes: Int, overtimeMinutes: Int): WorkDayValidationError? =
        validate(WorkDayType.WORK, workedMinutes, overtimeMinutes)

    fun validate(
        type: WorkDayType,
        workedMinutes: Int,
        overtimeMinutes: Int,
    ): WorkDayValidationError? = when {
        type != WorkDayType.WORK && (workedMinutes != 0 || overtimeMinutes != 0) ->
            WorkDayValidationError.NON_WORK_HAS_TIME
        type != WorkDayType.WORK -> null
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
    NON_WORK_HAS_TIME,
}
