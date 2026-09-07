package com.arttvad.worktime.domain.model

data class MonthSummary(
    val workedMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val earningsMinor: Long? = null,
)
