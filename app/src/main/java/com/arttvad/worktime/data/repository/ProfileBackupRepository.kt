package com.arttvad.worktime.data.repository

import androidx.room.withTransaction
import com.arttvad.worktime.data.local.DEFAULT_PROFILE_ID
import com.arttvad.worktime.data.local.DEFAULT_PROFILE_NAME
import com.arttvad.worktime.data.local.WorkDayEntity
import com.arttvad.worktime.data.local.WorkProfileEntity
import com.arttvad.worktime.data.local.WorkTimeDatabase
import com.arttvad.worktime.domain.backup.BackupPayment
import com.arttvad.worktime.domain.backup.BackupProfile
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate

interface ProfileBackupRepository {
    suspend fun snapshotAll(): List<BackupProfile>
    suspend fun replaceAll(profiles: List<BackupProfile>)
}

class RoomProfileBackupRepository(
    private val database: WorkTimeDatabase,
) : ProfileBackupRepository {
    override suspend fun snapshotAll(): List<BackupProfile> = database.withTransaction {
        val profileDao = database.workProfileDao()
        val dayDao = database.workDayDao()
        var profiles = profileDao.getAll()
        val days = dayDao.getAllProfiles()

        if (profiles.isEmpty()) {
            val defaultProfile = WorkProfileEntity(
                id = DEFAULT_PROFILE_ID,
                name = DEFAULT_PROFILE_NAME,
                createdAtEpochMillis = 0L,
            )
            profileDao.upsert(defaultProfile)
            profiles = listOf(defaultProfile)
        }

        val profileIds = profiles.map(WorkProfileEntity::id).toHashSet()
        require(days.all { day -> day.profileId in profileIds }) { "Orphan work-day profile id" }
        val daysByProfile = days.groupBy(WorkDayEntity::profileId)
        profiles.map { profile ->
            BackupProfile(
                id = profile.id,
                name = profile.name,
                createdAtEpochMillis = profile.createdAtEpochMillis,
                days = daysByProfile[profile.id].orEmpty().map(WorkDayEntity::toDomain),
                payment = profile.currencyCode?.let { currencyCode ->
                    BackupPayment(
                        hourlyRateMinor = profile.hourlyRateMinor,
                        currencyCode = currencyCode,
                    )
                },
            )
        }
    }

    override suspend fun replaceAll(profiles: List<BackupProfile>) {
        require(profiles.isNotEmpty()) { "At least one work profile is required" }
        database.withTransaction {
            database.workDayDao().deleteEverything()
            database.workProfileDao().deleteAll()
            database.workProfileDao().upsertAll(
                profiles.map { profile ->
                    WorkProfileEntity(
                        id = profile.id,
                        name = profile.name,
                        createdAtEpochMillis = profile.createdAtEpochMillis,
                        hourlyRateMinor = profile.payment?.hourlyRateMinor,
                        currencyCode = profile.payment?.currencyCode,
                    )
                },
            )
            val days = profiles.flatMap { profile ->
                profile.days.map { day -> day.toEntity(profile.id) }
            }
            if (days.isNotEmpty()) database.workDayDao().upsertAll(days)
        }
    }
}

private fun WorkDayEntity.toDomain() = WorkDay(
    date = LocalDate.parse(date),
    workedMinutes = workedMinutes,
    overtimeMinutes = overtimeMinutes,
    note = note,
    updatedAtEpochMillis = updatedAtEpochMillis,
    hourlyRateOverrideMinor = hourlyRateOverrideMinor,
    type = WorkDayType.valueOf(dayType),
)

private fun WorkDay.toEntity(profileId: Long) = WorkDayEntity(
    profileId = profileId,
    date = date.toString(),
    workedMinutes = workedMinutes,
    overtimeMinutes = overtimeMinutes,
    note = note,
    updatedAtEpochMillis = updatedAtEpochMillis,
    hourlyRateOverrideMinor = hourlyRateOverrideMinor,
    dayType = type.name,
)
