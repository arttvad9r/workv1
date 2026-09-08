package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.exporting.MonthReportDataBuilder
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthReportDataBuilderTest {
    @Test
    fun filtersSortsAndCalculatesEffectiveRatesForReportRows() {
        val month = YearMonth.of(2026, 9)
        val report = MonthReportDataBuilder.build(
            month = month,
            days = listOf(
                WorkDay(
                    date = LocalDate.of(2026, 10, 1),
                    workedMinutes = 480,
                    overtimeMinutes = 0,
                    note = "outside month",
                    updatedAtEpochMillis = 3L,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 9, 2),
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "vacation",
                    updatedAtEpochMillis = 2L,
                    type = WorkDayType.VACATION,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 9, 1),
                    workedMinutes = 480,
                    overtimeMinutes = 60,
                    note = "override",
                    updatedAtEpochMillis = 1L,
                    hourlyRateOverrideMinor = 2_000L,
                ),
            ),
            hourlyRateMinor = 1_500L,
            currencyCode = "eur",
        )

        assertEquals("EUR", report.currencyCode)
        assertEquals(2, report.days.size)
        assertEquals(LocalDate.of(2026, 9, 1), report.days[0].date)
        assertEquals(2_000L, report.days[0].effectiveHourlyRateMinor)
        assertEquals(16_000L, report.days[0].earningsMinor)
        assertEquals(LocalDate.of(2026, 9, 2), report.days[1].date)
        assertNull(report.days[1].effectiveHourlyRateMinor)
        assertNull(report.days[1].earningsMinor)
        assertEquals(1, report.statistics.workDays)
        assertEquals(1, report.statistics.vacationDays)
        assertEquals(480, report.statistics.workedMinutes)
        assertEquals(60, report.statistics.overtimeMinutes)
        assertEquals(16_000L, report.statistics.earningsMinor)
    }
}
