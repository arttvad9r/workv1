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

private const val BackupMagic = 0x5754424B
private const val LegacyBackupVersion = 1
private const val BackupVersion = 2
private const val LegacyDefaultProfileId = 1L
private const val LegacyDefaultProfileName = "Основная работа"
private const val MaxBackupProfiles = 1_000
private const val MaxBackupDays = 100_000
private const val MaxProfileNameLength = 40
private const val MaxStringBytes = 1_048_576

data class BackupPayment(
    val hourlyRateMinor: Long?,
    val currencyCode: String,
)

data class BackupProfile(
    val id: Long,
    val name: String,
    val createdAtEpochMillis: Long,
    val days: List<WorkDay>,
)

data class WorkTimeBackup(
    val payment: BackupPayment,
    val activeProfileId: Long,
    val profiles: List<BackupProfile>,
) {
    val totalDays: Int get() = profiles.sumOf { profile -> profile.days.size }
}

object WorkTimeBackupCodec {
    fun encode(backup: WorkTimeBackup): ByteArray = ByteArrayOutputStream().use { output ->
        write(output, backup)
        output.toByteArray()
    }

    fun decode(bytes: ByteArray): WorkTimeBackup = ByteArrayInputStream(bytes).use(::read)

    fun write(output: OutputStream, backup: WorkTimeBackup) {
        validate(backup)
        val data = DataOutputStream(output)
        data.writeInt(BackupMagic)
        data.writeInt(BackupVersion)
        writePayment(data, backup.payment)
        data.writeLong(backup.activeProfileId)
        val sortedProfiles = backup.profiles.sortedBy(BackupProfile::id)
        data.writeInt(sortedProfiles.size)
        sortedProfiles.forEach { profile ->
            data.writeLong(profile.id)
            writeString(data, profile.name)
            data.writeLong(profile.createdAtEpochMillis)
            val sortedDays = profile.days.sortedBy(WorkDay::date)
            data.writeInt(sortedDays.size)
            sortedDays.forEach { day -> writeDay(data, day) }
        }
        data.flush()
    }

    fun read(input: InputStream): WorkTimeBackup {
        val data = DataInputStream(input)
        require(data.readInt() == BackupMagic) { "Invalid WorkTime backup header" }
        val version = data.readInt()
        val backup = when (version) {
            LegacyBackupVersion -> readLegacyV1(data)
            BackupVersion -> readV2(data)
            else -> throw IllegalArgumentException("Unsupported WorkTime backup version: $version")
        }
        require(data.read() == -1) { "Unexpected trailing backup data" }
        return backup.also(::validate)
    }

    private fun readLegacyV1(input: DataInputStream): WorkTimeBackup {
        val payment = readPayment(input)
        val count = input.readInt()
        require(count in 0..MaxBackupDays) { "Invalid work-day count" }
        val days = ArrayList<WorkDay>(count)
        repeat(count) { days += readDay(input) }
        return WorkTimeBackup(
            payment = payment,
            activeProfileId = LegacyDefaultProfileId,
            profiles = listOf(
                BackupProfile(
                    id = LegacyDefaultProfileId,
                    name = LegacyDefaultProfileName,
                    createdAtEpochMillis = 0L,
                    days = days,
                ),
            ),
        )
    }

    private fun readV2(input: DataInputStream): WorkTimeBackup {
        val payment = readPayment(input)
        val activeProfileId = input.readLong()
        val profileCount = input.readInt()
        require(profileCount in 1..MaxBackupProfiles) { "Invalid work-profile count" }
        var totalDays = 0
        val profiles = ArrayList<BackupProfile>(profileCount)
        repeat(profileCount) {
            val id = input.readLong()
            val name = readString(input)
            val createdAtEpochMillis = input.readLong()
            val dayCount = input.readInt()
            require(dayCount in 0..MaxBackupDays) { "Invalid work-day count" }
            totalDays += dayCount
            require(totalDays <= MaxBackupDays) { "Too many work-day records" }
            val days = ArrayList<WorkDay>(dayCount)
            repeat(dayCount) { days += readDay(input) }
            profiles += BackupProfile(
                id = id,
                name = name,
                createdAtEpochMillis = createdAtEpochMillis,
                days = days,
            )
        }
        return WorkTimeBackup(
            payment = payment,
            activeProfileId = activeProfileId,
            profiles = profiles,
        )
    }

