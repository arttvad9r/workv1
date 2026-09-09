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
import com.arttvad.worktime.domain.calculation.CombinedMonthStatistics
import com.arttvad.worktime.domain.calculation.CombinedYearStatistics
import com.arttvad.worktime.domain.calculation.CurrencyEarningsStatistics
import com.arttvad.worktime.domain.calculation.DetailedMonthStatistics
import com.arttvad.worktime.domain.calculation.DetailedYearStatistics
import com.arttvad.worktime.domain.calculation.MoneyFormatter
import com.arttvad.worktime.domain.calculation.ProfilePeriodStatistics
import com.arttvad.worktime.domain.calculation.YearMonthStatistics
import com.arttvad.worktime.ui.profile.LocalProfileReportEnvironment
import java.time.Year
import java.time.YearMonth

private enum class StatisticsScope {
    MONTH,
    YEAR,
}

private enum class ProfileStatisticsScope {
    ACTIVE,
    ALL,
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
    onExportXlsx: () -> Unit,
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
            onExportXlsx = onExportXlsx,
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
    onExportXlsx: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var scope by rememberSaveable { mutableStateOf(StatisticsScope.MONTH) }
    var profileScope by rememberSaveable { mutableStateOf(ProfileStatisticsScope.ACTIVE) }
    val profileReports = LocalProfileReportEnvironment.current
    val activeProfile = profileReports.profiles.firstOrNull { profile ->
        profile.profileId == profileReports.activeProfileId
    }
    val hasMultipleProfiles = profileReports.profiles.size > 1
    val showCombined = hasMultipleProfiles && profileScope == ProfileStatisticsScope.ALL
    val periodTitle = if (scope == StatisticsScope.MONTH) {
        formatMonthTitle(month)
    } else {
        yearStatistics.year.value.toString()
    }
    val profileTitle = if (showCombined) {
        stringResource(R.string.statistics_profile_all)
    } else {
        activeProfile?.profileName ?: stringResource(R.string.statistics_profile_current)
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
                    text = stringResource(
                        R.string.statistics_period_profile,
                        periodTitle,
                        profileTitle,
                    ),
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

        if (hasMultipleProfiles) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !showCombined,
                    onClick = { profileScope = ProfileStatisticsScope.ACTIVE },
                    label = {
                        Text(activeProfile?.profileName ?: stringResource(R.string.statistics_profile_current))
                    },
                    modifier = Modifier.testTag("statistics-profile-active"),
                )
                FilterChip(
                    selected = showCombined,
                    onClick = { profileScope = ProfileStatisticsScope.ALL },
                    label = { Text(stringResource(R.string.statistics_profile_all)) },
                    modifier = Modifier.testTag("statistics-profile-all"),
                )
            }
        }

        if (scope == StatisticsScope.MONTH) {
            if (showCombined) {
                CombinedMonthStatisticsBody(
                    combined = profileReports.combinedMonthStatistics,
                    profiles = profileReports.profiles,
                )
            } else {
                MonthStatisticsBody(
                    statistics = statistics,
                    preferences = preferences,
                    onExportCsv = onExportCsv,
                    onExportXlsx = onExportXlsx,
                    onExportPdf = onExportPdf,
                )
            }
        } else {
            if (showCombined) {
                CombinedYearStatisticsBody(
                    combined = profileReports.combinedYearStatistics,
                    profiles = profileReports.profiles,
                )
            } else {
                YearStatisticsBody(
                    statistics = yearStatistics,
                    preferences = preferences,
                )
            }
        }
    }
}

