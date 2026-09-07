package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calculation.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun parsesCommaDecimalForTwoDigitCurrency() {
        assertEquals(1_550L, MoneyFormatter.parseMajorToMinor("15,50", "EUR"))
    }

    @Test
    fun parsesZeroFractionCurrency() {
        assertEquals(1_556L, MoneyFormatter.parseMajorToMinor("1555.6", "JPY"))
    }

    @Test
    fun rejectsNegativeRate() {
        assertThrows(IllegalArgumentException::class.java) {
            MoneyFormatter.parseMajorToMinor("-1", "EUR")
        }
    }
}
