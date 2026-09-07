package com.arttvad.worktime.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDayDao {
    @Query(
        """
        SELECT * FROM work_days
        WHERE date BETWEEN :fromDate AND :toDate
        ORDER BY date ASC
        """,
    )
    fun observeRange(fromDate: String, toDate: String): Flow<List<WorkDayEntity>>

    @Upsert
    suspend fun upsert(entity: WorkDayEntity)

    @Query("DELETE FROM work_days WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
