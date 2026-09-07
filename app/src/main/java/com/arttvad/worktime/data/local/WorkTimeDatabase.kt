package com.arttvad.worktime.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WorkDayEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WorkTimeDatabase : RoomDatabase() {
    abstract fun workDayDao(): WorkDayDao
}
