package com.arttvad.worktime.data.repository

import com.arttvad.worktime.data.local.DEFAULT_PROFILE_ID
import com.arttvad.worktime.data.local.DEFAULT_PROFILE_NAME
import com.arttvad.worktime.data.local.WorkProfileDao
import com.arttvad.worktime.data.local.WorkProfileEntity
import com.arttvad.worktime.domain.model.WorkProfile
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface WorkProfileRepository {
    fun observeProfiles(): Flow<List<WorkProfile>>
    suspend fun create(name: String): WorkProfile
    suspend fun updatePayment(profileId: Long, hourlyRateMinor: Long?, currencyCode: String)
}

class RoomWorkProfileRepository(
    private val dao: WorkProfileDao,
) : WorkProfileRepository {
    override fun observeProfiles(): Flow<List<WorkProfile>> = flow {
        ensureDefaultProfile()
        emitAll(
            dao.observeAll().map { profiles -> profiles.map(WorkProfileEntity::toDomain) },
        )
    }

    override suspend fun create(name: String): WorkProfile {
        val normalized = name.trim()
        require(normalized.isNotBlank()) { "Profile name must not be blank" }
        require(normalized.length <= 40) { "Profile name must not exceed 40 characters" }
        ensureDefaultProfile()
        val nextId = (dao.maxId() ?: DEFAULT_PROFILE_ID) + 1L
        val entity = WorkProfileEntity(
            id = nextId,
            name = normalized,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun updatePayment(
        profileId: Long,
        hourlyRateMinor: Long?,
        currencyCode: String,
    ) {
        require(profileId > 0L) { "Profile id must be positive" }
        require(hourlyRateMinor == null || hourlyRateMinor >= 0L) { "Hourly rate must be non-negative" }
        val normalizedCurrency = currencyCode.uppercase(Locale.ROOT)
        require(normalizedCurrency.length == 3) { "Invalid currency code" }
        Currency.getInstance(normalizedCurrency)
        check(dao.updatePayment(profileId, hourlyRateMinor, normalizedCurrency) == 1) {
            "Unknown work profile"
        }
    }

    private suspend fun ensureDefaultProfile() {
        if (dao.getById(DEFAULT_PROFILE_ID) == null) {
            dao.upsert(
                WorkProfileEntity(
                    id = DEFAULT_PROFILE_ID,
                    name = DEFAULT_PROFILE_NAME,
                    createdAtEpochMillis = 0L,
                ),
            )
        }
    }
}

private fun WorkProfileEntity.toDomain() = WorkProfile(
    id = id,
    name = name,
    hourlyRateMinor = hourlyRateMinor,
    currencyCode = currencyCode,
)
