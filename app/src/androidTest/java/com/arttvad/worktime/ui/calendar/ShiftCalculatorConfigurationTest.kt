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
import androidx.compose.ui.test.performTextInput
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class ShiftCalculatorConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calculatorApplyRemainsReachableAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    DayEditorContent(
                        date = LocalDate.of(2026, 9, 8),
                        entry = null,
                        currencyCode = "EUR",
                        onDismiss = {},
                        onSave = { _, _, _, _, _, _ -> },
                        onDelete = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("day-editor-time-calculator")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("day-editor-shift-start")
            .performScrollTo()
            .performTextInput("09:00")
        composeRule.onNodeWithTag("day-editor-shift-end")
            .performScrollTo()
            .performTextInput("18:00")
        composeRule.onNodeWithTag("day-editor-shift-break")
            .performScrollTo()
            .performTextInput("60")
        composeRule.onNodeWithTag("day-editor-shift-apply")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }
}
