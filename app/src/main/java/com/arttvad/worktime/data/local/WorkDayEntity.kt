package com.arttvad.worktime.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "work_days",
    primaryKeys = ["profileId", "date"],
    indices = [Index("profileId")],
)
data class WorkDayEntity(
    val profileId: Long = DEFAULT_PROFILE_ID,
    val date: String,
    val workedMinutes: Int,
    val overtimeMinutes: Int,
    val note: String,
    val updatedAtEpochMillis: Long,
    val hourlyRateOverrideMinor: Long? = null,
    val dayType: String = "WORK",
)
