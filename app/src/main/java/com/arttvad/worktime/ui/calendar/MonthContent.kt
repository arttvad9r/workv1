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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.domain.calculation.MoneyFormatter
import com.arttvad.worktime.domain.model.MonthSummary
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@Composable
internal fun MonthContent(
    uiState: CalendarUiState,
    pagerState: PagerState,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val displayedMonth = monthForPage(pagerState.currentPage)

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
        )

        MonthSummaryCard(uiState.summary, uiState.preferences)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 370.dp, max = 390.dp),
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
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
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
        Text(
            text = formatMonthTitle(month),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.next_month),
            )
        }
    }
}

@Composable
private fun MonthSummaryCard(
    summary: MonthSummary,
    preferences: WorkPreferences,
) {
    val earnedText = remember(summary.earningsMinor, preferences.currencyCode) {
        runCatching {
            MoneyFormatter.formatCurrency(summary.earningsMinor, preferences.currencyCode)
        }.getOrDefault("—")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
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

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
