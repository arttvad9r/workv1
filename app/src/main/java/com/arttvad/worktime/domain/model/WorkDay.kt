package com.arttvad.worktime.domain.model

import java.time.LocalDate

data class WorkDay(
    val date: LocalDate,
    val workedMinutes: Int,
    val overtimeMinutes: Int,
    val note: String,
    val updatedAtEpochMillis: Long,
    val hourlyRateOverrideMinor: Long? = null,
    val type: WorkDayType = WorkDayType.WORK,
)

enum class WorkDayType {
    WORK,
    DAY_OFF,
    VACATION,
    SICK,
}
