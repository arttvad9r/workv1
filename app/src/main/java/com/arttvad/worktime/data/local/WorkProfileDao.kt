package com.arttvad.worktime.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkProfileDao {
    @Query("SELECT * FROM work_profiles ORDER BY id ASC")
    fun observeAll(): Flow<List<WorkProfileEntity>>

    @Query("SELECT * FROM work_profiles ORDER BY id ASC")
    suspend fun getAll(): List<WorkProfileEntity>

    @Query("SELECT * FROM work_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkProfileEntity?

    @Query("SELECT MAX(id) FROM work_profiles")
    suspend fun maxId(): Long?

    @Upsert
    suspend fun upsert(profile: WorkProfileEntity)

    @Upsert
    suspend fun upsertAll(profiles: List<WorkProfileEntity>)

    @Query("DELETE FROM work_profiles")
    suspend fun deleteAll()
}
