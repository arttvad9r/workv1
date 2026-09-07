package com.arttvad.worktime.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
)

object MonthGridBuilder {
    const val CELL_COUNT = 42

    fun build(month: YearMonth, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): List<CalendarDay> {
        val firstOfMonth = month.atDay(1)
        val leadingDays = Math.floorMod(
            firstOfMonth.dayOfWeek.value - firstDayOfWeek.value,
            7,
        )
        val gridStart = firstOfMonth.minusDays(leadingDays.toLong())

        return List(CELL_COUNT) { index ->
            val date = gridStart.plusDays(index.toLong())
            CalendarDay(date = date, isCurrentMonth = YearMonth.from(date) == month)
        }
    }
}
