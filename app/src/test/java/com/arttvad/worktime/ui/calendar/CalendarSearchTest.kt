package com.arttvad.worktime.ui.calendar

import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarSearchTest {
    private val workEntry = WorkDay(
        date = LocalDate.of(2026, 9, 5),
        workedMinutes = 8 * 60,
        overtimeMinutes = 0,
        note = "Командировка в офис",
        updatedAtEpochMillis = 1L,
        type = WorkDayType.WORK,
    )
    private val vacationEntry = WorkDay(
        date = LocalDate.of(2026, 9, 2),
        workedMinutes = 0,
        overtimeMinutes = 0,
        note = "Море",
        updatedAtEpochMillis = 2L,
        type = WorkDayType.VACATION,
    )
    private val sickEntry = WorkDay(
        date = LocalDate.of(2026, 9, 9),
        workedMinutes = 0,
        overtimeMinutes = 0,
        note = "",
        updatedAtEpochMillis = 3L,
        type = WorkDayType.SICK,
    )

    @Test
    fun noteSearchIsTrimmedCaseInsensitiveAndSortedByDate() {
        val earlierMatch = workEntry.copy(
            date = LocalDate.of(2026, 9, 1),
            note = "  командировка утром  ",
        )

        val result = filterCalendarEntries(
            entries = listOf(workEntry, earlierMatch, vacationEntry),
            query = "  КОМАНДИРОВКА ",
            filter = CalendarEntryFilter.ALL,
        )

        assertEquals(listOf(earlierMatch, workEntry), result)
    }

    @Test
    fun dayTypeFilterWorksWithEmptyQuery() {
        val result = filterCalendarEntries(
            entries = listOf(workEntry, vacationEntry, sickEntry),
            query = "",
            filter = CalendarEntryFilter.VACATION,
        )

        assertEquals(listOf(vacationEntry), result)
    }

    @Test
    fun queryAndTypeFilterAreCombined() {
        val result = filterCalendarEntries(
            entries = listOf(workEntry, vacationEntry, sickEntry),
            query = "море",
            filter = CalendarEntryFilter.WORK,
        )

        assertEquals(emptyList<WorkDay>(), result)
    }
}
