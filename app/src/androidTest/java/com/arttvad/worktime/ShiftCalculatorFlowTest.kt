package com.arttvad.worktime

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arttvad.worktime.data.repository.WorkDayRepository
import com.arttvad.worktime.domain.model.WorkDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ShiftCalculatorFlowTest {
    private val today = LocalDate.now()
    private val dayTag = "day-$today"

    private val application: WorkTimeApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as WorkTimeApplication

    private val repository: WorkDayRepository
        get() = application.container.workDayRepository

    @Before
    fun clearTestDay() {
        runBlocking { repository.delete(today) }
    }

    @After
    fun cleanupTestDay() {
        runBlocking { repository.delete(today) }
    }

    @Test
    fun overnightShiftWithBreakSavesCalculatedDuration() = runEmptyComposeUiTest {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag(dayTag).assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag(dayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-time-calculator").assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag("day-editor-time-calculator").performScrollTo().performClick()
            onNodeWithTag("day-editor-shift-start")
                .performScrollTo()
                .performTextInput("22:00")
            onNodeWithTag("day-editor-shift-end")
                .performScrollTo()
                .performTextInput("06:00")
            onNodeWithTag("day-editor-shift-break")
                .performScrollTo()
                .performTextInput("30")
            onNodeWithTag("day-editor-shift-apply")
                .performScrollTo()
                .performClick()

            onNodeWithTag("day-editor-worked-hours").assertTextContains("7")
            onNodeWithTag("day-editor-worked-minutes").assertTextContains("30")
            onNodeWithTag("day-editor-save").performScrollTo().performClick()

            waitUntil(timeoutMillis = 5_000) {
                currentEntry()?.workedMinutes == 7 * 60 + 30
            }
        }
    }

    private fun currentEntry(): WorkDay? = runBlocking {
        repository.observeMonth(YearMonth.from(today))
            .first()
            .firstOrNull { it.date == today }
    }
}
