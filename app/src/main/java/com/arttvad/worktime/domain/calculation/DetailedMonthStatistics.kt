package com.arttvad.worktime.domain.calculation

import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType

data class DetailedMonthStatistics(
    val workDays: Int = 0,
    val daysOff: Int = 0,
    val vacationDays: Int = 0,
    val sickDays: Int = 0,
    val workedMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val averageWorkedMinutes: Int = 0,
    val longestWorkedMinutes: Int = 0,
    val earningsMinor: Long? = null,
)

object DetailedMonthStatisticsCalculator {
    fun calculate(days: List<WorkDay>, hourlyRateMinor: Long?): DetailedMonthStatistics {
        val summary = MonthSummaryCalculator.calculate(days, hourlyRateMinor)
        val workedDays = days.filter { it.type == WorkDayType.WORK && it.workedMinutes > 0 }
        val average = if (workedDays.isEmpty()) {
            0
        } else {
            (workedDays.sumOf(WorkDay::workedMinutes) + workedDays.size / 2) / workedDays.size
        }

        return DetailedMonthStatistics(
            workDays = workedDays.size,
            daysOff = days.count { it.type == WorkDayType.DAY_OFF },
            vacationDays = days.count { it.type == WorkDayType.VACATION },
            sickDays = days.count { it.type == WorkDayType.SICK },
            workedMinutes = summary.workedMinutes,
            overtimeMinutes = summary.overtimeMinutes,
            averageWorkedMinutes = average,
            longestWorkedMinutes = workedDays.maxOfOrNull(WorkDay::workedMinutes) ?: 0,
            earningsMinor = summary.earningsMinor,
        )
    }
}
