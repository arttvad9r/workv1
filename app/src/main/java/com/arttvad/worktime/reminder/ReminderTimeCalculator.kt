package com.arttvad.worktime.reminder

import java.time.LocalTime
import java.time.ZonedDateTime

object ReminderTimeCalculator {
    fun nextTrigger(now: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
        require(hour in 0..23)
        require(minute in 0..59)
        val time = LocalTime.of(hour, minute)
        val today = now.toLocalDate().atTime(time).atZone(now.zone)
        return if (today.isAfter(now)) {
            today
        } else {
            now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
        }
    }
}
