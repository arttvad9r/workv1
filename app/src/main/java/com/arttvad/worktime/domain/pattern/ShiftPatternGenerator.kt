package com.arttvad.worktime.domain.pattern

import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ShiftPatternDay(
    val date: LocalDate,
    val type: WorkDayType,
    val workedMinutes: Int,
)

object ShiftPatternGenerator {
    const val MAX_RANGE_DAYS = 366

    fun generate(
        startDate: LocalDate,
        endDate: LocalDate,
        workDays: Int,
        offDays: Int,
        workedMinutes: Int,
    ): List<ShiftPatternDay>? {
        if (endDate.isBefore(startDate)) return null
        if (workDays !in 1..31 || offDays !in 1..31) return null
        if (workedMinutes !in 1..24 * 60) return null

        val dayCount = ChronoUnit.DAYS.between(startDate, endDate) + 1
        if (dayCount !in 1..MAX_RANGE_DAYS.toLong()) return null

        val cycleLength = workDays + offDays
        return List(dayCount.toInt()) { index ->
            val isWorkDay = index % cycleLength < workDays
            ShiftPatternDay(
                date = startDate.plusDays(index.toLong()),
                type = if (isWorkDay) WorkDayType.WORK else WorkDayType.DAY_OFF,
                workedMinutes = if (isWorkDay) workedMinutes else 0,
            )
        }
    }
}
