package com.arttvad.worktime.widget

import com.arttvad.worktime.domain.calculation.MoneyFormatter
import com.arttvad.worktime.domain.calculation.MonthSummaryCalculator
import com.arttvad.worktime.domain.model.WorkDay
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WorkTimeWidgetSnapshot(
    val monthTitle: String,
    val worked: String,
    val earned: String,
    val overtime: String,
)

object WorkTimeWidgetSnapshotFactory {
    fun create(
        month: YearMonth,
        days: List<WorkDay>,
        hourlyRateMinor: Long?,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): WorkTimeWidgetSnapshot {
        val monthDays = days.filter { day -> YearMonth.from(day.date) == month }
        val summary = MonthSummaryCalculator.calculate(monthDays, hourlyRateMinor)
        val rawTitle = month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        return WorkTimeWidgetSnapshot(
            monthTitle = rawTitle.replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase(locale) else character.toString()
            },
            worked = formatDuration(summary.workedMinutes),
            earned = runCatching {
                MoneyFormatter.formatCurrency(summary.earningsMinor, currencyCode, locale)
            }.getOrDefault("—"),
            overtime = formatDuration(summary.overtimeMinutes),
        )
    }

    private fun formatDuration(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            minutes == 0 -> "$hours ч"
            hours == 0 -> "$minutes мин"
            else -> "$hours ч $minutes мин"
        }
    }
}
