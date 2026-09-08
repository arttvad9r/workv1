package com.arttvad.worktime

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.tryPerformAccessibilityChecks
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
class WorkTimeFlowTest {
    private val today: LocalDate = LocalDate.now()
    private val dayTag: String = "day-$today"

    private val repository: WorkDayRepository
        get() {
            val application = InstrumentationRegistry.getInstrumentation()
                .targetContext
                .applicationContext as WorkTimeApplication
            return application.container.workDayRepository
        }

    @Before
    fun clearTestDay() {
        runBlocking { repository.delete(today) }
    }

    @After
    fun cleanupTestDay() {
        runBlocking { repository.delete(today) }
    }

    @Test
    fun mainCalendarAndDayEditorPassAccessibilityChecks() = runEmptyComposeUiTest {
        enableAccessibilityChecks()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag(dayTag).assertExists()
                    true
                }.getOrDefault(false)
            }
            onRoot().tryPerformAccessibilityChecks()

            onNodeWithTag(dayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-worked-hours").assertExists()
                    true
                }.getOrDefault(false)
            }
            onRoot().tryPerformAccessibilityChecks()
        }
    }

    @Test
    fun saveEditDeleteUndoAndRelaunch() = runEmptyComposeUiTest {
        val undoLabel = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.undo)

        ActivityScenario.launch(MainActivity::class.java).use {
            onNodeWithTag(dayTag).performClick()
            onNodeWithTag("day-editor-worked-hours").performTextInput("8")
            onNodeWithTag("day-editor-worked-minutes").performTextInput("30")
            onNodeWithTag("day-editor-save").performClick()

            waitUntil(timeoutMillis = 5_000) {
                currentEntry()?.workedMinutes == 8 * 60 + 30
            }

            onNodeWithTag(dayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-delete").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("day-editor-worked-hours").performTextReplacement("9")
            onNodeWithTag("day-editor-worked-minutes").performTextReplacement("")
            onNodeWithTag("day-editor-save").performClick()

            waitUntil(timeoutMillis = 5_000) {
                currentEntry()?.workedMinutes == 9 * 60
            }

            onNodeWithTag(dayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-delete").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("day-editor-delete").performClick()

            waitUntil(timeoutMillis = 5_000) { currentEntry() == null }
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithText(undoLabel).assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithText(undoLabel).performClick()
            waitUntil(timeoutMillis = 5_000) {
                currentEntry()?.workedMinutes == 9 * 60
            }
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            onNodeWithTag(dayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-delete").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("day-editor-worked-hours").assertTextContains("9")
        }
    }

    @Test
    fun visibleMonthAndEditorDraftSurviveRecreation() = runEmptyComposeUiTest {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val nextMonthLabel = targetContext.getString(R.string.next_month)
        val todayLabel = targetContext.getString(R.string.today)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onNodeWithContentDescription(nextMonthLabel).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithContentDescription(todayLabel).assertExists()
                    true
                }.getOrDefault(false)
            }

            scenario.recreate()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithContentDescription(todayLabel).assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithContentDescription(todayLabel).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag(dayTag).assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag(dayTag).performClick()
            onNodeWithTag("day-editor-worked-hours").performTextInput("7")
            onNodeWithTag("day-editor-worked-minutes").performTextInput("45")
            onNodeWithTag("day-editor-add-note").performClick()
            onNodeWithTag("day-editor-note").performTextInput("черновик")

            scenario.recreate()

            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-worked-hours").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("day-editor-worked-hours").assertTextContains("7")
            onNodeWithTag("day-editor-worked-minutes").assertTextContains("45")
            onNodeWithTag("day-editor-note").assertTextContains("черновик")
        }
    }

    private fun currentEntry(): WorkDay? = runBlocking {
        repository.observeMonth(YearMonth.from(today))
            .first()
            .firstOrNull { it.date == today }
    }
}
