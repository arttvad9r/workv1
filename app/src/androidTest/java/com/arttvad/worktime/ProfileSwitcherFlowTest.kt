package com.arttvad.worktime

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arttvad.worktime.domain.backup.BackupProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileSwitcherFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun switchingAndCreatingProfilesUpdatesActiveSelection() {
        val application = ApplicationProvider.getApplicationContext<WorkTimeApplication>()
        val profileBackup = application.container.profileBackupRepository
        val preferences = application.container.preferencesRepository
        val originalProfiles = runBlocking { profileBackup.snapshotAll() }
        val originalPreferences = runBlocking { preferences.preferences.first() }

        try {
            runBlocking {
                preferences.clearActiveShift()
                profileBackup.replaceAll(
                    listOf(
                        BackupProfile(
                            id = 1L,
                            name = "Основная работа",
                            createdAtEpochMillis = 0L,
                            days = emptyList(),
                        ),
                        BackupProfile(
                            id = 2L,
                            name = "Подработка",
                            createdAtEpochMillis = 1L,
                            days = emptyList(),
                        ),
                    ),
                )
                preferences.updatePaymentAndProfile(
                    hourlyRateMinor = originalPreferences.hourlyRateMinor,
                    currencyCode = originalPreferences.currencyCode,
                    profileId = 1L,
                )
            }

            composeRule.waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    composeRule.onNodeWithTag("profile-switcher-open")
                        .assertTextContains("Основная работа")
                    true
                }.getOrDefault(false)
            }

            composeRule.onNodeWithTag("profile-switcher-open").performClick()
            composeRule.onNodeWithTag("profile-row-2").performScrollTo().performClick()

            composeRule.waitUntil(timeoutMillis = 5_000) {
                runBlocking { preferences.preferences.first().activeProfileId == 2L }
            }
            composeRule.onNodeWithTag("profile-switcher-open")
                .assertTextContains("Подработка")

            composeRule.onNodeWithTag("profile-switcher-open").performClick()
            composeRule.onNodeWithTag("profile-add").performScrollTo().performClick()
            composeRule.onNodeWithTag("profile-name")
                .performScrollTo()
                .performTextInput("Вторая работа")
            composeRule.onNodeWithTag("profile-create")
                .performScrollTo()
                .performClick()

            composeRule.waitUntil(timeoutMillis = 5_000) {
                runBlocking { preferences.preferences.first().activeProfileId == 3L }
            }
            composeRule.onNodeWithTag("profile-switcher-open")
                .assertTextContains("Вторая работа")
            assertTrue(
                runBlocking { profileBackup.snapshotAll() }
                    .any { profile -> profile.id == 3L && profile.name == "Вторая работа" },
            )
        } finally {
            runBlocking {
                profileBackup.replaceAll(originalProfiles)
                preferences.updatePaymentAndProfile(
                    hourlyRateMinor = originalPreferences.hourlyRateMinor,
                    currencyCode = originalPreferences.currencyCode,
                    profileId = originalPreferences.activeProfileId,
                )
                preferences.clearActiveShift()
                originalPreferences.activeShift?.let { shift ->
                    preferences.startShift(
                        date = shift.date,
                        startedAtEpochMillis = shift.startedAtEpochMillis,
                    )
                }
            }
        }

        assertEquals(
            originalPreferences.activeProfileId,
            runBlocking { preferences.preferences.first().activeProfileId },
        )
    }
}
