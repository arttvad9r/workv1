package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calculation.MonthSummaryCalculator
import com.arttvad.worktime.domain.model.WorkDay
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
}