    private fun writePayment(output: DataOutputStream, payment: BackupPayment) {
        writeString(output, payment.currencyCode.uppercase(Locale.ROOT))
        writeNullableLong(output, payment.hourlyRateMinor)
    }

    private fun readPayment(input: DataInputStream): BackupPayment = BackupPayment(
        currencyCode = readString(input).uppercase(Locale.ROOT),
        hourlyRateMinor = readNullableLong(input),
    )

    private fun writeDay(output: DataOutputStream, day: WorkDay) {
        writeString(output, day.date.toString())
        writeString(output, day.type.name)
        output.writeInt(day.workedMinutes)
        output.writeInt(day.overtimeMinutes)
        writeNullableLong(output, day.hourlyRateOverrideMinor)
        output.writeLong(day.updatedAtEpochMillis)
        writeString(output, day.note)
    }

    private fun readDay(input: DataInputStream): WorkDay {
        val date = LocalDate.parse(readString(input))
        val type = WorkDayType.valueOf(readString(input))
        val workedMinutes = input.readInt()
        val overtimeMinutes = input.readInt()
        val overrideMinor = readNullableLong(input)
        val updatedAtEpochMillis = input.readLong()
        val note = readString(input)
        return WorkDay(
            date = date,
            workedMinutes = workedMinutes,
            overtimeMinutes = overtimeMinutes,
            note = note,
            updatedAtEpochMillis = updatedAtEpochMillis,
            hourlyRateOverrideMinor = overrideMinor,
            type = type,
        )
    }

    private fun validate(backup: WorkTimeBackup) {
        require(backup.profiles.size in 1..MaxBackupProfiles) { "Invalid work-profile count" }
        require(backup.activeProfileId > 0L) { "Invalid active profile id" }
        val currencyCode = backup.payment.currencyCode.uppercase(Locale.ROOT)
        require(currencyCode.length == 3) { "Invalid currency code" }
        Currency.getInstance(currencyCode)
        require(backup.payment.hourlyRateMinor == null || backup.payment.hourlyRateMinor >= 0) {
            "Invalid base hourly rate"
        }
        val profileIds = HashSet<Long>(backup.profiles.size)
        var totalDays = 0
        backup.profiles.forEach { profile ->
            require(profile.id > 0L) { "Invalid work-profile id" }
            require(profileIds.add(profile.id)) { "Duplicate work-profile id: ${profile.id}" }
            require(profile.name == profile.name.trim() && profile.name.isNotBlank()) {
                "Invalid work-profile name"
            }
            require(profile.name.length <= MaxProfileNameLength) { "Work-profile name is too long" }
            require(profile.createdAtEpochMillis >= 0L) { "Invalid work-profile timestamp" }
            totalDays += profile.days.size
            require(totalDays <= MaxBackupDays) { "Too many work-day records" }
            validateDays(profile.days, profile.id)
        }
        require(backup.activeProfileId in profileIds) { "Active profile is missing from backup" }
    }

    private fun validateDays(days: List<WorkDay>, profileId: Long) {
        val dates = HashSet<LocalDate>(days.size)
        days.forEach { day ->
            require(dates.add(day.date)) { "Duplicate work-day date in profile $profileId: ${day.date}" }
            require(WorkDayValidator.validate(day.type, day.workedMinutes, day.overtimeMinutes) == null) {
                "Invalid work-day duration: ${day.date}"
            }
            require(day.hourlyRateOverrideMinor == null || day.hourlyRateOverrideMinor >= 0) {
                "Invalid day hourly rate: ${day.date}"
            }
            require(day.type == WorkDayType.WORK || day.hourlyRateOverrideMinor == null) {
                "Non-work day has an hourly-rate override: ${day.date}"
            }
            require(day.updatedAtEpochMillis >= 0L) { "Invalid update timestamp: ${day.date}" }
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
