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
        WHERE date BETWEEN :fromDate AND :toDate
        ORDER BY date ASC
        """,
    )
    abstract fun observeRange(fromDate: String, toDate: String): Flow<List<WorkDayEntity>>

    @Upsert
    abstract suspend fun upsert(entity: WorkDayEntity)

    @Query("DELETE FROM work_days WHERE date = :date")
    abstract suspend fun deleteByDate(date: String)

    @Query("SELECT date FROM work_days WHERE date IN (:dates)")
    protected abstract suspend fun existingDates(dates: List<String>): List<String>

    @Upsert
    protected abstract suspend fun upsertAll(entities: List<WorkDayEntity>)

    @Query("DELETE FROM work_days WHERE date IN (:dates)")
    protected abstract suspend fun deleteByDatesInternal(dates: List<String>)

    @Transaction
    open suspend fun insertMissing(entities: List<WorkDayEntity>): List<String> {
        if (entities.isEmpty()) return emptyList()
        val existing = existingDates(entities.map(WorkDayEntity::date)).toHashSet()
        val missing = entities.filterNot { it.date in existing }
        if (missing.isNotEmpty()) upsertAll(missing)
        return missing.map(WorkDayEntity::date)
    }

    @Transaction
    open suspend fun deleteByDates(dates: List<String>) {
        if (dates.isNotEmpty()) deleteByDatesInternal(dates)
    }
}
