package com.arttvad.worktime

import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ShiftTimerFlowTest {
    @Test
    fun timerPersistsAcrossRelaunchAndStopPrefillsWorkedDuration() = runEmptyComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as WorkTimeApplication
        val preferencesRepository = application.container.preferencesRepository
        val today = LocalDate.now()

        runBlocking { preferencesRepository.clearActiveShift() }
        try {
            launchToday().use {
                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("day-editor-time-calculator").assertExists()
                        true
                    }.getOrDefault(false)
                }
                onNodeWithTag("day-editor-time-calculator")
                    .performScrollTo()
                    .performClick()
                onNodeWithTag("day-editor-timer-start")
                    .performScrollTo()
                    .performClick()

                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("day-editor-timer-stop").assertExists()
                        true
                    }.getOrDefault(false)
                }
            }

            val persisted = runBlocking { preferencesRepository.preferences.first().activeShift }
            assertEquals(today, persisted?.date)

            launchToday().use {
                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("day-editor-time-calculator").assertExists()
                        true
                    }.getOrDefault(false)
                }
                onNodeWithTag("day-editor-time-calculator")
                    .performScrollTo()
                    .performClick()
                onNodeWithTag("day-editor-timer-stop")
                    .performScrollTo()
                    .assertExists()
            }

            runBlocking {
                preferencesRepository.clearActiveShift()
                preferencesRepository.startShift(
                    date = today,
                    startedAtEpochMillis = System.currentTimeMillis() - (8 * 60 + 30) * 60_000L - 5_000L,
                )
            }

            launchToday().use {
                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("day-editor-time-calculator").assertExists()
                        true
                    }.getOrDefault(false)
                }
                onNodeWithTag("day-editor-time-calculator")
                    .performScrollTo()
                    .performClick()
                onNodeWithTag("day-editor-timer-stop")
                    .performScrollTo()
                    .performClick()

                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("day-editor-worked-hours").assertTextContains("8")
                        onNodeWithTag("day-editor-worked-minutes").assertTextContains("30")
                        true
                    }.getOrDefault(false)
                }
            }

            assertNull(runBlocking { preferencesRepository.preferences.first().activeShift })
        } finally {
            runBlocking { preferencesRepository.clearActiveShift() }
        }
    }

    private fun launchToday(): ActivityScenario<MainActivity> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return ActivityScenario.launch(
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_ADD_TODAY
            },
        )
    }
}
