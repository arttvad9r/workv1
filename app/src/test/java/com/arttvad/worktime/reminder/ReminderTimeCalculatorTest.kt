package com.arttvad.worktime.reminder

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeCalculatorTest {
    private val zone = ZoneId.of("Europe/Amsterdam")

    @Test
    fun usesTodayWhenConfiguredTimeIsStillAhead() {
        val now = ZonedDateTime.of(2026, 9, 9, 18, 15, 0, 0, zone)

        val result = ReminderTimeCalculator.nextTrigger(now, 20, 0)

        assertEquals(ZonedDateTime.of(2026, 9, 9, 20, 0, 0, 0, zone), result)
    }

    @Test
    fun usesNextDayWhenConfiguredTimeHasPassed() {
        val now = ZonedDateTime.of(2026, 9, 9, 21, 30, 0, 0, zone)

        val result = ReminderTimeCalculator.nextTrigger(now, 20, 0)

        assertEquals(ZonedDateTime.of(2026, 9, 10, 20, 0, 0, 0, zone), result)
    }

    @Test
    fun equalTimeSchedulesNextDayInsteadOfImmediateRepeat() {
        val now = ZonedDateTime.of(2026, 9, 9, 20, 0, 0, 0, zone)

        val result = ReminderTimeCalculator.nextTrigger(now, 20, 0)

        assertEquals(ZonedDateTime.of(2026, 9, 10, 20, 0, 0, 0, zone), result)
    }
}
