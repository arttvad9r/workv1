package com.arttvad.worktime

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arttvad.worktime.data.local.WorkTimeDatabase
import com.arttvad.worktime.data.repository.RoomWorkDayRepository
import com.arttvad.worktime.domain.model.WorkDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveProfileIsolationTest {
    @Test
    fun repositorySwitchesProfilesWithoutMixingSameDateRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, WorkTimeDatabase::class.java).build()
        try {
            val activeProfileId = MutableStateFlow(1L)
            val repository = RoomWorkDayRepository(
                dao = database.workDayDao(),
                activeProfileId = activeProfileId,
            )
            val date = LocalDate.of(2026, 9, 8)
            val month = YearMonth.of(2026, 9)

            repository.upsert(
                WorkDay(
                    date = date,
                    workedMinutes = 480,
                    overtimeMinutes = 0,
                    note = "primary",
                    updatedAtEpochMillis = 1L,
                ),
            )
            assertEquals("primary", repository.observeMonth(month).first().single().note)

            activeProfileId.value = 2L
            assertEquals(emptyList<WorkDay>(), repository.observeMonth(month).first())
            repository.upsert(
                WorkDay(
                    date = date,
                    workedMinutes = 240,
                    overtimeMinutes = 30,
                    note = "secondary",
                    updatedAtEpochMillis = 2L,
                ),
            )
            assertEquals("secondary", repository.observeMonth(month).first().single().note)

            activeProfileId.value = 1L
            val primary = repository.observeMonth(month).first().single()
            assertEquals(480, primary.workedMinutes)
            assertEquals("primary", primary.note)

            activeProfileId.value = 2L
            val secondary = repository.observeMonth(month).first().single()
            assertEquals(240, secondary.workedMinutes)
            assertEquals(30, secondary.overtimeMinutes)
            assertEquals("secondary", secondary.note)
        } finally {
            database.close()
        }
    }
}
