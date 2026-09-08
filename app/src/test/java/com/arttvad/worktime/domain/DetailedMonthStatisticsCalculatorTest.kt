package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calculation.DetailedMonthStatisticsCalculator
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailedMonthStatisticsCalculatorTest {
    @Test
    fun calculatesBreakdownAndWorkDayMetrics() {
        val days = listOf(
            WorkDay(LocalDate.of(2026, 9, 1), 480, 60, "", 1L),
            WorkDay(LocalDate.of(2026, 9, 2), 600, 0, "", 2L),
            WorkDay(LocalDate.of(2026, 9, 3), 0, 0, "", 3L, type = WorkDayType.DAY_OFF),
            WorkDay(LocalDate.of(2026, 9, 4), 0, 0, "", 4L, type = WorkDayType.VACATION),
            WorkDay(LocalDate.of(2026, 9, 5), 0, 0, "", 5L, type = WorkDayType.SICK),
        )

        val stats = DetailedMonthStatisticsCalculator.calculate(days, hourlyRateMinor = 1_200L)

        assertEquals(2, stats.workDays)
        assertEquals(1, stats.daysOff)
        assertEquals(1, stats.vacationDays)
        assertEquals(1, stats.sickDays)
        assertEquals(1_080, stats.workedMinutes)
        assertEquals(60, stats.overtimeMinutes)
        assertEquals(540, stats.averageWorkedMinutes)
        assertEquals(600, stats.longestWorkedMinutes)
        assertEquals(21_600L, stats.earningsMinor)
    }

    @Test
    fun nonWorkDaysDoNotInvalidateOverrideOnlyEarnings() {
        val days = listOf(
            WorkDay(
                date = LocalDate.of(2026, 9, 1),
                workedMinutes = 480,
                overtimeMinutes = 0,
                note = "",
                updatedAtEpochMillis = 1L,
                hourlyRateOverrideMinor = 1_500L,
            ),
            WorkDay(
                date = LocalDate.of(2026, 9, 2),
                workedMinutes = 0,
                overtimeMinutes = 0,
                note = "",
                updatedAtEpochMillis = 2L,
                type = WorkDayType.VACATION,
            ),
        )

        val stats = DetailedMonthStatisticsCalculator.calculate(days, hourlyRateMinor = null)

        assertEquals(12_000L, stats.earningsMinor)
        assertEquals(1, stats.workDays)
        assertEquals(1, stats.vacationDays)
    }

    @Test
    fun emptyMonthKeepsZeroMetrics() {
        val stats = DetailedMonthStatisticsCalculator.calculate(emptyList(), hourlyRateMinor = 1_200L)

        assertEquals(0, stats.workDays)
        assertEquals(0, stats.averageWorkedMinutes)
        assertEquals(0, stats.longestWorkedMinutes)
        assertEquals(0L, stats.earningsMinor)
    }
}
