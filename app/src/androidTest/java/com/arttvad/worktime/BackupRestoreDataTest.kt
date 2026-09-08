package com.arttvad.worktime

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arttvad.worktime.domain.backup.BackupPayment
import com.arttvad.worktime.domain.backup.BackupProfile
import com.arttvad.worktime.domain.backup.BackupRestoreCoordinator
import com.arttvad.worktime.domain.backup.BackupSettings
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreDataTest {
    @Test
    fun backupRoundTripRestoresAllProfilesAndActiveSelection() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<WorkTimeApplication>()
        val profileBackup = application.container.profileBackupRepository
        val preferences = application.container.preferencesRepository
        val originalProfiles = profileBackup.snapshotAll()
        val originalSettings = preferences.preferences.first()

        val coordinator = BackupRestoreCoordinator(
            snapshotProfiles = profileBackup::snapshotAll,
            replaceProfiles = profileBackup::replaceAll,
            readSettings = {
                preferences.preferences.first().let { value ->
                    BackupSettings(
                        payment = BackupPayment(value.hourlyRateMinor, value.currencyCode),
                        activeProfileId = value.activeProfileId,
                    )
                }
            },
            replaceSettings = { settings ->
                preferences.updatePaymentAndProfile(
                    hourlyRateMinor = settings.payment.hourlyRateMinor,
                    currencyCode = settings.payment.currencyCode,
                    profileId = settings.activeProfileId,
                )
            },
        )

        try {
            val sameDate = LocalDate.of(2026, 1, 10)
            val backupProfiles = listOf(
                BackupProfile(
                    id = 1L,
                    name = "Основная работа",
                    createdAtEpochMillis = 0L,
                    days = listOf(
                        WorkDay(
                            date = sameDate,
                            workedMinutes = 480,
                            overtimeMinutes = 60,
                            note = "primary",
                            updatedAtEpochMillis = 10L,
                            hourlyRateOverrideMinor = 2_000L,
                        ),
                    ),
                ),
                BackupProfile(
                    id = 2L,
                    name = "Подработка",
                    createdAtEpochMillis = 20L,
                    days = listOf(
                        WorkDay(
                            date = sameDate,
                            workedMinutes = 0,
                            overtimeMinutes = 0,
                            note = "vacation",
                            updatedAtEpochMillis = 11L,
                            type = WorkDayType.VACATION,
                        ),
                    ),
                ),
            )
            profileBackup.replaceAll(backupProfiles)
            preferences.updatePaymentAndProfile(1_500L, "EUR", 2L)

            val output = ByteArrayOutputStream()
            assertEquals(2, coordinator.writeBackup(output).getOrThrow())

            profileBackup.replaceAll(
                listOf(
                    BackupProfile(
                        id = 1L,
                        name = "Temporary",
                        createdAtEpochMillis = 99L,
                        days = listOf(
                            WorkDay(
                                date = LocalDate.of(2026, 8, 1),
                                workedMinutes = 720,
                                overtimeMinutes = 0,
                                note = "replacement",
                                updatedAtEpochMillis = 12L,
                            ),
                        ),
                    ),
                ),
            )
            preferences.updatePaymentAndProfile(900L, "USD", 1L)

            val restoreResult = coordinator.restoreBackup(ByteArrayInputStream(output.toByteArray()))

            assertTrue(restoreResult.isSuccess)
            assertEquals(2, restoreResult.getOrThrow())
            assertEquals(backupProfiles, profileBackup.snapshotAll())
            val restoredSettings = preferences.preferences.first()
            assertEquals(1_500L, restoredSettings.hourlyRateMinor)
            assertEquals("EUR", restoredSettings.currencyCode)
            assertEquals(2L, restoredSettings.activeProfileId)
        } finally {
            profileBackup.replaceAll(originalProfiles)
            preferences.updatePaymentAndProfile(
                originalSettings.hourlyRateMinor,
                originalSettings.currencyCode,
                originalSettings.activeProfileId,
            )
        }
    }
}
