package com.arttvad.worktime

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arttvad.worktime.data.local.DEFAULT_PROFILE_ID
import com.arttvad.worktime.data.local.DEFAULT_PROFILE_NAME
import com.arttvad.worktime.data.local.WorkDayEntity
import com.arttvad.worktime.data.local.WorkProfileEntity
import com.arttvad.worktime.data.local.WorkTimeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkProfileMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val databaseName = "work-profile-migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration3To4PreservesLegacyRowsAndAddsProfileIsolation() = runBlocking {
        val path = context.getDatabasePath(databaseName)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE `work_days` (
                    `date` TEXT NOT NULL,
                    `workedMinutes` INTEGER NOT NULL,
                    `overtimeMinutes` INTEGER NOT NULL,
                    `note` TEXT NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    `hourlyRateOverrideMinor` INTEGER,
                    `dayType` TEXT NOT NULL DEFAULT 'WORK',
                    PRIMARY KEY(`date`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO `work_days` (
                    `date`, `workedMinutes`, `overtimeMinutes`, `note`,
                    `updatedAtEpochMillis`, `hourlyRateOverrideMinor`, `dayType`
                ) VALUES ('2026-09-08', 510, 30, 'legacy', 123456, 2500, 'WORK')
                """.trimIndent(),
            )
            database.version = 3
        }

        val migrated = Room.databaseBuilder(context, WorkTimeDatabase::class.java, databaseName)
            .addMigrations(WorkTimeDatabase.MIGRATION_3_4)
            .build()
        try {
            val profiles = migrated.workProfileDao().observeAll().first()
            assertEquals(1, profiles.size)
            assertEquals(DEFAULT_PROFILE_ID, profiles.single().id)
            assertEquals(DEFAULT_PROFILE_NAME, profiles.single().name)

            val legacy = migrated.workDayDao().getAll(DEFAULT_PROFILE_ID).single()
            assertEquals("2026-09-08", legacy.date)
            assertEquals(510, legacy.workedMinutes)
            assertEquals(30, legacy.overtimeMinutes)
            assertEquals("legacy", legacy.note)
            assertEquals(123456L, legacy.updatedAtEpochMillis)
            assertEquals(2500L, legacy.hourlyRateOverrideMinor)
            assertEquals("WORK", legacy.dayType)

            migrated.workProfileDao().upsert(
                WorkProfileEntity(
                    id = 2L,
                    name = "Подработка",
                    createdAtEpochMillis = 1L,
                ),
            )
            migrated.workDayDao().upsert(
                WorkDayEntity(
                    profileId = 2L,
                    date = "2026-09-08",
                    workedMinutes = 240,
                    overtimeMinutes = 0,
                    note = "second profile",
                    updatedAtEpochMillis = 2L,
                ),
            )

            assertEquals(1, migrated.workDayDao().getAll(DEFAULT_PROFILE_ID).size)
            assertEquals(1, migrated.workDayDao().getAll(2L).size)
            assertNotNull(migrated.workProfileDao().getById(2L))
        } finally {
            migrated.close()
        }
    }
}
