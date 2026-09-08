package com.arttvad.worktime.domain.calculation

object ShiftTimerCalculator {
    const val MAX_TRACKED_MINUTES: Int = 24 * 60

    fun elapsedMinutes(
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long,
    ): Int? {
        if (startedAtEpochMillis < 0L || endedAtEpochMillis <= startedAtEpochMillis) return null

        val elapsedMinutes = (endedAtEpochMillis - startedAtEpochMillis) / 60_000L
        if (elapsedMinutes !in 1L..MAX_TRACKED_MINUTES.toLong()) return null

        return elapsedMinutes.toInt()
    }
}
