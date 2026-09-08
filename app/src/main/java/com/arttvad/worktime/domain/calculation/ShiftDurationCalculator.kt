package com.arttvad.worktime.domain.calculation

object ShiftDurationCalculator {
    private const val MinutesPerDay = 24 * 60

    fun calculate(
        start: String,
        end: String,
        breakMinutes: Int,
    ): Int? {
        val startMinutes = parseClockMinutes(start) ?: return null
        val endMinutes = parseClockMinutes(end) ?: return null
        if (startMinutes == endMinutes) return null
        if (breakMinutes < 0) return null

        val grossMinutes = if (endMinutes > startMinutes) {
            endMinutes - startMinutes
        } else {
            MinutesPerDay - startMinutes + endMinutes
        }
        if (breakMinutes >= grossMinutes) return null

        return grossMinutes - breakMinutes
    }

    private fun parseClockMinutes(value: String): Int? {
        val parts = value.trim().split(':')
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        if (hours !in 0..23 || minutes !in 0..59) return null
        return hours * 60 + minutes
    }
}
