package com.arttvad.worktime.widget

import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkTimeWidgetSnapshotFactoryTest {
    @Test
    fun currentMonthSnapshotFiltersOtherMonthsAndUsesEffectiveRate() {
        val snapshot = WorkTimeWidgetSnapshotFactory.create(
            month = YearMonth.of(2026, 9),
            days = listOf(
                WorkDay(
                    date = LocalDate.of(2026, 9, 1),
                    workedMinutes = 480,
                    overtimeMinutes = 60,
                    note = "",
                    updatedAtEpochMillis = 1L,
                    hourlyRateOverrideMinor = 2_000L,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 9, 2),
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 2L,
                    type = WorkDayType.VACATION,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 10, 1),
                    workedMinutes = 600,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 3L,
                ),
            ),
            hourlyRateMinor = 1_500L,
            currencyCode = "EUR",
            locale = Locale.US,
        )

        assertEquals("September 2026", snapshot.monthTitle)
        assertEquals("8 ч", snapshot.worked)
        assertEquals("€160.00", snapshot.earned)
        assertEquals("1 ч", snapshot.overtime)
    }
}
