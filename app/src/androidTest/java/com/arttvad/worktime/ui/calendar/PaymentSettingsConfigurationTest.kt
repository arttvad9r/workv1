package com.arttvad.worktime.ui.calendar

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import org.junit.Rule
import org.junit.Test

class PaymentSettingsConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backupActionsRemainReachableAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    PaymentSettingsSheet(
                        preferences = WorkPreferences(
                            hourlyRateMinor = 1_500L,
                            currencyCode = "EUR",
                        ),
                        onDismiss = {},
                        onSave = { _, _ -> },
                        onCreateBackup = {},
                        onRestoreBackup = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("settings-data-open")
            .performClick()

        composeRule
            .onNodeWithTag("settings-backup-create")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()

        composeRule
            .onNodeWithTag("settings-backup-restore")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }
}
