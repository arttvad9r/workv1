package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.model.WorkDayType
import com.arttvad.worktime.domain.pattern.ShiftPatternGenerator
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShiftPatternGeneratorTest {
    @Test
    fun twoOnTwoOffRepeatsFromFirstWorkDay() {
        val days = requireNotNull(
            ShiftPatternGenerator.generate(
                startDate = LocalDate.of(2026, 9, 1),
                endDate = LocalDate.of(2026, 9, 8),
                workDays = 2,
                offDays = 2,
                workedMinutes = 12 * 60,
            ),
        )

        assertEquals(
            listOf(
                WorkDayType.WORK,
                WorkDayType.WORK,
                WorkDayType.DAY_OFF,
                WorkDayType.DAY_OFF,
                WorkDayType.WORK,
                WorkDayType.WORK,
                WorkDayType.DAY_OFF,
                WorkDayType.DAY_OFF,
            ),
            days.map { it.type },
        )
        assertEquals(listOf(720, 720, 0, 0, 720, 720, 0, 0), days.map { it.workedMinutes })
    }

    @Test
    fun threeOnThreeOffRepeatsAcrossMonthBoundary() {
        val days = requireNotNull(
            ShiftPatternGenerator.generate(
                startDate = LocalDate.of(2026, 9, 29),
                endDate = LocalDate.of(2026, 10, 4),
                workDays = 3,
                offDays = 3,
                workedMinutes = 8 * 60,
            ),
        )

        assertEquals(LocalDate.of(2026, 10, 4), days.last().date)
        assertEquals(
            listOf(
                WorkDayType.WORK,
                WorkDayType.WORK,
                WorkDayType.WORK,
                WorkDayType.DAY_OFF,
                WorkDayType.DAY_OFF,
                WorkDayType.DAY_OFF,
            ),
            days.map { it.type },
        )
    }

    @Test
    fun invalidRangeIsRejected() {
        assertNull(
            ShiftPatternGenerator.generate(
                startDate = LocalDate.of(2026, 9, 2),
                endDate = LocalDate.of(2026, 9, 1),
                workDays = 2,
                offDays = 2,
                workedMinutes = 8 * 60,
            ),
        )
    }

    @Test
    fun rangeLongerThanOneYearIsRejected() {
        val start = LocalDate.of(2026, 1, 1)
        assertNull(
            ShiftPatternGenerator.generate(
                startDate = start,
                endDate = start.plusDays(366),
                workDays = 2,
                offDays = 2,
                workedMinutes = 8 * 60,
            ),
        )
    }

    @Test
    fun invalidCycleOrDurationIsRejected() {
        val start = LocalDate.of(2026, 9, 1)
        assertNull(ShiftPatternGenerator.generate(start, start, 0, 2, 480))
        assertNull(ShiftPatternGenerator.generate(start, start, 2, 0, 480))
        assertNull(ShiftPatternGenerator.generate(start, start, 2, 2, 0))
        assertNull(ShiftPatternGenerator.generate(start, start, 2, 2, 24 * 60 + 1))
    }
}
