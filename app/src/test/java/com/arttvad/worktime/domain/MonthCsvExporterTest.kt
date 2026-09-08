package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.exporting.MonthCsvExporter
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthCsvExporterTest {
    private val month = YearMonth.of(2026, 9)

    @Test
    fun exportsStableMetadataSortedRowsRatesAndEarnings() {
        val csv = MonthCsvExporter.export(
            month = month,
            days = listOf(
                WorkDay(
                    date = LocalDate.of(2026, 9, 2),
                    workedMinutes = 480,
                    overtimeMinutes = 0,
                    note = "override",
                    updatedAtEpochMillis = 2L,
                    hourlyRateOverrideMinor = 2_000L,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 9, 1),
                    workedMinutes = 600,
                    overtimeMinutes = 60,
                    note = "base",
                    updatedAtEpochMillis = 1L,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 9, 3),
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 3L,
                    type = WorkDayType.VACATION,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 10, 1),
                    workedMinutes = 480,
                    overtimeMinutes = 0,
                    note = "outside period",
                    updatedAtEpochMillis = 4L,
                ),
            ),
            hourlyRateMinor = 1_500L,
            currencyCode = "EUR",
        )

        val lines = csv.trimEnd().lines()
        assertEquals(
            "record_type,period,currency,date,day_type,worked_minutes,overtime_minutes,effective_hourly_rate,earnings,note",
            lines[0],
        )
        assertEquals("metadata,2026-09,EUR,,,,,,,", lines[1])
        assertEquals("day,2026-09,EUR,2026-09-01,WORK,600,60,15.00,150.00,base", lines[2])
        assertEquals("day,2026-09,EUR,2026-09-02,WORK,480,0,20.00,160.00,override", lines[3])
        assertEquals("day,2026-09,EUR,2026-09-03,VACATION,0,0,,,", lines[4])
        assertEquals(5, lines.size)
        assertFalse(csv.contains("outside period"))
    }

    @Test
    fun escapesNotesWithoutChangingTheirContent() {
        val csv = MonthCsvExporter.export(
            month = month,
            days = listOf(
                WorkDay(
                    date = LocalDate.of(2026, 9, 1),
                    workedMinutes = 60,
                    overtimeMinutes = 0,
                    note = "проверка, \"ночь\"\nвторая строка",
                    updatedAtEpochMillis = 1L,
                ),
            ),
            hourlyRateMinor = null,
            currencyCode = "EUR",
        )

        assertTrue(csv.contains("\"проверка, \"\"ночь\"\"\nвторая строка\""))
        assertTrue(csv.contains("day,2026-09,EUR,2026-09-01,WORK,60,0,,,"))
    }

    @Test
    fun emptyMonthStillExportsExplicitPeriodAndCurrency() {
        val csv = MonthCsvExporter.export(
            month = month,
            days = emptyList(),
            hourlyRateMinor = null,
            currencyCode = "USD",
        )

        val lines = csv.trimEnd().lines()
        assertEquals(2, lines.size)
        assertEquals("metadata,2026-09,USD,,,,,,,", lines[1])
    }
}
