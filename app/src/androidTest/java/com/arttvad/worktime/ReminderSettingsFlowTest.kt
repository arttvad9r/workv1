package com.arttvad.worktime

import android.Manifest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.reminder.WorkReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ReminderSettingsFlowTest {
    private val application: WorkTimeApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as WorkTimeApplication

    @Test
    fun reminderPersistsAcrossRelaunchAndCanBeCancelled() = runEmptyComposeUiTest {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val repository = application.container.preferencesRepository
        val original: WorkPreferences = runBlocking { repository.preferences.first() }
        val settingsLabel = context.getString(R.string.settings)

        try {
            runCatching {
                instrumentation.uiAutomation.grantRuntimePermission(
                    context.packageName,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
            runBlocking { repository.updateReminder(false, 20, 0) }
            WorkReminderScheduler.cancel(context)

            ActivityScenario.launch(MainActivity::class.java).use {
                onNodeWithContentDescription(settingsLabel).performClick()
                onNodeWithTag("settings-reminder-open").performScrollTo().performClick()
                onNodeWithTag("settings-reminder-enabled").performClick()
                onNodeWithTag("settings-reminder-time").performTextReplacement("21:30")
                onNodeWithTag("settings-reminder-save").performScrollTo().performClick()

                waitUntil(timeoutMillis = 5_000) {
                    val current = runBlocking { repository.preferences.first() }
                    current.reminderEnabled &&
                        current.reminderHour == 21 &&
                        current.reminderMinute == 30 &&
                        WorkReminderScheduler.isScheduled(context)
                }
            }

            ActivityScenario.launch(MainActivity::class.java).use {
                onNodeWithContentDescription(settingsLabel).performClick()
                onNodeWithTag("settings-reminder-open").performScrollTo().performClick()
                onNodeWithTag("settings-reminder-time").assertTextContains("21:30")

                onNodeWithTag("settings-reminder-enabled").performClick()
                onNodeWithTag("settings-reminder-save").performScrollTo().performClick()

                waitUntil(timeoutMillis = 5_000) {
                    val current = runBlocking { repository.preferences.first() }
                    !current.reminderEnabled && !WorkReminderScheduler.isScheduled(context)
                }
            }
        } finally {
            runBlocking {
                repository.updateReminder(
                    enabled = original.reminderEnabled,
                    hour = original.reminderHour,
                    minute = original.reminderMinute,
                )
            }
            WorkReminderScheduler.sync(
                context = context,
                enabled = original.reminderEnabled,
                hour = original.reminderHour,
                minute = original.reminderMinute,
            )
        }
    }
}
