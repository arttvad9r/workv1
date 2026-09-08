package com.arttvad.worktime.data.repository

import com.arttvad.worktime.data.local.DEFAULT_PROFILE_ID
import com.arttvad.worktime.data.local.WorkDayDao
import com.arttvad.worktime.data.local.WorkDayEntity
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class RoomWorkDayRepository(
    private val dao: WorkDayDao,
    activeProfileId: Flow<Long> = flowOf(DEFAULT_PROFILE_ID),
) : WorkDayRepository {
    private val activeProfileId = activeProfileId
        .map { profileId -> profileId.takeIf { it > 0L } ?: DEFAULT_PROFILE_ID }
        .distinctUntilChanged()

    override fun observeMonth(month: YearMonth): Flow<List<WorkDay>> =
        observeRange(month.atDay(1), month.atEndOfMonth())

    override fun observeYear(year: Year): Flow<List<WorkDay>> =
        observeRange(year.atDay(1), year.atMonth(12).atEndOfMonth())

    private fun observeRange(first: LocalDate, last: LocalDate): Flow<List<WorkDay>> =
        activeProfileId.flatMapLatest { profileId ->
            dao.observeRange(profileId, first.toString(), last.toString())
                .map { entities -> entities.map(WorkDayEntity::toDomain) }
        }

    override suspend fun snapshotAll(): List<WorkDay> {
        val profileId = activeProfileId.first()
        return dao.getAll(profileId).map(WorkDayEntity::toDomain)
    }

    override suspend fun upsert(day: WorkDay) {
        val profileId = activeProfileId.first()
        dao.upsert(day.toEntity(profileId))
    }

    override suspend fun delete(date: LocalDate) {
        val profileId = activeProfileId.first()
        dao.deleteByDate(profileId, date.toString())
    }

    override suspend fun insertMissing(days: List<WorkDay>): List<LocalDate> {
        val profileId = activeProfileId.first()
        return dao.insertMissing(profileId, days.map { day -> day.toEntity(profileId) })
            .map(LocalDate::parse)
    }

    override suspend fun deleteAll(dates: List<LocalDate>) {
        val profileId = activeProfileId.first()
        dao.deleteByDates(profileId, dates.map(LocalDate::toString))
    }

    override suspend fun replaceAll(days: List<WorkDay>) {
        val profileId = activeProfileId.first()
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