@Composable
private fun MonthStatisticsBody(
    statistics: DetailedMonthStatistics,
    preferences: WorkPreferences,
    onExportCsv: () -> Unit,
    onExportXlsx: () -> Unit,
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
        onClick = onExportXlsx,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("statistics-export-xlsx"),
    ) {
        Text(stringResource(R.string.export_xlsx))
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
private fun CombinedMonthStatisticsBody(
    combined: CombinedMonthStatistics,
    profiles: List<ProfilePeriodStatistics>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsSection(
            title = stringResource(R.string.statistics_time),
            rows = statisticRows(
                statistics = combined.statistics,
                earned = "",
                prefix = "statistics-combined",
                includeEarnings = false,
            ),
        )
        StatisticsSection(
            title = stringResource(R.string.statistics_days),
            rows = dayRows(combined.statistics, prefix = "statistics-combined"),
        )
        CombinedEarningsSection(
            earnings = combined.earningsByCurrency,
            prefix = "statistics-combined",
        )
        ProfileBreakdownSection(
            profiles = profiles,
            year = false,
        )
        CombinedReportNotes()
    }
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
private fun CombinedYearStatisticsBody(
    combined: CombinedYearStatistics,
    profiles: List<ProfilePeriodStatistics>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatisticsSection(
            title = stringResource(R.string.statistics_year_totals),
            rows = statisticRows(
                statistics = combined.totals,
                earned = "",
                prefix = "statistics-combined-year",
                includeEarnings = false,
            ),
        )
        StatisticsSection(
            title = stringResource(R.string.statistics_days),
            rows = dayRows(combined.totals, prefix = "statistics-combined-year"),
        )
        CombinedEarningsSection(
            earnings = combined.earningsByCurrency,
            prefix = "statistics-combined-year",
        )

        Text(
            text = stringResource(R.string.statistics_months),
            style = MaterialTheme.typography.titleSmall,
        )
        combined.months.forEachIndexed { index, monthStatistics ->
            CombinedYearMonthRow(monthStatistics)
            if (index != combined.months.lastIndex) HorizontalDivider()
        }

        ProfileBreakdownSection(
            profiles = profiles,
            year = true,
        )
        CombinedReportNotes()
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
private fun CombinedYearMonthRow(
    statistics: com.arttvad.worktime.domain.calculation.CombinedYearMonthStatistics,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("statistics-combined-year-month-${statistics.month.monthValue}")
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
            text = formatCurrencyEarningsList(statistics.earningsByCurrency),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(
                "statistics-combined-year-month-${statistics.month.monthValue}-earnings",
            ),
        )
    }
}

@Composable
private fun CombinedEarningsSection(
    earnings: List<CurrencyEarningsStatistics>,
    prefix: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.statistics_combined_earnings),
            style = MaterialTheme.typography.titleSmall,
        )
        if (earnings.isEmpty()) {
            Text(
                text = stringResource(R.string.unknown_amount),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("$prefix-earnings-empty"),
            )
        } else {
            earnings.forEachIndexed { index, earningsRow ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("$prefix-earnings-${earningsRow.currencyCode}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = earningsRow.currencyCode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatEarnings(
                            earningsRow.earningsMinor,
                            earningsRow.currencyCode,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag(
                            "$prefix-earnings-${earningsRow.currencyCode}-value",
                        ),
                    )
                }
                if (index != earnings.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ProfileBreakdownSection(
    profiles: List<ProfilePeriodStatistics>,
    year: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.statistics_profiles_breakdown),
            style = MaterialTheme.typography.titleSmall,
        )
        profiles.forEachIndexed { index, profile ->
            val statistics = if (year) profile.year.totals else profile.month
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("statistics-profile-row-${profile.profileId}")
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = profile.profileName,
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
                    text = formatEarnings(statistics.earningsMinor, profile.currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(
                        "statistics-profile-row-${profile.profileId}-earnings",
                    ),
                )
            }
            if (index != profiles.lastIndex) HorizontalDivider()
        }
    }
}

@Composable
private fun CombinedReportNotes() {
    Text(
        text = stringResource(R.string.statistics_combined_currency_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.statistics_combined_incomplete_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.statistics_export_profile_only),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun statisticRows(
    statistics: DetailedMonthStatistics,
    earned: String,
    prefix: String = "statistics",
    includeEarnings: Boolean = true,
): List<StatisticRow> {
    val rows = mutableListOf(
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
    )
    if (includeEarnings) {
        rows += StatisticRow(
            tag = "$prefix-earned",
            label = stringResource(R.string.earned),
            value = earned,
        )
    }
    return rows
}

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
        formatEarningsValue(amountMinor, currencyCode)
    }

private fun formatEarningsValue(amountMinor: Long?, currencyCode: String): String =
    runCatching {
        MoneyFormatter.formatCurrency(amountMinor, currencyCode)
    }.getOrDefault("—")

private fun formatCurrencyEarningsList(
    earnings: List<CurrencyEarningsStatistics>,
): String = if (earnings.isEmpty()) {
    "—"
} else {
    earnings.joinToString(separator = " · ") { row ->
        formatEarningsValue(row.earningsMinor, row.currencyCode)
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
