package com.arttvad.worktime.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkDayDaoBulkTest {
    @Test
    fun insertMissingAndUndoAreScopedToProfile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(
            context,
            WorkTimeDatabase::class.java,
        ).build()

        try {
            val dao = database.workDayDao()
            val existing = WorkDayEntity(
                profileId = DEFAULT_PROFILE_ID,
                date = "2026-09-08",
                workedMinutes = 600,
                overtimeMinutes = 60,
                note = "existing",
                updatedAtEpochMillis = 1L,
                hourlyRateOverrideMinor = 1_500L,
                dayType = "WORK",
            )
            val generated = listOf(
                existing.copy(
                    workedMinutes = 480,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 2L,
                    hourlyRateOverrideMinor = null,
                ),
                WorkDayEntity(
                    profileId = DEFAULT_PROFILE_ID,
                    date = "2026-09-09",
                    workedMinutes = 480,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 2L,
                    dayType = "WORK",
                ),
                WorkDayEntity(
                    profileId = DEFAULT_PROFILE_ID,
                    date = "2026-09-10",
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 2L,
                    dayType = "DAY_OFF",
                ),
            )

            val inserted = runBlocking {
                dao.upsert(existing)
                dao.insertMissing(DEFAULT_PROFILE_ID, generated)
            }

            assertEquals(listOf("2026-09-09", "2026-09-10"), inserted)

            val afterApply = runBlocking {
                dao.observeRange(
                    DEFAULT_PROFILE_ID,
                    "2026-09-08",
                    "2026-09-10",
                ).first()
            }
            assertEquals(3, afterApply.size)
            assertEquals(existing, afterApply.first { it.date == existing.date })

            runBlocking { dao.deleteByDates(DEFAULT_PROFILE_ID, inserted) }

            val afterUndo = runBlocking {
                dao.observeRange(
                    DEFAULT_PROFILE_ID,
                    "2026-09-08",
                    "2026-09-10",
                ).first()
            }
            assertEquals(listOf(existing), afterUndo)

            val secondProfileId = 2L
            val secondProfileDay = existing.copy(
                profileId = secondProfileId,
                workedMinutes = 300,
                note = "second profile",
            )
            runBlocking { dao.upsert(secondProfileDay) }

            val defaultProfileRows = runBlocking {
                dao.observeRange(DEFAULT_PROFILE_ID, existing.date, existing.date).first()
            }
            val secondProfileRows = runBlocking {
                dao.observeRange(secondProfileId, existing.date, existing.date).first()
            }
            assertEquals(listOf(existing), defaultProfileRows)
            assertEquals(listOf(secondProfileDay), secondProfileRows)
        } finally {
            database.close()
        }
    }
}
