package com.arttvad.worktime

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test

class BackupRestoreUiFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun restoreRequiresExplicitConfirmationBeforeDocumentPicker() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val settings = context.getString(R.string.settings)

        composeRule
            .onNodeWithContentDescription(settings)
            .performClick()

        composeRule
            .onNodeWithTag("settings-data-open")
            .performClick()

        composeRule
            .onNodeWithTag("settings-backup-restore")
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithTag("backup-restore-confirm")
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag("backup-restore-cancel")
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNodeWithTag("backup-restore-confirm")
            .assertDoesNotExist()
        composeRule
            .onNodeWithTag("settings-payment-open")
            .assertIsDisplayed()
    }
}
