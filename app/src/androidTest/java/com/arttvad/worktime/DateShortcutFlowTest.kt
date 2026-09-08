package com.arttvad.worktime

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DateShortcutFlowTest {
    @Test
    fun monthTitleOpensDatePickerAndConfirmedDateOpensEditor() = runEmptyComposeUiTest {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("month-date-shortcut").assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag("month-date-shortcut").performClick()

            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("date-shortcut-picker").assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag("date-shortcut-confirm")
                .assertIsEnabled()
                .performClick()

            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-worked-hours").assertExists()
                    true
                }.getOrDefault(false)
            }
        }
    }
}
