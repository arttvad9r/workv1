package com.arttvad.worktime

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.arttvad.worktime.data.local.WorkTimeDatabase
import com.arttvad.worktime.data.preferences.WorkPreferencesRepository
import com.arttvad.worktime.data.repository.ProfileBackupRepository
import com.arttvad.worktime.data.repository.RoomProfileBackupRepository
import com.arttvad.worktime.data.repository.RoomWorkDayRepository
import com.arttvad.worktime.data.repository.RoomWorkProfileRepository
import com.arttvad.worktime.data.repository.WorkDayRepository
import com.arttvad.worktime.data.repository.WorkProfileRepository
import kotlinx.coroutines.flow.map

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
            WorkTimeDatabase.MIGRATION_3_4,
            WorkTimeDatabase.MIGRATION_4_5,
        )
        .build()

    val preferencesRepository = WorkPreferencesRepository(context.applicationContext)
    val workDayRepository: WorkDayRepository = RoomWorkDayRepository(
        dao = database.workDayDao(),
        activeProfileId = preferencesRepository.preferences.map { it.activeProfileId },
    )
    val workProfileRepository: WorkProfileRepository = RoomWorkProfileRepository(database.workProfileDao())
    val profileBackupRepository: ProfileBackupRepository = RoomProfileBackupRepository(database)
}
