package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.domain.calculation.DetailedMonthStatistics
import com.arttvad.worktime.domain.calculation.DetailedYearStatistics
import com.arttvad.worktime.domain.calculation.MoneyFormatter
import com.arttvad.worktime.domain.calculation.YearMonthStatistics
import java.time.Year
import java.time.YearMonth

private enum class StatisticsScope {
    MONTH,
    YEAR,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthStatisticsSheet(
    month: YearMonth,
    statistics: DetailedMonthStatistics,
    yearStatistics: DetailedYearStatistics,
    preferences: WorkPreferences,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
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
            yearStatistics = yearStatistics,
            preferences = preferences,
            onDismiss = onDismiss,
            onExportCsv = onExportCsv,
            onExportPdf = onExportPdf,
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
    yearStatistics: DetailedYearStatistics = DetailedYearStatistics(Year.of(month.year)),
    preferences: WorkPreferences,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var scope by rememberSaveable { mutableStateOf(StatisticsScope.MONTH) }

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
                    text = if (scope == StatisticsScope.MONTH) {
                        formatMonthTitle(month)
                    } else {
                        yearStatistics.year.value.toString()
                    },
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

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = scope == StatisticsScope.MONTH,
                onClick = { scope = StatisticsScope.MONTH },
                label = { Text(stringResource(R.string.statistics_scope_month)) },
                modifier = Modifier.testTag("statistics-scope-month"),
            )
            FilterChip(
                selected = scope == StatisticsScope.YEAR,
                onClick = { scope = StatisticsScope.YEAR },
                label = { Text(stringResource(R.string.statistics_scope_year)) },
                modifier = Modifier.testTag("statistics-scope-year"),
            )
        }

        if (scope == StatisticsScope.MONTH) {
            MonthStatisticsBody(
                statistics = statistics,
                preferences = preferences,
                onExportCsv = onExportCsv,
                onExportPdf = onExportPdf,
            )
        } else {
            YearStatisticsBody(
                statistics = yearStatistics,
                preferences = preferences,
            )
        }
    }
}

@Composable
private fun MonthStatisticsBody(
    statistics: DetailedMonthStatistics,
    preferences: WorkPreferences,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
) {
    val earned = formatEarnings(statistics.earningsMinor, preferences.currencyCode)

    StatisticsSection(
        title = stringResource(R.string.statistics_time),
        rows = statisticRows(statistics, earned),
    )

    StatisticsSection(
        title = stringResource(R.string.statistics_days),
        rows = dayRows(statistics),
    )

    OutlinedButton(
        onClick = onExportCsv,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("statistics-export-csv"),
    ) {
        Text(stringResource(R.string.export_csv))
    }

    OutlinedButton(
        onClick = onExportPdf,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("statistics-export-pdf"),
    ) {
        Text(stringResource(R.string.export_pdf))
    }

    Text(
        text = stringResource(R.string.statistics_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun YearStatisticsBody(
    statistics: DetailedYearStatistics,
    preferences: WorkPreferences,
) {
    val totals = statistics.totals
    val earned = formatEarnings(totals.earningsMinor, preferences.currencyCode)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsSection(
            title = stringResource(R.string.statistics_year_totals),
            rows = statisticRows(totals, earned, prefix = "statistics-year"),
        )
        StatisticsSection(
            title = stringResource(R.string.statistics_days),
            rows = dayRows(totals, prefix = "statistics-year"),
        )

        Text(
            text = stringResource(R.string.statistics_months),
            style = MaterialTheme.typography.titleSmall,
        )
        statistics.months.forEachIndexed { index, monthStatistics ->
            YearMonthRow(monthStatistics, preferences)
            if (index != statistics.months.lastIndex) HorizontalDivider()
        }

        Text(
            text = stringResource(R.string.statistics_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun YearMonthRow(
    statistics: YearMonthStatistics,
    preferences: WorkPreferences,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("statistics-year-month-${statistics.month.monthValue}")
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = formatMonthTitle(statistics.month),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(
                R.string.statistics_month_time,
                formatDurationSummary(statistics.workedMinutes),
                statistics.workDays,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatEarnings(statistics.earningsMinor, preferences.currencyCode),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(
                "statistics-year-month-${statistics.month.monthValue}-earnings",
            ),
        )
    }
}

@Composable
private fun statisticRows(
    statistics: DetailedMonthStatistics,
    earned: String,
    prefix: String = "statistics",
): List<StatisticRow> = listOf(
    StatisticRow(
        tag = "$prefix-worked",
        label = stringResource(R.string.worked),
        value = formatDurationSummary(statistics.workedMinutes),
    ),
    StatisticRow(
        tag = "$prefix-overtime",
        label = stringResource(R.string.overtime),
        value = formatDurationSummary(statistics.overtimeMinutes),
    ),
    StatisticRow(
        tag = "$prefix-average-shift",
        label = stringResource(R.string.statistics_average_shift),
        value = formatDurationSummary(statistics.averageWorkedMinutes),
    ),
    StatisticRow(
        tag = "$prefix-longest-shift",
        label = stringResource(R.string.statistics_longest_shift),
        value = formatDurationSummary(statistics.longestWorkedMinutes),
    ),
    StatisticRow(
        tag = "$prefix-earned",
        label = stringResource(R.string.earned),
        value = earned,
    ),
)

@Composable
private fun dayRows(
    statistics: DetailedMonthStatistics,
    prefix: String = "statistics",
): List<StatisticRow> = listOf(
    StatisticRow(
        tag = "$prefix-work-days",
        label = stringResource(R.string.day_type_work),
        value = statistics.workDays.toString(),
    ),
    StatisticRow(
        tag = "$prefix-days-off",
        label = stringResource(R.string.day_type_day_off),
        value = statistics.daysOff.toString(),
    ),
    StatisticRow(
        tag = "$prefix-vacation-days",
        label = stringResource(R.string.day_type_vacation),
        value = statistics.vacationDays.toString(),
    ),
    StatisticRow(
        tag = "$prefix-sick-days",
        label = stringResource(R.string.day_type_sick),
        value = statistics.sickDays.toString(),
    ),
)

@Composable
private fun formatEarnings(amountMinor: Long?, currencyCode: String): String =
    remember(amountMinor, currencyCode) {
        runCatching {
            MoneyFormatter.formatCurrency(amountMinor, currencyCode)
        }.getOrDefault("—")
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
