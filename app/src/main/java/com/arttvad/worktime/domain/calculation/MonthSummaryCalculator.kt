package com.arttvad.worktime.domain.calculation

import com.arttvad.worktime.domain.model.MonthSummary
import com.arttvad.worktime.domain.model.WorkDay

object MonthSummaryCalculator {
    fun calculate(days: List<WorkDay>, hourlyRateMinor: Long?): MonthSummary {
        val worked = days.sumOf { it.workedMinutes }
        val overtime = days.sumOf { it.overtimeMinutes }
        val payableDays = days.filter { it.workedMinutes > 0 }
        val earnings = when {
            payableDays.isEmpty() -> hourlyRateMinor?.let { 0L }
            payableDays.any { day -> day.hourlyRateOverrideMinor == null && hourlyRateMinor == null } -> null
            else -> payableDays.fold(0L) { total, day ->
                val rate = day.hourlyRateOverrideMinor ?: requireNotNull(hourlyRateMinor)
                Math.addExact(total, EarningsCalculator.calculateMinor(day.workedMinutes, rate))
            }
        }

        return MonthSummary(
            workedMinutes = worked,
            overtimeMinutes = overtime,
            earningsMinor = earnings,
        )
    }
}
