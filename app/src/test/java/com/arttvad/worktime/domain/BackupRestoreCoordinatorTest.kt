package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.backup.BackupPayment
import com.arttvad.worktime.domain.backup.BackupProfile
import com.arttvad.worktime.domain.backup.BackupRestoreCoordinator
import com.arttvad.worktime.domain.backup.BackupSettings
import com.arttvad.worktime.domain.backup.WorkTimeBackup
import com.arttvad.worktime.domain.backup.WorkTimeBackupCodec
import com.arttvad.worktime.domain.model.WorkDay
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestoreCoordinatorTest {
    @Test
    fun invalidBackupIsRejectedBeforeAnyMutation() = runBlocking {
        var replaceProfilesCalled = false
        var replaceSettingsCalled = false
        val coordinator = BackupRestoreCoordinator(
            snapshotProfiles = { profiles("previous") },
            replaceProfiles = { replaceProfilesCalled = true },
            readSettings = { settings(1_500L, "EUR", 1L) },
            replaceSettings = { replaceSettingsCalled = true },
        )

        val result = coordinator.restoreBackup(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)))

        assertTrue(result.isFailure)
        assertFalse(replaceProfilesCalled)
        assertFalse(replaceSettingsCalled)
    }

    @Test
    fun settingsFailureRollsBackProfileGraphAndSettings() = runBlocking {
        val previousProfiles = profiles("previous")
        val restoredProfiles = listOf(
            BackupProfile(1L, "Основная работа", 0L, listOf(workDay("2026-09-01", 600, "restored"))),
            BackupProfile(2L, "Подработка", 2L, listOf(workDay("2026-09-01", 240, "second"))),
        )
        val previousSettings = settings(1_500L, "EUR", 1L)
        val restoredSettings = settings(2_000L, "USD", 2L)
        var currentProfiles = previousProfiles
        var currentSettings = previousSettings
        var failNextSettingsReplacement = true

        val coordinator = BackupRestoreCoordinator(
            snapshotProfiles = { currentProfiles },
            replaceProfiles = { profiles -> currentProfiles = profiles },
            readSettings = { currentSettings },
            replaceSettings = { value ->
                if (failNextSettingsReplacement) {
                    failNextSettingsReplacement = false
                    throw IllegalStateException("simulated DataStore failure")
                }
                currentSettings = value
            },
        )
        val bytes = WorkTimeBackupCodec.encode(
            WorkTimeBackup(
                payment = restoredSettings.payment,
                activeProfileId = restoredSettings.activeProfileId,
                profiles = restoredProfiles,
            ),
        )

        val result = coordinator.restoreBackup(ByteArrayInputStream(bytes))

        assertTrue(result.isFailure)
        assertEquals(previousProfiles, currentProfiles)
        assertEquals(previousSettings, currentSettings)
    }

    @Test
    fun writeBackupUsesAllProfilesAndReturnsTotalDayCount() = runBlocking {
        val profiles = listOf(
            BackupProfile(1L, "Основная работа", 0L, listOf(workDay("2026-09-03", 720, "one"))),
            BackupProfile(2L, "Подработка", 2L, listOf(workDay("2026-09-04", 240, "two"))),
        )
        val settings = settings(1_750L, "EUR", 2L)
        val coordinator = BackupRestoreCoordinator(
            snapshotProfiles = { profiles },
            replaceProfiles = {},
            readSettings = { settings },
            replaceSettings = {},
        )
        val output = ByteArrayOutputStream()

        val result = coordinator.writeBackup(output)
        val decoded = WorkTimeBackupCodec.decode(output.toByteArray())

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow())
        assertEquals(profiles, decoded.profiles)
        assertEquals(settings.activeProfileId, decoded.activeProfileId)
        assertEquals(settings.payment, decoded.payment)
    }

    private fun profiles(note: String) = listOf(
        BackupProfile(1L, "Основная работа", 0L, listOf(workDay("2026-08-01", 480, note))),
    )

    private fun settings(rate: Long?, currency: String, profileId: Long) = BackupSettings(
        payment = BackupPayment(rate, currency),
        activeProfileId = profileId,
    )

    private fun workDay(date: String, workedMinutes: Int, note: String) = WorkDay(
        date = LocalDate.parse(date),
        workedMinutes = workedMinutes,
        overtimeMinutes = 0,
        note = note,
        updatedAtEpochMillis = 1L,
    )
}
