package com.arttvad.worktime.ui.calendar

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDurationShort(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        totalMinutes == 0 -> "0 ч"
        minutes == 0 -> "$hours ч"
        hours == 0 -> "$minutes мин"
        else -> "%d:%02d".format(Locale.ROOT, hours, minutes)
    }
}

fun formatDurationSummary(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        minutes == 0 -> "$hours ч"
        hours == 0 -> "$minutes мин"
        else -> "$hours ч $minutes мин"
    }
}

fun formatMonthTitle(month: YearMonth, locale: Locale = Locale.getDefault()): String =
    month.atDay(1)
        .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        .replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(locale) else character.toString()
        }

fun formatDayTitle(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
        .replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(locale) else character.toString()
        }
