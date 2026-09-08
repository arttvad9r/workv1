package com.arttvad.worktime.domain.backup

import com.arttvad.worktime.domain.model.WorkDay
import java.io.InputStream
import java.io.OutputStream

class BackupRestoreCoordinator(
    private val snapshotDays: suspend () -> List<WorkDay>,
    private val replaceDays: suspend (List<WorkDay>) -> Unit,
    private val readPayment: suspend () -> BackupPayment,
    private val replacePayment: suspend (BackupPayment) -> Unit,
) {
    suspend fun writeBackup(output: OutputStream): Result<Int> = runCatching {
        val days = snapshotDays()
        val payment = readPayment()
        WorkTimeBackupCodec.write(
            output = output,
            backup = WorkTimeBackup(payment = payment, days = days),
        )
        days.size
    }

    suspend fun restoreBackup(input: InputStream): Result<Int> {
        val backup = runCatching { WorkTimeBackupCodec.read(input) }
            .getOrElse { return Result.failure(it) }
        val previousDays = runCatching { snapshotDays() }
            .getOrElse { return Result.failure(it) }
        val previousPayment = runCatching { readPayment() }
            .getOrElse { return Result.failure(it) }

        return try {
            replaceDays(backup.days)
            replacePayment(backup.payment)
            Result.success(backup.days.size)
        } catch (error: Throwable) {
            runCatching { replacePayment(previousPayment) }
                .exceptionOrNull()
                ?.let(error::addSuppressed)
            runCatching { replaceDays(previousDays) }
                .exceptionOrNull()
                ?.let(error::addSuppressed)
            Result.failure(error)
        }
    }
}
