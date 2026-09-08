package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test

class PatternGeneratorConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryActionRemainsReachableAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    PatternGeneratorContent(
                        visibleMonth = YearMonth.of(2026, 9),
                        onDismiss = {},
                        onApply = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("pattern-apply")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }
}
