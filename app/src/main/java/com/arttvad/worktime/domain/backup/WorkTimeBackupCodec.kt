package com.arttvad.worktime.domain.backup

import com.arttvad.worktime.domain.calculation.WorkDayValidator
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

private const val BackupMagic = 0x5754424B // WTBK
private const val BackupVersion = 1
private const val MaxBackupDays = 100_000
private const val MaxStringBytes = 1_048_576

data class BackupPayment(
    val hourlyRateMinor: Long?,
    val currencyCode: String,
)

data class WorkTimeBackup(
    val payment: BackupPayment,
    val days: List<WorkDay>,
)

object WorkTimeBackupCodec {
    fun encode(backup: WorkTimeBackup): ByteArray = ByteArrayOutputStream().use { output ->
        write(output, backup)
        output.toByteArray()
    }

    fun decode(bytes: ByteArray): WorkTimeBackup =
        ByteArrayInputStream(bytes).use(::read)

    fun write(output: OutputStream, backup: WorkTimeBackup) {
        validate(backup)
        val normalizedCurrency = backup.payment.currencyCode.uppercase(Locale.ROOT)
        val data = DataOutputStream(output)
        data.writeInt(BackupMagic)
        data.writeInt(BackupVersion)
        writeString(data, normalizedCurrency)
        writeNullableLong(data, backup.payment.hourlyRateMinor)

        val sortedDays = backup.days.sortedBy(WorkDay::date)
        data.writeInt(sortedDays.size)
        sortedDays.forEach { day ->
            writeString(data, day.date.toString())
            writeString(data, day.type.name)
            data.writeInt(day.workedMinutes)
            data.writeInt(day.overtimeMinutes)
            writeNullableLong(data, day.hourlyRateOverrideMinor)
            data.writeLong(day.updatedAtEpochMillis)
            writeString(data, day.note)
        }
        data.flush()
    }

    fun read(input: InputStream): WorkTimeBackup {
        val data = DataInputStream(input)
        require(data.readInt() == BackupMagic) { "Invalid WorkTime backup header" }
        val version = data.readInt()
        require(version == BackupVersion) { "Unsupported WorkTime backup version: $version" }

        val currencyCode = readString(data).uppercase(Locale.ROOT)
        val hourlyRateMinor = readNullableLong(data)
        val count = data.readInt()
        require(count in 0..MaxBackupDays) { "Invalid work-day count" }

        val days = ArrayList<WorkDay>(count)
        repeat(count) {
            val date = LocalDate.parse(readString(data))
            val type = WorkDayType.valueOf(readString(data))
            val workedMinutes = data.readInt()
            val overtimeMinutes = data.readInt()
            val overrideMinor = readNullableLong(data)
            val updatedAtEpochMillis = data.readLong()
            val note = readString(data)
            days += WorkDay(
                date = date,
                workedMinutes = workedMinutes,
                overtimeMinutes = overtimeMinutes,
                note = note,
                updatedAtEpochMillis = updatedAtEpochMillis,
                hourlyRateOverrideMinor = overrideMinor,
                type = type,
            )
        }
        require(data.read() == -1) { "Unexpected trailing backup data" }

        return WorkTimeBackup(
            payment = BackupPayment(
                hourlyRateMinor = hourlyRateMinor,
                currencyCode = currencyCode,
            ),
            days = days,
        ).also(::validate)
    }

    private fun validate(backup: WorkTimeBackup) {
        require(backup.days.size <= MaxBackupDays) { "Too many work-day records" }
        val currencyCode = backup.payment.currencyCode.uppercase(Locale.ROOT)
        require(currencyCode.length == 3) { "Invalid currency code" }
        Currency.getInstance(currencyCode)
        require(backup.payment.hourlyRateMinor == null || backup.payment.hourlyRateMinor >= 0) {
            "Invalid base hourly rate"
        }

        val dates = HashSet<LocalDate>(backup.days.size)
        backup.days.forEach { day ->
            require(dates.add(day.date)) { "Duplicate work-day date: ${day.date}" }
            require(WorkDayValidator.validate(day.type, day.workedMinutes, day.overtimeMinutes) == null) {
                "Invalid work-day duration: ${day.date}"
            }
            require(day.hourlyRateOverrideMinor == null || day.hourlyRateOverrideMinor >= 0) {
                "Invalid day hourly rate: ${day.date}"
            }
            require(day.type == WorkDayType.WORK || day.hourlyRateOverrideMinor == null) {
                "Non-work day has an hourly-rate override: ${day.date}"
            }
            require(day.updatedAtEpochMillis >= 0) { "Invalid update timestamp: ${day.date}" }
            require(day.note.toByteArray(StandardCharsets.UTF_8).size <= MaxStringBytes) {
                "Work-day note is too large: ${day.date}"
            }
        }
    }

    private fun writeNullableLong(output: DataOutputStream, value: Long?) {
        output.writeBoolean(value != null)
        if (value != null) output.writeLong(value)
    }

    private fun readNullableLong(input: DataInputStream): Long? =
        if (input.readBoolean()) input.readLong() else null

    private fun writeString(output: DataOutputStream, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MaxStringBytes) { "Backup string is too large" }
        output.writeInt(bytes.size)
        output.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val length = input.readInt()
        require(length in 0..MaxStringBytes) { "Invalid backup string length" }
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
