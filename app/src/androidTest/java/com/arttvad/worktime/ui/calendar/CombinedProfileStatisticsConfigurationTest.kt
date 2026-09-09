package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.domain.calculation.DetailedMonthStatistics
import com.arttvad.worktime.domain.calculation.DetailedYearStatistics
import com.arttvad.worktime.domain.calculation.ProfilePeriodStatistics
import com.arttvad.worktime.domain.calculation.ProfileReportStatisticsCalculator
import com.arttvad.worktime.domain.calculation.YearMonthStatistics
import com.arttvad.worktime.ui.profile.LocalProfileReportEnvironment
import com.arttvad.worktime.ui.profile.ProfileReportEnvironment
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import java.time.Year
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test

class CombinedProfileStatisticsConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun combinedProfileReportsRemainReachableAtTwoHundredPercentFontScale() {
        val year = Year.of(2026)
        val month = YearMonth.of(2026, 9)
        val reports = listOf(
            ProfilePeriodStatistics(
                profileId = 1L,
                profileName = "Основная работа",
                currencyCode = "EUR",
                month = DetailedMonthStatistics(
                    workDays = 2,
                    workedMinutes = 960,
                    overtimeMinutes = 60,
                    averageWorkedMinutes = 480,
                    longestWorkedMinutes = 540,
                    earningsMinor = 24_000L,
                ),
                year = DetailedYearStatistics(
                    year = year,
                    totals = DetailedMonthStatistics(
                        workDays = 2,
                        workedMinutes = 960,
                        overtimeMinutes = 60,
                        averageWorkedMinutes = 480,
                        longestWorkedMinutes = 540,
                        earningsMinor = 24_000L,
                    ),
                    months = listOf(
                        YearMonthStatistics(
                            month = YearMonth.of(2026, 1),
                            workedMinutes = 480,
                            workDays = 1,
                            earningsMinor = 12_000L,
                        ),
                    ),
                ),
            ),
            ProfilePeriodStatistics(
                profileId = 2L,
                profileName = "Подработка",
                currencyCode = "USD",
                month = DetailedMonthStatistics(
                    workDays = 1,
                    workedMinutes = 600,
                    overtimeMinutes = 30,
                    averageWorkedMinutes = 600,
                    longestWorkedMinutes = 600,
                    earningsMinor = 30_000L,
                ),
                year = DetailedYearStatistics(
                    year = year,
                    totals = DetailedMonthStatistics(
                        workDays = 1,
                        workedMinutes = 600,
                        overtimeMinutes = 30,
                        averageWorkedMinutes = 600,
                        longestWorkedMinutes = 600,
                        earningsMinor = 30_000L,
                    ),
                    months = listOf(
                        YearMonthStatistics(
                            month = YearMonth.of(2026, 1),
                            workedMinutes = 600,
                            overtimeMinutes = 30,
                            workDays = 1,
                            earningsMinor = 30_000L,
                        ),
                    ),
                ),
            ),
        )
        val combinedMonth = ProfileReportStatisticsCalculator.combineMonth(reports)
        val combinedYear = ProfileReportStatisticsCalculator.combineYear(year, reports)

        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    CompositionLocalProvider(
                        LocalProfileReportEnvironment provides ProfileReportEnvironment(
                            activeProfileId = 1L,
                            profiles = reports,
                            combinedMonthStatistics = combinedMonth,
                            combinedYearStatistics = combinedYear,
                        ),
                    ) {
                        MonthStatisticsContent(
                            month = month,
                            statistics = reports.first().month,
                            yearStatistics = reports.first().year,
                            preferences = WorkPreferences(
                                hourlyRateMinor = 1_500L,
                                currencyCode = "EUR",
                            ),
                            onDismiss = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("statistics-profile-all").performClick()

        composeRule
            .onNodeWithTag("statistics-combined-worked-value")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("26 ч")
        composeRule
            .onNodeWithTag("statistics-combined-earnings-EUR-value")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("statistics-combined-earnings-USD-value")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("statistics-profile-row-2")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("statistics-scope-year")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithTag("statistics-combined-year-month-1")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
