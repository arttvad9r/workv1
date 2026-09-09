package com.arttvad.worktime.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkDayDao {
    @Query(
        """
        SELECT * FROM work_days
        WHERE profileId = :profileId
          AND date BETWEEN :fromDate AND :toDate
        ORDER BY date ASC
        """,
    )
    abstract fun observeRange(
        profileId: Long,
        fromDate: String,
        toDate: String,
    ): Flow<List<WorkDayEntity>>

    @Query(
        """
        SELECT * FROM work_days
        WHERE date BETWEEN :fromDate AND :toDate
        ORDER BY profileId ASC, date ASC
        """,
    )
    abstract fun observeRangeAllProfiles(
        fromDate: String,
        toDate: String,
    ): Flow<List<WorkDayEntity>>

    @Query("SELECT * FROM work_days WHERE profileId = :profileId ORDER BY date ASC")
    abstract suspend fun getAll(profileId: Long): List<WorkDayEntity>

    @Query("SELECT * FROM work_days ORDER BY profileId ASC, date ASC")
    abstract suspend fun getAllProfiles(): List<WorkDayEntity>

    @Upsert
    abstract suspend fun upsert(entity: WorkDayEntity)

    @Upsert
    abstract suspend fun upsertAll(entities: List<WorkDayEntity>)

    @Query("DELETE FROM work_days WHERE profileId = :profileId AND date = :date")
    abstract suspend fun deleteByDate(profileId: Long, date: String)

    @Query("SELECT date FROM work_days WHERE profileId = :profileId AND date IN (:dates)")
    protected abstract suspend fun existingDates(profileId: Long, dates: List<String>): List<String>

    @Query("DELETE FROM work_days WHERE profileId = :profileId AND date IN (:dates)")
    protected abstract suspend fun deleteByDatesInternal(profileId: Long, dates: List<String>)

    @Query("DELETE FROM work_days WHERE profileId = :profileId")
    protected abstract suspend fun deleteAllInternal(profileId: Long)

    @Query("DELETE FROM work_days")
    abstract suspend fun deleteEverything()

    @Transaction
    open suspend fun insertMissing(profileId: Long, entities: List<WorkDayEntity>): List<String> {
        if (entities.isEmpty()) return emptyList()
        require(entities.all { it.profileId == profileId }) { "All entities must belong to profile $profileId" }
        val existing = existingDates(profileId, entities.map(WorkDayEntity::date)).toHashSet()
        val missing = entities.filterNot { it.date in existing }
        if (missing.isNotEmpty()) upsertAll(missing)
        return missing.map(WorkDayEntity::date)
    }

    @Transaction
    open suspend fun deleteByDates(profileId: Long, dates: List<String>) {
        if (dates.isNotEmpty()) deleteByDatesInternal(profileId, dates)
    }

    @Transaction
    open suspend fun replaceAll(profileId: Long, entities: List<WorkDayEntity>) {
        require(entities.all { it.profileId == profileId }) { "All entities must belong to profile $profileId" }
        deleteAllInternal(profileId)
        if (entities.isNotEmpty()) upsertAll(entities)
    }
}
