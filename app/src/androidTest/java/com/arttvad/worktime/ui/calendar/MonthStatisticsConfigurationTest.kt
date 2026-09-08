package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.domain.calculation.DetailedMonthStatistics
import com.arttvad.worktime.domain.calculation.DetailedYearStatisticsCalculator
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test

class MonthStatisticsConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun monthAndYearStatisticsRemainReachableAtTwoHundredPercentFontScale() {
        val year = Year.of(2026)
        val yearStatistics = DetailedYearStatisticsCalculator.calculate(
            year = year,
            days = listOf(
                WorkDay(
                    date = LocalDate.of(2026, 1, 10),
                    workedMinutes = 600,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 1L,
                ),
            ),
            hourlyRateMinor = 1_500L,
        )

        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    MonthStatisticsContent(
                        month = YearMonth.of(2026, 9),
                        statistics = DetailedMonthStatistics(
                            workDays = 12,
                            daysOff = 8,
                            vacationDays = 2,
                            sickDays = 1,
                            workedMinutes = 7_200,
                            overtimeMinutes = 240,
                            averageWorkedMinutes = 600,
                            longestWorkedMinutes = 720,
                            earningsMinor = 180_000L,
                        ),
                        yearStatistics = yearStatistics,
                        preferences = WorkPreferences(
                            hourlyRateMinor = 1_500L,
                            currencyCode = "EUR",
                        ),
                        onDismiss = {},
                        onExportCsv = {},
                        onExportXlsx = {},
                        onExportPdf = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("statistics-export-csv")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()

        composeRule
            .onNodeWithTag("statistics-export-xlsx")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()

        composeRule
            .onNodeWithTag("statistics-export-pdf")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()

        composeRule
            .onNodeWithTag("statistics-scope-year")
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithTag("statistics-year-month-12")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("statistics-close")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
