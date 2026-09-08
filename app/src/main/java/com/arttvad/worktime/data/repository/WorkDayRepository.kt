package com.arttvad.worktime.data.repository

import com.arttvad.worktime.data.local.DEFAULT_PROFILE_ID
import com.arttvad.worktime.data.local.WorkDayDao
import com.arttvad.worktime.data.local.WorkDayEntity
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WorkDayRepository {
    fun observeMonth(month: YearMonth): Flow<List<WorkDay>>
    fun observeYear(year: Year): Flow<List<WorkDay>>
    suspend fun snapshotAll(): List<WorkDay>
    suspend fun upsert(day: WorkDay)
    suspend fun delete(date: LocalDate)
    suspend fun insertMissing(days: List<WorkDay>): List<LocalDate>
    suspend fun deleteAll(dates: List<LocalDate>)
    suspend fun replaceAll(days: List<WorkDay>)
}

class RoomWorkDayRepository(
    private val dao: WorkDayDao,
    private val profileId: Long = DEFAULT_PROFILE_ID,
) : WorkDayRepository {
    override fun observeMonth(month: YearMonth): Flow<List<WorkDay>> =
        observeRange(month.atDay(1), month.atEndOfMonth())

    override fun observeYear(year: Year): Flow<List<WorkDay>> =
        observeRange(year.atDay(1), year.atMonth(12).atEndOfMonth())

    private fun observeRange(first: LocalDate, last: LocalDate): Flow<List<WorkDay>> =
        dao.observeRange(profileId, first.toString(), last.toString())
            .map { entities -> entities.map(WorkDayEntity::toDomain) }

    override suspend fun snapshotAll(): List<WorkDay> =
        dao.getAll(profileId).map(WorkDayEntity::toDomain)

    override suspend fun upsert(day: WorkDay) {
        dao.upsert(day.toEntity(profileId))
    }

    override suspend fun delete(date: LocalDate) {
        dao.deleteByDate(profileId, date.toString())
    }

    override suspend fun insertMissing(days: List<WorkDay>): List<LocalDate> =
        dao.insertMissing(profileId, days.map { day -> day.toEntity(profileId) })
            .map(LocalDate::parse)

    override suspend fun deleteAll(dates: List<LocalDate>) {
        dao.deleteByDates(profileId, dates.map(LocalDate::toString))
    }

    override suspend fun replaceAll(days: List<WorkDay>) {
        dao.replaceAll(profileId, days.map { day -> day.toEntity(profileId) })
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
