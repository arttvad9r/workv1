package com.arttvad.worktime.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShiftTimerCalculatorTest {
    @Test
    fun elapsedMinutesUsesWholeMinutes() {
        val start = 1_000_000L
        val end = start + 8 * 60 * 60_000L + 30 * 60_000L + 42_000L

        assertEquals(510, ShiftTimerCalculator.elapsedMinutes(start, end))
    }

    @Test
    fun elapsedMinutesRejectsSubMinuteAndBackwardsIntervals() {
        val start = 1_000_000L

        assertNull(ShiftTimerCalculator.elapsedMinutes(start, start + 59_999L))
        assertNull(ShiftTimerCalculator.elapsedMinutes(start, start))
        assertNull(ShiftTimerCalculator.elapsedMinutes(start, start - 1L))
    }

    @Test
    fun elapsedMinutesRejectsDurationsThatCannotFitEditorHoursField() {
        val start = 1_000_000L
        val validEnd = start + ShiftTimerCalculator.MAX_TRACKED_MINUTES * 60_000L
        val invalidEnd = validEnd + 60_000L

        assertEquals(
            ShiftTimerCalculator.MAX_TRACKED_MINUTES,
            ShiftTimerCalculator.elapsedMinutes(start, validEnd),
        )
        assertNull(ShiftTimerCalculator.elapsedMinutes(start, invalidEnd))
    }
}
