package com.arttvad.worktime.data.repository

import com.arttvad.worktime.data.local.WorkDayDao
import com.arttvad.worktime.data.local.WorkDayEntity
import com.arttvad.worktime.domain.model.WorkDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WorkDayRepository {
    fun observeMonth(month: YearMonth): Flow<List<WorkDay>>
    suspend fun upsert(day: WorkDay)
    suspend fun delete(date: LocalDate)
}

class RoomWorkDayRepository(
    private val dao: WorkDayDao,
) : WorkDayRepository {
    override fun observeMonth(month: YearMonth): Flow<List<WorkDay>> {
        val first = month.atDay(1).toString()
        val last = month.atEndOfMonth().toString()
        return dao.observeRange(first, last).map { entities -> entities.map(WorkDayEntity::toDomain) }
    }

    override suspend fun upsert(day: WorkDay) {
        dao.upsert(day.toEntity())
    }

    override suspend fun delete(date: LocalDate) {
        dao.deleteByDate(date.toString())
    }
}

private fun WorkDayEntity.toDomain() = WorkDay(
    date = LocalDate.parse(date),
    workedMinutes = workedMinutes,
    overtimeMinutes = overtimeMinutes,
    note = note,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun WorkDay.toEntity() = WorkDayEntity(
    date = date.toString(),
    workedMinutes = workedMinutes,
    overtimeMinutes = overtimeMinutes,
    note = note,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
