package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.backup.BackupPayment
import com.arttvad.worktime.domain.backup.BackupProfile
import com.arttvad.worktime.domain.backup.WorkTimeBackup
import com.arttvad.worktime.domain.backup.WorkTimeBackupCodec
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkTimeBackupCodecTest {
    @Test
    fun v2RoundTripIsDeterministicAndPreservesProfiles() {
        val sameDate = LocalDate.of(2026, 9, 1)
        val backup = WorkTimeBackup(
            payment = BackupPayment(1_500L, "eur"),
            activeProfileId = 2L,
            profiles = listOf(
                BackupProfile(
                    id = 2L,
                    name = "Подработка",
                    createdAtEpochMillis = 2L,
                    days = listOf(workDay(sameDate, 240, "secondary")),
                ),
                BackupProfile(
                    id = 1L,
                    name = "Основная работа",
                    createdAtEpochMillis = 0L,
                    days = listOf(
                        WorkDay(
                            date = LocalDate.of(2026, 9, 2),
                            workedMinutes = 0,
                            overtimeMinutes = 0,
                            note = "Отпуск",
                            updatedAtEpochMillis = 3L,
                            type = WorkDayType.VACATION,
                        ),
                        workDay(sameDate, 480, "primary", 2_000L),
                    ),
                ),
            ),
        )

        val first = WorkTimeBackupCodec.encode(backup)
        val decoded = WorkTimeBackupCodec.decode(first)
        val second = WorkTimeBackupCodec.encode(decoded)

        assertArrayEquals(first, second)
        assertEquals("EUR", decoded.payment.currencyCode)
        assertEquals(2L, decoded.activeProfileId)
        assertEquals(listOf(1L, 2L), decoded.profiles.map { it.id })
        assertEquals(3, decoded.totalDays)
        assertEquals("primary", decoded.profiles.first().days.first().note)
        assertEquals("secondary", decoded.profiles.last().days.single().note)
    }

    @Test
    fun readsLegacyV1AsDefaultProfile() {
        val bytes = legacyV1Bytes()

        val decoded = WorkTimeBackupCodec.decode(bytes)

        assertEquals("EUR", decoded.payment.currencyCode)
        assertEquals(1_500L, decoded.payment.hourlyRateMinor)
        assertEquals(1L, decoded.activeProfileId)
        assertEquals(1, decoded.profiles.size)
        assertEquals("Основная работа", decoded.profiles.single().name)
        assertEquals("legacy", decoded.profiles.single().days.single().note)
    }

    @Test
    fun decodeRejectsUnsupportedVersionBeforeReturningAnyData() {
        val bytes = WorkTimeBackupCodec.encode(
            WorkTimeBackup(
                payment = BackupPayment(null, "EUR"),
                activeProfileId = 1L,
                profiles = listOf(BackupProfile(1L, "Основная работа", 0L, emptyList())),
            ),
        ).copyOf()
        ByteBuffer.wrap(bytes).putInt(4, 99)

        assertThrows(IllegalArgumentException::class.java) {
            WorkTimeBackupCodec.decode(bytes)
        }
    }

    @Test
    fun duplicateDateIsRejectedWithinProfileButAllowedAcrossProfiles() {
        val date = LocalDate.of(2026, 9, 1)
        val day = workDay(date, 480, "one")
        assertThrows(IllegalArgumentException::class.java) {
            WorkTimeBackupCodec.encode(
                WorkTimeBackup(
                    payment = BackupPayment(null, "EUR"),
                    activeProfileId = 1L,
                    profiles = listOf(
                        BackupProfile(1L, "Основная работа", 0L, listOf(day, day.copy(note = "duplicate"))),
                    ),
                ),
            )
        }

        WorkTimeBackupCodec.encode(
            WorkTimeBackup(
                payment = BackupPayment(null, "EUR"),
                activeProfileId = 1L,
                profiles = listOf(
                    BackupProfile(1L, "Основная работа", 0L, listOf(day)),
                    BackupProfile(2L, "Подработка", 1L, listOf(day.copy(note = "other profile"))),
                ),
            ),
        )
    }

    private fun workDay(
        date: LocalDate,
        minutes: Int,
        note: String,
        override: Long? = null,
    ) = WorkDay(
        date = date,
        workedMinutes = minutes,
        overtimeMinutes = 0,
        note = note,
        updatedAtEpochMillis = 1L,
        hourlyRateOverrideMinor = override,
    )

    private fun legacyV1Bytes(): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(0x5754424B)
            output.writeInt(1)
            writeString(output, "EUR")
            output.writeBoolean(true)
            output.writeLong(1_500L)
            output.writeInt(1)
            writeString(output, "2026-09-01")
            writeString(output, "WORK")
            output.writeInt(480)
            output.writeInt(0)
            output.writeBoolean(false)
            output.writeLong(1L)
            writeString(output, "legacy")
        }
        bytes.toByteArray()
    }

    private fun writeString(output: DataOutputStream, value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8)
        output.writeInt(encoded.size)
        output.write(encoded)
    }
}
