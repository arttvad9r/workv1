package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.domain.calculation.DetailedMonthStatistics
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test

class MonthStatisticsConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailedStatisticsRemainReachableAtTwoHundredPercentFontScale() {
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

        composeRule
            .onNodeWithTag("statistics-sick-days")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("statistics-close")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
