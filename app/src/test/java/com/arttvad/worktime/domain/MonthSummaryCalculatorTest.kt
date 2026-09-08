package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calculation.MonthSummaryCalculator
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthSummaryCalculatorTest {
    private val days = listOf(
        WorkDay(LocalDate.of(2026, 9, 1), 480, 60, "", 1L),
        WorkDay(LocalDate.of(2026, 9, 2), 450, 30, "", 2L),
    )

    @Test
    fun sumsWorkedAndOvertime() {
        val summary = MonthSummaryCalculator.calculate(days, hourlyRateMinor = null)
        assertEquals(930, summary.workedMinutes)
        assertEquals(90, summary.overtimeMinutes)
        assertNull(summary.earningsMinor)
    }

    @Test
    fun calculatesEarningsWhenRateExists() {
        val summary = MonthSummaryCalculator.calculate(days, hourlyRateMinor = 1_200L)
        assertEquals(18_600L, summary.earningsMinor)
    }

    @Test
    fun dayOverrideReplacesBaseRateOnlyForThatDay() {
        val withOverride = days.toMutableList().apply {
            this[0] = this[0].copy(hourlyRateOverrideMinor = 1_500L)
        }

        val summary = MonthSummaryCalculator.calculate(withOverride, hourlyRateMinor = 1_200L)

        assertEquals(21_000L, summary.earningsMinor)
    }

    @Test
    fun allDaysCanUseOverridesWithoutBaseRate() {
        val withOverrides = listOf(
            days[0].copy(hourlyRateOverrideMinor = 1_500L),
            days[1].copy(hourlyRateOverrideMinor = 1_200L),
        )

        val summary = MonthSummaryCalculator.calculate(withOverrides, hourlyRateMinor = null)

        assertEquals(21_000L, summary.earningsMinor)
    }

    @Test
    fun missingRateForAnyWorkedDayKeepsEarningsUnknown() {
        val partiallyRated = listOf(
            days[0].copy(hourlyRateOverrideMinor = 1_500L),
            days[1],
        )

        val summary = MonthSummaryCalculator.calculate(partiallyRated, hourlyRateMinor = null)

        assertNull(summary.earningsMinor)
    }

    @Test
    fun nonWorkDayDoesNotRequireRateForOtherwiseFullyRatedMonth() {
        val mixedDays = listOf(
            days[0].copy(hourlyRateOverrideMinor = 1_500L),
            WorkDay(
                date = LocalDate.of(2026, 9, 2),
                workedMinutes = 0,
                overtimeMinutes = 0,
                note = "",
                updatedAtEpochMillis = 2L,
                type = WorkDayType.DAY_OFF,
            ),
        )

        val summary = MonthSummaryCalculator.calculate(mixedDays, hourlyRateMinor = null)

        assertEquals(12_000L, summary.earningsMinor)
        assertEquals(480, summary.workedMinutes)
        assertEquals(60, summary.overtimeMinutes)
    }

    @Test
    fun nonWorkOnlyMonthWithConfiguredRateHasZeroEarnings() {
        val nonWorkDays = listOf(
            WorkDay(
                date = LocalDate.of(2026, 9, 3),
                workedMinutes = 0,
                overtimeMinutes = 0,
                note = "",
                updatedAtEpochMillis = 3L,
                type = WorkDayType.VACATION,
            ),
        )

        val summary = MonthSummaryCalculator.calculate(nonWorkDays, hourlyRateMinor = 1_200L)

        assertEquals(0L, summary.earningsMinor)
        assertEquals(0, summary.workedMinutes)
        assertEquals(0, summary.overtimeMinutes)
    }
}
