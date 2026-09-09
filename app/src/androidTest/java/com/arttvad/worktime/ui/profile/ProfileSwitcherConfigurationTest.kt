package com.arttvad.worktime.ui.profile

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.arttvad.worktime.domain.model.WorkProfile
import com.arttvad.worktime.ui.theme.WorkTimeTheme
import org.junit.Rule
import org.junit.Test

class ProfileSwitcherConfigurationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileActionsRemainReachableAtTwoHundredPercentFontScale() {
        composeRule.setContent {
            WorkTimeTheme(darkTheme = false) {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f),
                ) {
                    ProfileSwitcherSheet(
                        profiles = listOf(
                            WorkProfile(1L, "Основная работа"),
                            WorkProfile(2L, "Подработка"),
                        ),
                        activeProfileId = 1L,
                        switchingEnabled = true,
                        onDismiss = {},
                        onSelectProfile = { Result.success(Unit) },
                        onCreateProfile = { Result.success(3L) },
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("profile-add")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeRule
            .onNodeWithTag("profile-name")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        composeRule
            .onNodeWithTag("profile-create")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
