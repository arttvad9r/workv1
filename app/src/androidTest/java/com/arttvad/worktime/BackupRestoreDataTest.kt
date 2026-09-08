package com.arttvad.worktime

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arttvad.worktime.domain.backup.BackupPayment
import com.arttvad.worktime.domain.backup.BackupRestoreCoordinator
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
    fun backupRoundTripRestoresRoomAndPaymentSettings() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<WorkTimeApplication>()
        val workDays = application.container.workDayRepository
        val preferences = application.container.preferencesRepository
        val originalDays = workDays.snapshotAll()
        val originalPayment = preferences.preferences.first()

        val coordinator = BackupRestoreCoordinator(
            snapshotDays = workDays::snapshotAll,
            replaceDays = workDays::replaceAll,
            readPayment = {
                preferences.preferences.first().let { value ->
                    BackupPayment(value.hourlyRateMinor, value.currencyCode)
                }
            },
            replacePayment = { payment ->
                preferences.updatePayment(payment.hourlyRateMinor, payment.currencyCode)
            },
        )

        try {
            val backupDays = listOf(
                WorkDay(
                    date = LocalDate.of(2026, 1, 10),
                    workedMinutes = 480,
                    overtimeMinutes = 60,
                    note = "backup-work",
                    updatedAtEpochMillis = 10L,
                    hourlyRateOverrideMinor = 2_000L,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 1, 11),
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "backup-vacation",
                    updatedAtEpochMillis = 11L,
                    type = WorkDayType.VACATION,
                ),
            )
            workDays.replaceAll(backupDays)
            preferences.updatePayment(1_500L, "EUR")

            val output = ByteArrayOutputStream()
            assertEquals(2, coordinator.writeBackup(output).getOrThrow())

            workDays.replaceAll(
                listOf(
                    WorkDay(
                        date = LocalDate.of(2026, 8, 1),
                        workedMinutes = 720,
                        overtimeMinutes = 0,
                        note = "replacement",
                        updatedAtEpochMillis = 12L,
                    ),
                ),
            )
            preferences.updatePayment(900L, "USD")

            val restoreResult = coordinator.restoreBackup(
                ByteArrayInputStream(output.toByteArray()),
            )

            assertTrue(restoreResult.isSuccess)
            assertEquals(2, restoreResult.getOrThrow())
            assertEquals(backupDays, workDays.snapshotAll())
            assertEquals(1_500L, preferences.preferences.first().hourlyRateMinor)
            assertEquals("EUR", preferences.preferences.first().currencyCode)
        } finally {
            workDays.replaceAll(originalDays)
            preferences.updatePayment(
                originalPayment.hourlyRateMinor,
                originalPayment.currencyCode,
            )
        }
    }
}
