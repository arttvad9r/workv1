package com.arttvad.worktime.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [WorkDayEntity::class, WorkProfileEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class WorkTimeDatabase : RoomDatabase() {
    abstract fun workDayDao(): WorkDayDao
    abstract fun workProfileDao(): WorkProfileDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `work_days` ADD COLUMN `hourlyRateOverrideMinor` INTEGER",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `work_days` ADD COLUMN `dayType` TEXT NOT NULL DEFAULT 'WORK'",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `work_profiles` (
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                connection.execSQL(
                    """
                    INSERT OR IGNORE INTO `work_profiles` (`id`, `name`, `createdAtEpochMillis`)
                    VALUES ($DEFAULT_PROFILE_ID, '$DEFAULT_PROFILE_NAME', 0)
                    """.trimIndent(),
                )
                connection.execSQL(
                    """
                    CREATE TABLE `work_days_new` (
                        `profileId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `workedMinutes` INTEGER NOT NULL,
                        `overtimeMinutes` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `updatedAtEpochMillis` INTEGER NOT NULL,
                        `hourlyRateOverrideMinor` INTEGER,
                        `dayType` TEXT NOT NULL,
                        PRIMARY KEY(`profileId`, `date`)
                    )
                    """.trimIndent(),
                )
                connection.execSQL(
                    "CREATE INDEX `index_work_days_profileId` ON `work_days_new` (`profileId`)",
                )
                connection.execSQL(
                    """
                    INSERT INTO `work_days_new` (
                        `profileId`, `date`, `workedMinutes`, `overtimeMinutes`, `note`,
                        `updatedAtEpochMillis`, `hourlyRateOverrideMinor`, `dayType`
                    )
                    SELECT $DEFAULT_PROFILE_ID, `date`, `workedMinutes`, `overtimeMinutes`, `note`,
                        `updatedAtEpochMillis`, `hourlyRateOverrideMinor`, `dayType`
                    FROM `work_days`
                    """.trimIndent(),
                )
                connection.execSQL("DROP TABLE `work_days`")
                connection.execSQL("ALTER TABLE `work_days_new` RENAME TO `work_days`")
            }
        }
    }
}
