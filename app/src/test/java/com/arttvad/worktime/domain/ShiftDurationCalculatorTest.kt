package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calculation.ShiftDurationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShiftDurationCalculatorTest {
    @Test
    fun daytimeShiftSubtractsBreak() {
        assertEquals(
            8 * 60,
            ShiftDurationCalculator.calculate(
                start = "09:00",
                end = "18:00",
                breakMinutes = 60,
            ),
        )
    }

    @Test
    fun overnightShiftWrapsAcrossMidnight() {
        assertEquals(
            7 * 60 + 30,
            ShiftDurationCalculator.calculate(
                start = "22:00",
                end = "06:00",
                breakMinutes = 30,
            ),
        )
    }

    @Test
    fun singleDigitHourIsAccepted() {
        assertEquals(
            8 * 60,
            ShiftDurationCalculator.calculate(
                start = "9:00",
                end = "17:00",
                breakMinutes = 0,
            ),
        )
    }

    @Test
    fun sameStartAndEndIsRejected() {
        assertNull(
            ShiftDurationCalculator.calculate(
                start = "09:00",
                end = "09:00",
                breakMinutes = 0,
            ),
        )
    }

    @Test
    fun invalidClockTimeIsRejected() {
        assertNull(
            ShiftDurationCalculator.calculate(
                start = "24:00",
                end = "18:00",
                breakMinutes = 0,
            ),
        )
    }

    @Test
    fun breakCannotConsumeWholeShift() {
        assertNull(
            ShiftDurationCalculator.calculate(
                start = "09:00",
                end = "10:00",
                breakMinutes = 60,
            ),
        )
    }
}
