package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calculation.EarningsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EarningsCalculatorTest {
    @Test
    fun zeroMinutesProducesZero() {
        assertEquals(0L, EarningsCalculator.calculateMinor(0, 1_500L))
    }

    @Test
    fun fullHourUsesHourlyRateExactly() {
        assertEquals(1_550L, EarningsCalculator.calculateMinor(60, 1_550L))
    }

    @Test
    fun halfHourUsesHalfUpMinorUnitRounding() {
        assertEquals(778L, EarningsCalculator.calculateMinor(30, 1_555L))
    }

    @Test
    fun negativeInputsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            EarningsCalculator.calculateMinor(-1, 1_000L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EarningsCalculator.calculateMinor(60, -1L)
        }
    }
}
