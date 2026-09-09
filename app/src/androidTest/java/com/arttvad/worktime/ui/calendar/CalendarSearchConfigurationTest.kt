package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test

class CalendarSearchConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchControlsAndResultsRemainReachableAtTwoHundredPercentFontScale() {
        val resultDate = LocalDate.of(2026, 9, 18)
        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    CalendarSearchContent(
                        month = YearMonth.of(2026, 9),
                        entries = listOf(
                            WorkDay(
                                date = resultDate,
                                workedMinutes = 0,
                                overtimeMinutes = 0,
                                note = "Отпуск у моря",
                                updatedAtEpochMillis = 1L,
                                type = WorkDayType.VACATION,
                            ),
                        ),
                        onDismiss = {},
                        onSelectDay = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("calendar-search-query")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("calendar-search-filter-vacation")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("calendar-search-result-$resultDate")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("calendar-search-close")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
