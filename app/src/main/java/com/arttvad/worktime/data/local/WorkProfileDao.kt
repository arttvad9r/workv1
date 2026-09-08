package com.arttvad.worktime.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WorkProfileDao {
    @Query("SELECT * FROM work_profiles ORDER BY id ASC")
    suspend fun getAll(): List<WorkProfileEntity>

    @Query("SELECT * FROM work_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkProfileEntity?

    @Upsert
    suspend fun upsert(profile: WorkProfileEntity)
}
