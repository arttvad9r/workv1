package com.arttvad.worktime.domain.exporting

import com.arttvad.worktime.domain.calculation.DetailedMonthStatistics
import com.arttvad.worktime.domain.calculation.DetailedMonthStatisticsCalculator
import com.arttvad.worktime.domain.calculation.EarningsCalculator
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import java.util.Locale

data class MonthReportDay(
    val date: LocalDate,
    val type: WorkDayType,
    val workedMinutes: Int,
    val overtimeMinutes: Int,
    val effectiveHourlyRateMinor: Long?,
    val earningsMinor: Long?,
    val note: String,
)

data class MonthReportData(
    val month: YearMonth,
    val currencyCode: String,
    val statistics: DetailedMonthStatistics,
    val days: List<MonthReportDay>,
)

object MonthReportDataBuilder {
    fun build(
        month: YearMonth,
        days: List<WorkDay>,
        hourlyRateMinor: Long?,
        currencyCode: String,
    ): MonthReportData {
        val normalizedCurrency = Currency
            .getInstance(currencyCode.uppercase(Locale.ROOT))
            .currencyCode
        val monthDays = days
            .filter { YearMonth.from(it.date) == month }
            .sortedBy(WorkDay::date)
        val reportDays = monthDays.map { day ->
            val effectiveRate = if (day.type == WorkDayType.WORK && day.workedMinutes > 0) {
                day.hourlyRateOverrideMinor ?: hourlyRateMinor
            } else {
                null
            }
            MonthReportDay(
                date = day.date,
                type = day.type,
                workedMinutes = day.workedMinutes,
                overtimeMinutes = day.overtimeMinutes,
                effectiveHourlyRateMinor = effectiveRate,
                earningsMinor = effectiveRate?.let { rate ->
                    EarningsCalculator.calculateMinor(day.workedMinutes, rate)
                },
                note = day.note,
            )
        }

        return MonthReportData(
            month = month,
            currencyCode = normalizedCurrency,
            statistics = DetailedMonthStatisticsCalculator.calculate(monthDays, hourlyRateMinor),
            days = reportDays,
        )
    }
}
