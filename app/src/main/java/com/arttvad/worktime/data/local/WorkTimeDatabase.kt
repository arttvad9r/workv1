package com.arttvad.worktime.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [WorkDayEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class WorkTimeDatabase : RoomDatabase() {
    abstract fun workDayDao(): WorkDayDao

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
    }
}
