package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.backup.BackupPayment
import com.arttvad.worktime.domain.backup.BackupRestoreCoordinator
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
        var replaceDaysCalled = false
        var replacePaymentCalled = false
        val coordinator = BackupRestoreCoordinator(
            snapshotDays = { emptyList() },
            replaceDays = { replaceDaysCalled = true },
            readPayment = { BackupPayment(null, "EUR") },
            replacePayment = { replacePaymentCalled = true },
        )

        val result = coordinator.restoreBackup(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)))

        assertTrue(result.isFailure)
        assertFalse(replaceDaysCalled)
        assertFalse(replacePaymentCalled)
    }

    @Test
    fun paymentFailureRollsBackPreviouslyReplacedDaysAndPayment() = runBlocking {
        val previousDays = listOf(workDay(LocalDate.of(2026, 8, 1), 480))
        val restoredDays = listOf(workDay(LocalDate.of(2026, 9, 1), 600))
        val previousPayment = BackupPayment(1_500L, "EUR")
        val restoredPayment = BackupPayment(2_000L, "USD")
        var currentDays = previousDays
        var currentPayment = previousPayment
        var failNextPaymentReplacement = true

        val coordinator = BackupRestoreCoordinator(
            snapshotDays = { currentDays },
            replaceDays = { days -> currentDays = days },
            readPayment = { currentPayment },
            replacePayment = { payment ->
                if (failNextPaymentReplacement) {
                    failNextPaymentReplacement = false
                    throw IllegalStateException("simulated DataStore failure")
                }
                currentPayment = payment
            },
        )
        val bytes = WorkTimeBackupCodec.encode(
            WorkTimeBackup(
                payment = restoredPayment,
                days = restoredDays,
            ),
        )

        val result = coordinator.restoreBackup(ByteArrayInputStream(bytes))

        assertTrue(result.isFailure)
        assertEquals(previousDays, currentDays)
        assertEquals(previousPayment, currentPayment)
    }

    @Test
    fun writeBackupUsesCurrentSnapshots() = runBlocking {
        val days = listOf(workDay(LocalDate.of(2026, 9, 3), 720))
        val payment = BackupPayment(1_750L, "EUR")
        val coordinator = BackupRestoreCoordinator(
            snapshotDays = { days },
            replaceDays = {},
            readPayment = { payment },
            replacePayment = {},
        )
        val output = ByteArrayOutputStream()

        val result = coordinator.writeBackup(output)
        val decoded = WorkTimeBackupCodec.decode(output.toByteArray())

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(days, decoded.days)
        assertEquals(payment, decoded.payment)
    }

    private fun workDay(date: LocalDate, workedMinutes: Int) = WorkDay(
        date = date,
        workedMinutes = workedMinutes,
        overtimeMinutes = 0,
        note = "",
        updatedAtEpochMillis = 1L,
    )
}
