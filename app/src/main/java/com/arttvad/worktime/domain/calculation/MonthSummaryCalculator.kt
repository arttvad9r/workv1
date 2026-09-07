package com.arttvad.worktime.domain.calculation

import com.arttvad.worktime.domain.model.MonthSummary
import com.arttvad.worktime.domain.model.WorkDay

object MonthSummaryCalculator {
    fun calculate(days: List<WorkDay>, hourlyRateMinor: Long?): MonthSummary {
        val worked = days.sumOf { it.workedMinutes }
        val overtime = days.sumOf { it.overtimeMinutes }
        val earnings = hourlyRateMinor?.let { rate ->
            days.fold(0L) { total, day ->
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
