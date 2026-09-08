package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.arttvad.worktime.data.preferences.ActiveShiftSession
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class ShiftTimerConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun timerActionsRemainReachableAtTwoHundredPercentFontScale() {
        val date = LocalDate.of(2026, 9, 8)

        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    CompositionLocalProvider(
                        LocalShiftTimerEnvironment provides ShiftTimerEnvironment(
                            activeShift = ActiveShiftSession(
                                date = date,
                                startedAtEpochMillis = System.currentTimeMillis() - 8 * 60 * 60_000L,
                            ),
                            onStartShift = { Result.success(Unit) },
                            onStopShift = { Result.success(8 * 60) },
                            onResetShift = { Result.success(Unit) },
                        ),
                    ) {
                        DayEditorContent(
                            date = date,
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
        }

        composeRule.onNodeWithTag("day-editor-time-calculator")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("day-editor-timer-stop")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        composeRule.onNodeWithTag("day-editor-timer-reset")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }
}
