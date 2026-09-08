package com.arttvad.worktime.domain.exporting

import com.arttvad.worktime.domain.calculation.EarningsCalculator
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Currency
import java.util.Locale

object MonthCsvExporter {
    private val header = listOf(
        "record_type",
        "period",
        "currency",
        "date",
        "day_type",
        "worked_minutes",
        "overtime_minutes",
        "effective_hourly_rate",
        "earnings",
        "note",
    )

    fun export(
        month: YearMonth,
        days: List<WorkDay>,
        hourlyRateMinor: Long?,
        currencyCode: String,
    ): String {
        val currency = Currency.getInstance(currencyCode.uppercase(Locale.ROOT))
        val rows = buildList {
            add(header)
            add(
                listOf(
                    "metadata",
                    month.toString(),
                    currency.currencyCode,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                ),
            )

            days.asSequence()
                .filter { YearMonth.from(it.date) == month }
                .sortedBy(WorkDay::date)
                .forEach { day ->
                    val effectiveRate = if (day.type == WorkDayType.WORK && day.workedMinutes > 0) {
                        day.hourlyRateOverrideMinor ?: hourlyRateMinor
                    } else {
                        null
                    }
                    val earningsMinor = effectiveRate?.let { rate ->
                        EarningsCalculator.calculateMinor(day.workedMinutes, rate)
                    }

                    add(
                        listOf(
                            "day",
                            month.toString(),
                            currency.currencyCode,
                            day.date.toString(),
                            day.type.name,
                            day.workedMinutes.toString(),
                            day.overtimeMinutes.toString(),
                            effectiveRate?.let { formatMinor(it, currency) }.orEmpty(),
                            earningsMinor?.let { formatMinor(it, currency) }.orEmpty(),
                            day.note,
                        ),
                    )
                }
        }

        return rows.joinToString(separator = "\r\n", postfix = "\r\n") { row ->
            row.joinToString(separator = ",", transform = ::escapeCell)
        }
    }

    private fun formatMinor(amountMinor: Long, currency: Currency): String {
        val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
        return BigDecimal.valueOf(amountMinor, fractionDigits)
            .setScale(fractionDigits)
            .toPlainString()
    }

    private fun escapeCell(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\r' || it == '\n' }) return value
        return buildString(value.length + 2) {
            append('"')
            value.forEach { character ->
                if (character == '"') append("\"\"") else append(character)
            }
            append('"')
        }
    }
}
