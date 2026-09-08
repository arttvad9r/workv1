package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.domain.calendar.MonthGridBuilder
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun MonthCalendar(
    month: YearMonth,
    entries: Map<LocalDate, WorkDay>,
    selectedDate: LocalDate?,
    onDaySelected: (LocalDate) -> Unit,
) {
    val weekdayNames = stringArrayResource(R.array.weekday_short)
    val cells = remember(month) { MonthGridBuilder.build(month) }
    val today = remember { LocalDate.now() }
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val cellHeight = if (largeFont) 80.dp else 50.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayNames.forEach { weekday ->
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                )
            }
        }

        repeat(6) { weekIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { dayIndex ->
                    val cell = cells[weekIndex * 7 + dayIndex]
                    if (!cell.isCurrentMonth) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight),
                        )
                    } else {
                        DayCell(
                            date = cell.date,
                            entry = entries[cell.date],
                            isToday = cell.date == today,
                            isSelected = cell.date == selectedDate,
                            onClick = { onDaySelected(cell.date) },
                            height = cellHeight,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    entry: WorkDay?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val worked = entry?.workedMinutes ?: 0
    val hours = worked / 60
    val minutes = worked % 60
    val hoursDescription = if (hours > 0) {
        pluralStringResource(R.plurals.hours_accessibility, hours, hours)
    } else ""
    val minutesDescription = if (minutes > 0) {
        pluralStringResource(R.plurals.minutes_accessibility, minutes, minutes)
    } else ""
    val durationDescription = listOf(hoursDescription, minutesDescription)
        .filter(String::isNotBlank)
        .joinToString(" ")
    val typeDescription = entry?.takeIf { it.type != WorkDayType.WORK }?.let { dayTypeLabel(it.type) }.orEmpty()
    val description = listOf(
        formatDayTitle(date),
        typeDescription,
        durationDescription,
        if (entry != null) stringResource(R.string.day_has_entry) else "",
        if (isToday) stringResource(R.string.day_today) else "",
    ).filter(String::isNotBlank).joinToString(", ")

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        entry != null -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surface
    }
    val border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

    Surface(
        onClick = onClick,
        modifier = modifier
            .padding(2.dp)
            .height(height)
            .testTag("day-$date")
            .semantics {
                contentDescription = description
                selected = isSelected
            },
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = border,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
            entry?.let { day ->
                Text(
                    text = if (day.type == WorkDayType.WORK) {
                        formatDurationShort(day.workedMinutes)
                    } else {
                        dayTypeShortLabel(day.type)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day.type == WorkDayType.WORK) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun dayTypeLabel(type: WorkDayType): String = stringResource(
    when (type) {
        WorkDayType.WORK -> R.string.day_type_work
        WorkDayType.DAY_OFF -> R.string.day_type_day_off
        WorkDayType.VACATION -> R.string.day_type_vacation
        WorkDayType.SICK -> R.string.day_type_sick
    },
)

@Composable
private fun dayTypeShortLabel(type: WorkDayType): String = stringResource(
    when (type) {
        WorkDayType.WORK -> R.string.day_type_work
        WorkDayType.DAY_OFF -> R.string.day_type_day_off_short
        WorkDayType.VACATION -> R.string.day_type_vacation_short
        WorkDayType.SICK -> R.string.day_type_sick_short
    },
)
