package com.arttvad.worktime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_PROFILE_ID: Long = 1L
const val DEFAULT_PROFILE_NAME: String = "Основная работа"

@Entity(tableName = "work_profiles")
data class WorkProfileEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val createdAtEpochMillis: Long,
    val hourlyRateMinor: Long? = null,
    val currencyCode: String? = null,
)
