package com.arttvad.worktime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_days")
data class WorkDayEntity(
    @PrimaryKey val date: String,
    val workedMinutes: Int,
    val overtimeMinutes: Int,
    val note: String,
    val updatedAtEpochMillis: Long,
    val hourlyRateOverrideMinor: Long? = null,
)
