package com.arttvad.worktime.domain.calculation

import com.arttvad.worktime.domain.model.WorkDay
import java.time.Year
import java.time.YearMonth

data class YearMonthStatistics(
    val month: YearMonth,
    val workedMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val workDays: Int = 0,
    val earningsMinor: Long? = null,
)

data class DetailedYearStatistics(
    val year: Year,
    val totals: DetailedMonthStatistics = DetailedMonthStatistics(),
    val months: List<YearMonthStatistics> = emptyList(),
)

object DetailedYearStatisticsCalculator {
    fun calculate(
        year: Year,
        days: List<WorkDay>,
        hourlyRateMinor: Long?,
    ): DetailedYearStatistics {
        val yearDays = days.filter { it.date.year == year.value }
        val totals = DetailedMonthStatisticsCalculator.calculate(yearDays, hourlyRateMinor)
        val months = (1..12).map { monthValue ->
            val month = YearMonth.of(year.value, monthValue)
            val monthDays = yearDays.filter { YearMonth.from(it.date) == month }
            val statistics = DetailedMonthStatisticsCalculator.calculate(monthDays, hourlyRateMinor)
            YearMonthStatistics(
                month = month,
                workedMinutes = statistics.workedMinutes,
                overtimeMinutes = statistics.overtimeMinutes,
                workDays = statistics.workDays,
                earningsMinor = statistics.earningsMinor,
            )
        }
        return DetailedYearStatistics(
            year = year,
            totals = totals,
            months = months,
        )
    }
}
