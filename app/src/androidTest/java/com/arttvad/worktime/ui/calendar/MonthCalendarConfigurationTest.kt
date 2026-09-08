package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test

class MonthCalendarConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dayCellsGrowAtTwoHundredPercentFontScale() {
        val date = LocalDate.of(2026, 9, 8)
        val entry = WorkDay(
            date = date,
            workedMinutes = 8 * 60 + 30,
            overtimeMinutes = 30,
            note = "",
            updatedAtEpochMillis = 0L,
        )

        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    MonthCalendar(
                        month = YearMonth.of(2026, 9),
                        entries = mapOf(date to entry),
                        selectedDate = date,
                        onDaySelected = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("day-$date")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(80.dp)
    }

    @Test
    fun dayEditorPrimaryActionRemainsReachableAtTwoHundredPercentFontScale() {
        val date = LocalDate.of(2026, 9, 8)
        val entry = WorkDay(
            date = date,
            workedMinutes = 8 * 60,
            overtimeMinutes = 30,
            note = "Проверка заметки при крупном шрифте",
            updatedAtEpochMillis = 1L,
            hourlyRateOverrideMinor = 2_500L,
        )

        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    DayEditorContent(
                        date = date,
                        entry = entry,
                        currencyCode = "EUR",
                        onDismiss = {},
                        onSave = { _, _, _, _, _, _ -> },
                        onDelete = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("day-editor-save")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun paymentSettingsPrimaryActionRemainsReachableAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    PaymentSettingsSheet(
                        preferences = WorkPreferences(
                            hourlyRateMinor = 1_550L,
                            currencyCode = "EUR",
                        ),
                        onDismiss = {},
                        onSave = { _, _ -> },
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("settings-save")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }
}
