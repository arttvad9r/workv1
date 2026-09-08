package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.domain.calculation.DetailedMonthStatistics
import com.arttvad.worktime.domain.calculation.MoneyFormatter
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthStatisticsSheet(
    month: YearMonth,
    statistics: DetailedMonthStatistics,
    preferences: WorkPreferences,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },
    ) {
        MonthStatisticsContent(
            month = month,
            statistics = statistics,
            preferences = preferences,
            onDismiss = onDismiss,
            onExportCsv = onExportCsv,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        )
    }
}

@Composable
fun MonthStatisticsContent(
    month: YearMonth,
    statistics: DetailedMonthStatistics,
    preferences: WorkPreferences,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val earned = remember(statistics.earningsMinor, preferences.currencyCode) {
        runCatching {
            MoneyFormatter.formatCurrency(statistics.earningsMinor, preferences.currencyCode)
        }.getOrDefault("—")
    }

    Column(
        modifier = modifier
            .testTag("month-statistics")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.statistics_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = formatMonthTitle(month),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("statistics-close"),
            ) {
                Text(stringResource(R.string.close))
            }
        }

        StatisticsSection(
            title = stringResource(R.string.statistics_time),
            rows = listOf(
                StatisticRow(
                    tag = "statistics-worked",
                    label = stringResource(R.string.worked),
                    value = formatDurationSummary(statistics.workedMinutes),
                ),
                StatisticRow(
                    tag = "statistics-overtime",
                    label = stringResource(R.string.overtime),
                    value = formatDurationSummary(statistics.overtimeMinutes),
                ),
                StatisticRow(
                    tag = "statistics-average-shift",
                    label = stringResource(R.string.statistics_average_shift),
                    value = formatDurationSummary(statistics.averageWorkedMinutes),
                ),
                StatisticRow(
                    tag = "statistics-longest-shift",
                    label = stringResource(R.string.statistics_longest_shift),
                    value = formatDurationSummary(statistics.longestWorkedMinutes),
                ),
                StatisticRow(
                    tag = "statistics-earned",
                    label = stringResource(R.string.earned),
                    value = earned,
                ),
            ),
        )

        StatisticsSection(
            title = stringResource(R.string.statistics_days),
            rows = listOf(
                StatisticRow(
                    tag = "statistics-work-days",
                    label = stringResource(R.string.day_type_work),
                    value = statistics.workDays.toString(),
                ),
                StatisticRow(
                    tag = "statistics-days-off",
                    label = stringResource(R.string.day_type_day_off),
                    value = statistics.daysOff.toString(),
                ),
                StatisticRow(
                    tag = "statistics-vacation-days",
                    label = stringResource(R.string.day_type_vacation),
                    value = statistics.vacationDays.toString(),
                ),
                StatisticRow(
                    tag = "statistics-sick-days",
                    label = stringResource(R.string.day_type_sick),
                    value = statistics.sickDays.toString(),
                ),
            ),
        )

        OutlinedButton(
            onClick = onExportCsv,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("statistics-export-csv"),
        ) {
            Text(stringResource(R.string.export_csv))
        }

        Text(
            text = stringResource(R.string.statistics_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class StatisticRow(
    val tag: String,
    val label: String,
    val value: String,
)

@Composable
private fun StatisticsSection(
    title: String,
    rows: List<StatisticRow>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(row.tag),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = row.value,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("${row.tag}-value"),
                )
            }
            if (index != rows.lastIndex) HorizontalDivider()
        }
    }
}
