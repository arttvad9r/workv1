package com.arttvad.worktime.domain.backup

import java.io.InputStream
import java.io.OutputStream

data class BackupSettings(
    val payment: BackupPayment,
    val activeProfileId: Long,
)

class BackupRestoreCoordinator(
    private val snapshotProfiles: suspend () -> List<BackupProfile>,
    private val replaceProfiles: suspend (List<BackupProfile>) -> Unit,
    private val readSettings: suspend () -> BackupSettings,
    private val replaceSettings: suspend (BackupSettings) -> Unit,
) {
    suspend fun writeBackup(output: OutputStream): Result<Int> = runCatching {
        val profiles = snapshotProfiles()
        val settings = readSettings()
        val backup = WorkTimeBackup(
            payment = settings.payment,
            activeProfileId = settings.activeProfileId,
            profiles = profiles,
        )
        WorkTimeBackupCodec.write(output, backup)
        backup.totalDays
    }

    suspend fun restoreBackup(input: InputStream): Result<Int> {
        val backup = runCatching { WorkTimeBackupCodec.read(input) }
            .getOrElse { return Result.failure(it) }
        val previousProfiles = runCatching { snapshotProfiles() }
            .getOrElse { return Result.failure(it) }
        val previousSettings = runCatching { readSettings() }
            .getOrElse { return Result.failure(it) }

        return try {
            replaceProfiles(backup.profiles)
            replaceSettings(
                BackupSettings(
                    payment = backup.payment,
                    activeProfileId = backup.activeProfileId,
                ),
            )
            Result.success(backup.totalDays)
        } catch (error: Throwable) {
            runCatching { replaceSettings(previousSettings) }
                .exceptionOrNull()
                ?.let(error::addSuppressed)
            runCatching { replaceProfiles(previousProfiles) }
                .exceptionOrNull()
                ?.let(error::addSuppressed)
            Result.failure(error)
        }
    }
}
