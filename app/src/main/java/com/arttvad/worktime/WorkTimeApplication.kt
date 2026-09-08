package com.arttvad.worktime

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.arttvad.worktime.data.local.WorkTimeDatabase
import com.arttvad.worktime.data.preferences.WorkPreferencesRepository
import com.arttvad.worktime.data.repository.RoomWorkDayRepository
import com.arttvad.worktime.data.repository.WorkDayRepository

class WorkTimeApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        WorkTimeDatabase::class.java,
        "worktime.db",
    )
        .addMigrations(
            WorkTimeDatabase.MIGRATION_1_2,
            WorkTimeDatabase.MIGRATION_2_3,
        )
        .build()

    val workDayRepository: WorkDayRepository = RoomWorkDayRepository(database.workDayDao())
    val preferencesRepository = WorkPreferencesRepository(context.applicationContext)
}
