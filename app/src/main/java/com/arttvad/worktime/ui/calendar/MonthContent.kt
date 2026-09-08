package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.domain.calculation.MoneyFormatter
import com.arttvad.worktime.domain.model.MonthSummary
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.launch

private const val LargeFontScaleThreshold = 1.5f

@Composable
internal fun MonthContent(
    uiState: CalendarUiState,
    pagerState: PagerState,
    onDaySelected: (LocalDate) -> Unit,
    onStatisticsRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val displayedMonth = monthForPage(pagerState.currentPage)
    val currentMonth = YearMonth.now()
    val today = LocalDate.now()
    val largeFont = LocalDensity.current.fontScale >= LargeFontScaleThreshold
    val pagerMinHeight = if (largeFont) 540.dp else 370.dp
    val pagerMaxHeight = if (largeFont) 570.dp else 390.dp
    var dateShortcutOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MonthHeader(
            month = displayedMonth,
            canGoPrevious = pagerState.currentPage > 0,
            canGoNext = pagerState.currentPage < PagerMonthCount - 1,
            showToday = displayedMonth != currentMonth,
            onPrevious = {
                if (pagerState.currentPage > 0) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }
            },
            onNext = {
                if (pagerState.currentPage < PagerMonthCount - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            onToday = {
                scope.launch { pagerState.animateScrollToPage(pageForMonth(currentMonth)) }
            },
            onDateShortcut = { dateShortcutOpen = true },
        )

        MonthSummaryCard(
            summary = uiState.summary,
            preferences = uiState.preferences,
            onClick = onStatisticsRequested,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = pagerMinHeight, max = pagerMaxHeight),
            beyondViewportPageCount = 1,
        ) { page ->
            val pageMonth = monthForPage(page)
            MonthCalendar(
                month = pageMonth,
                entries = if (pageMonth == uiState.visibleMonth) uiState.entries else emptyMap(),
                selectedDate = uiState.selectedDate,
                onDaySelected = onDaySelected,
            )
        }

        if (uiState.entries.isEmpty() && displayedMonth == uiState.visibleMonth) {
            Text(
                text = stringResource(R.string.tap_day_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (dateShortcutOpen) {
        val initialDate = uiState.selectedDate
            ?.takeIf { YearMonth.from(it) == displayedMonth }
            ?: if (YearMonth.from(today) == displayedMonth) today else displayedMonth.atDay(1)
        DateShortcutDialog(
            initialDate = initialDate,
            onDismiss = { dateShortcutOpen = false },
            onDateSelected = { date ->
                dateShortcutOpen = false
                scope.launch {
                    pagerState.scrollToPage(pageForMonth(YearMonth.from(date)))
                    onDaySelected(date)
                }
            },
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    showToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDateShortcut: () -> Unit,
) {
    val monthTitle = formatMonthTitle(month)
    val dateShortcutDescription = stringResource(R.string.date_shortcut_description, monthTitle)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(R.string.previous_month),
            )
        }
        TextButton(
            onClick = onDateShortcut,
            modifier = Modifier
                .weight(1f)
                .testTag("month-date-shortcut")
                .semantics { contentDescription = dateShortcutDescription },
        ) {
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
        if (showToday) {
            IconButton(onClick = onToday) {
                Icon(
                    imageVector = Icons.Rounded.Today,
                    contentDescription = stringResource(R.string.today),
                )
            }
        }
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.next_month),
            )
        }
    }
}

@Composable
internal fun DateShortcutDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
        initialDisplayedMonthMillis = initialDate.withDayOfMonth(1).toUtcMillis(),
        yearRange = PagerStartMonth.year..monthForPage(PagerMonthCount - 1).year,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { selectedMillis ->
                        onDateSelected(selectedMillis.toUtcLocalDate())
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
                modifier = Modifier.testTag("date-shortcut-confirm"),
            ) {
                Text(stringResource(R.string.date_shortcut_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("date-shortcut-cancel"),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(
            state = pickerState,
            modifier = Modifier.testTag("date-shortcut-picker"),
            title = { Text(stringResource(R.string.date_shortcut_title)) },
        )
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun MonthSummaryCard(
    summary: MonthSummary,
    preferences: WorkPreferences,
    onClick: () -> Unit,
) {
    val earnedText = remember(summary.earningsMinor, preferences.currencyCode) {
        runCatching {
            MoneyFormatter.formatCurrency(summary.earningsMinor, preferences.currencyCode)
        }.getOrDefault("—")
    }
    val largeFont = LocalDensity.current.fontScale >= LargeFontScaleThreshold

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("month-summary"),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        if (largeFont) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    value = formatDurationSummary(summary.workedMinutes),
                    label = stringResource(R.string.worked),
                    modifier = Modifier.fillMaxWidth(),
                )
                SummaryMetric(
                    value = earnedText,
                    label = stringResource(R.string.earned),
                    modifier = Modifier.fillMaxWidth(),
                )
                SummaryMetric(
                    value = formatDurationSummary(summary.overtimeMinutes),
                    label = stringResource(R.string.overtime),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummaryMetric(
                    value = formatDurationSummary(summary.workedMinutes),
                    label = stringResource(R.string.worked),
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(modifier = Modifier.height(42.dp))
                SummaryMetric(
                    value = earnedText,
                    label = stringResource(R.string.earned),
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(modifier = Modifier.height(42.dp))
                SummaryMetric(
                    value = formatDurationSummary(summary.overtimeMinutes),
                    label = stringResource(R.string.overtime),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val largeFont = LocalDensity.current.fontScale >= LargeFontScaleThreshold

    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (largeFont) 2 else 1,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (largeFont) 2 else 1,
            textAlign = TextAlign.Center,
        )
    }
}
