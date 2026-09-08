package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calculation.DetailedYearStatisticsCalculator
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.Year
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailedYearStatisticsCalculatorTest {
    @Test
    fun calculatesAnnualTotalsAndTwelveMonthBreakdown() {
        val days = listOf(
            WorkDay(LocalDate.of(2026, 1, 10), 480, 60, "", 1L),
            WorkDay(LocalDate.of(2026, 1, 11), 0, 0, "", 2L, type = WorkDayType.DAY_OFF),
            WorkDay(LocalDate.of(2026, 2, 10), 600, 0, "", 3L),
            WorkDay(LocalDate.of(2026, 2, 11), 0, 0, "", 4L, type = WorkDayType.VACATION),
            WorkDay(LocalDate.of(2025, 12, 31), 720, 0, "outside", 5L),
        )

        val stats = DetailedYearStatisticsCalculator.calculate(
            year = Year.of(2026),
            days = days,
            hourlyRateMinor = 1_200L,
        )

        assertEquals(2, stats.totals.workDays)
        assertEquals(1, stats.totals.daysOff)
        assertEquals(1, stats.totals.vacationDays)
        assertEquals(1_080, stats.totals.workedMinutes)
        assertEquals(60, stats.totals.overtimeMinutes)
        assertEquals(540, stats.totals.averageWorkedMinutes)
        assertEquals(600, stats.totals.longestWorkedMinutes)
        assertEquals(21_600L, stats.totals.earningsMinor)
        assertEquals(12, stats.months.size)
        assertEquals(480, stats.months[0].workedMinutes)
        assertEquals(1, stats.months[0].workDays)
        assertEquals(9_600L, stats.months[0].earningsMinor)
        assertEquals(600, stats.months[1].workedMinutes)
        assertEquals(12_000L, stats.months[1].earningsMinor)
        assertEquals(0, stats.months[11].workedMinutes)
        assertEquals(0L, stats.months[11].earningsMinor)
    }

    @Test
    fun missingRateMakesOnlyAffectedMoneyUnknownWhileTimeRemainsAvailable() {
        val days = listOf(
            WorkDay(
                date = LocalDate.of(2026, 1, 10),
                workedMinutes = 480,
                overtimeMinutes = 0,
                note = "",
                updatedAtEpochMillis = 1L,
                hourlyRateOverrideMinor = 1_500L,
            ),
            WorkDay(
                date = LocalDate.of(2026, 2, 10),
                workedMinutes = 480,
                overtimeMinutes = 0,
                note = "",
                updatedAtEpochMillis = 2L,
            ),
        )

        val stats = DetailedYearStatisticsCalculator.calculate(
            year = Year.of(2026),
            days = days,
            hourlyRateMinor = null,
        )

        assertEquals(960, stats.totals.workedMinutes)
        assertNull(stats.totals.earningsMinor)
        assertEquals(12_000L, stats.months[0].earningsMinor)
        assertNull(stats.months[1].earningsMinor)
    }
}
