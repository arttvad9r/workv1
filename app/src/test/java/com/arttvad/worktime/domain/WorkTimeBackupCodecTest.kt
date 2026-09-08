package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.backup.BackupPayment
import com.arttvad.worktime.domain.backup.WorkTimeBackup
import com.arttvad.worktime.domain.backup.WorkTimeBackupCodec
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.nio.ByteBuffer
import java.time.LocalDate
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkTimeBackupCodecTest {
    @Test
    fun roundTripIsDeterministicAndPreservesAllSupportedFields() {
        val backup = WorkTimeBackup(
            payment = BackupPayment(
                hourlyRateMinor = 1_500L,
                currencyCode = "eur",
            ),
            days = listOf(
                WorkDay(
                    date = LocalDate.of(2026, 9, 2),
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "Отпуск & заметка",
                    updatedAtEpochMillis = 2L,
                    type = WorkDayType.VACATION,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 9, 1),
                    workedMinutes = 480,
                    overtimeMinutes = 60,
                    note = "Работа",
                    updatedAtEpochMillis = 1L,
                    hourlyRateOverrideMinor = 2_000L,
                ),
            ),
        )

        val first = WorkTimeBackupCodec.encode(backup)
        val decoded = WorkTimeBackupCodec.decode(first)
        val second = WorkTimeBackupCodec.encode(decoded)

        assertArrayEquals(first, second)
        assertEquals("EUR", decoded.payment.currencyCode)
        assertEquals(1_500L, decoded.payment.hourlyRateMinor)
        assertEquals(listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2)), decoded.days.map { it.date })
        assertEquals(2_000L, decoded.days.first().hourlyRateOverrideMinor)
        assertEquals(WorkDayType.VACATION, decoded.days.last().type)
        assertEquals("Отпуск & заметка", decoded.days.last().note)
    }

    @Test
    fun decodeRejectsUnsupportedVersionBeforeReturningAnyData() {
        val bytes = WorkTimeBackupCodec.encode(
            WorkTimeBackup(
                payment = BackupPayment(null, "EUR"),
                days = emptyList(),
            ),
        ).copyOf()
        ByteBuffer.wrap(bytes).putInt(4, 99)

        assertThrows(IllegalArgumentException::class.java) {
            WorkTimeBackupCodec.decode(bytes)
        }
    }

    @Test
    fun encodeRejectsDuplicateDates() {
        val date = LocalDate.of(2026, 9, 1)
        val day = WorkDay(
            date = date,
            workedMinutes = 480,
            overtimeMinutes = 0,
            note = "",
            updatedAtEpochMillis = 1L,
        )

        assertThrows(IllegalArgumentException::class.java) {
            WorkTimeBackupCodec.encode(
                WorkTimeBackup(
                    payment = BackupPayment(null, "EUR"),
                    days = listOf(day, day.copy(updatedAtEpochMillis = 2L)),
                ),
            )
        }
    }
}
