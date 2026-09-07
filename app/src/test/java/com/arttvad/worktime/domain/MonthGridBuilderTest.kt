package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calendar.MonthGridBuilder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthGridBuilderTest {
    @Test
    fun mondayFirstGridAlwaysContainsSixWeeks() {
        val grid = MonthGridBuilder.build(YearMonth.of(2026, 9))
        assertEquals(42, grid.size)
        assertEquals(DayOfWeek.MONDAY, grid.first().date.dayOfWeek)
        assertEquals(LocalDate.of(2026, 8, 31), grid.first().date)
        assertEquals(LocalDate.of(2026, 10, 11), grid.last().date)
    }

    @Test
    fun currentMonthFlagIsCorrect() {
        val grid = MonthGridBuilder.build(YearMonth.of(2026, 9))
        assertFalse(grid.first().isCurrentMonth)
        assertTrue(grid.first { it.date == LocalDate.of(2026, 9, 1) }.isCurrentMonth)
        assertFalse(grid.last().isCurrentMonth)
    }
}
