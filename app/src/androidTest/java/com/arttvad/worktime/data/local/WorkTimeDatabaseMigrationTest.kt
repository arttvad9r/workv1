package com.arttvad.worktime.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkTimeDatabaseMigrationTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun deleteDatabaseBeforeTest() {
        context.deleteDatabase(DatabaseName)
    }

    @After
    fun deleteDatabaseAfterTest() {
        context.deleteDatabase(DatabaseName)
    }

    @Test
    fun migration1To4PreservesRowsAndAssignsLegacyDataToDefaultProfile() {
        val databaseFile = context.getDatabasePath(DatabaseName)
        databaseFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { legacyDatabase ->
            legacyDatabase.execSQL(
                """
                CREATE TABLE `work_days` (
                    `date` TEXT NOT NULL,
                    `workedMinutes` INTEGER NOT NULL,
                    `overtimeMinutes` INTEGER NOT NULL,
                    `note` TEXT NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`date`)
                )
                """.trimIndent(),
            )
            legacyDatabase.execSQL(
                """
                INSERT INTO `work_days`
                    (`date`, `workedMinutes`, `overtimeMinutes`, `note`, `updatedAtEpochMillis`)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>("2026-09-08", 480, 30, "legacy", 1234L),
            )
            legacyDatabase.version = 1
        }

        val database = Room.databaseBuilder(
            context,
            WorkTimeDatabase::class.java,
            DatabaseName,
        )
            .addMigrations(
                WorkTimeDatabase.MIGRATION_1_2,
                WorkTimeDatabase.MIGRATION_2_3,
                WorkTimeDatabase.MIGRATION_3_4,
            )
            .build()

        try {
            val migrated = runBlocking {
                database.workDayDao()
                    .observeRange(DEFAULT_PROFILE_ID, "2026-09-01", "2026-09-30")
                    .first()
                    .single()
            }
            val profile = runBlocking {
                database.workProfileDao().getById(DEFAULT_PROFILE_ID)
            }

            assertEquals(DEFAULT_PROFILE_ID, migrated.profileId)
            assertEquals("2026-09-08", migrated.date)
            assertEquals(480, migrated.workedMinutes)
            assertEquals(30, migrated.overtimeMinutes)
            assertEquals("legacy", migrated.note)
            assertEquals(1234L, migrated.updatedAtEpochMillis)
            assertNull(migrated.hourlyRateOverrideMinor)
            assertEquals("WORK", migrated.dayType)
            assertEquals(DEFAULT_PROFILE_NAME, profile?.name)
            assertEquals(0L, profile?.createdAtEpochMillis)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DatabaseName = "worktime-migration-test.db"
    }
}
