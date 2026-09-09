package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

internal enum class CalendarEntryFilter(val type: WorkDayType?) {
    ALL(null),
    WORK(WorkDayType.WORK),
    DAY_OFF(WorkDayType.DAY_OFF),
    VACATION(WorkDayType.VACATION),
    SICK(WorkDayType.SICK),
}

internal fun filterCalendarEntries(
    entries: Collection<WorkDay>,
    query: String,
    filter: CalendarEntryFilter,
): List<WorkDay> {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    return entries.asSequence()
        .filter { entry -> filter.type == null || entry.type == filter.type }
        .filter { entry ->
            normalizedQuery.isEmpty() || entry.note.lowercase(Locale.ROOT).contains(normalizedQuery)
        }
        .sortedBy(WorkDay::date)
        .toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarSearchSheet(
    month: YearMonth,
    entries: Collection<WorkDay>,
    onDismiss: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("calendar-search-sheet"),
        dragHandle = null,
    ) {
        CalendarSearchContent(
            month = month,
            entries = entries,
            onDismiss = onDismiss,
            onSelectDay = onSelectDay,
        )
    }
}

@Composable
internal fun CalendarSearchContent(
    month: YearMonth,
    entries: Collection<WorkDay>,
    onDismiss: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(CalendarEntryFilter.ALL) }
    val results = remember(entries, query, filter) {
        filterCalendarEntries(entries, query, filter)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.calendar_search_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(
                    R.string.calendar_search_scope,
                    formatMonthTitle(month),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar-search-query"),
            singleLine = true,
            label = { Text(stringResource(R.string.calendar_search_query)) },
            placeholder = { Text(stringResource(R.string.calendar_search_query_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = stringResource(R.string.calendar_search_clear),
                        )
                    }
                }
            } else {
                null
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CalendarEntryFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(filterLabel(option)) },
                    modifier = Modifier.testTag("calendar-search-filter-${option.name.lowercase(Locale.ROOT)}"),
                )
            }
        }

        Text(
            text = stringResource(R.string.calendar_search_results_count, results.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when {
            entries.isEmpty() -> CalendarSearchEmptyText(R.string.calendar_search_empty_month)
            results.isEmpty() -> CalendarSearchEmptyText(R.string.calendar_search_no_results)
            else -> results.forEach { entry ->
                CalendarSearchResult(
                    entry = entry,
                    onClick = { onSelectDay(entry.date) },
                )
            }
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("calendar-search-close"),
        ) {
            Text(stringResource(R.string.close))
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CalendarSearchEmptyText(messageRes: Int) {
    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun CalendarSearchResult(
    entry: WorkDay,
    onClick: () -> Unit,
) {
    val detail = if (entry.type == WorkDayType.WORK) {
        stringResource(
            R.string.calendar_search_work_detail,
            formatDurationSummary(entry.workedMinutes),
        )
    } else {
        dayTypeSearchLabel(entry.type)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar-search-result-${entry.date}"),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = formatDayTitle(entry.date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.note.isNotBlank()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun filterLabel(filter: CalendarEntryFilter): String = when (filter) {
    CalendarEntryFilter.ALL -> stringResource(R.string.calendar_search_filter_all)
    CalendarEntryFilter.WORK -> stringResource(R.string.day_type_work)
    CalendarEntryFilter.DAY_OFF -> stringResource(R.string.day_type_day_off)
    CalendarEntryFilter.VACATION -> stringResource(R.string.day_type_vacation)
    CalendarEntryFilter.SICK -> stringResource(R.string.day_type_sick)
}

@Composable
private fun dayTypeSearchLabel(type: WorkDayType): String = stringResource(
    when (type) {
        WorkDayType.WORK -> R.string.day_type_work
        WorkDayType.DAY_OFF -> R.string.day_type_day_off
        WorkDayType.VACATION -> R.string.day_type_vacation
        WorkDayType.SICK -> R.string.day_type_sick
    },
)
