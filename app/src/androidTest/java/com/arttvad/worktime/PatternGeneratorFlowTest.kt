package com.arttvad.worktime

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arttvad.worktime.data.repository.WorkDayRepository
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PatternGeneratorFlowTest {
    private val today = LocalDate.now()
    private val dates = List(4) { index -> today.plusDays(index.toLong()) }
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")

    private val application: WorkTimeApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as WorkTimeApplication

    private val repository: WorkDayRepository
        get() = application.container.workDayRepository

    @Before
    fun clearGeneratedDates() {
        runBlocking { dates.forEach { repository.delete(it) } }
    }

    @After
    fun cleanupGeneratedDates() {
        runBlocking { dates.forEach { repository.delete(it) } }
    }

    @Test
    fun twoOnTwoOffPreviewAppliesAndUndoRemovesOnlyGeneratedDays() = runEmptyComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val openLabel = context.getString(R.string.pattern_open)
        val undoLabel = context.getString(R.string.undo)

        ActivityScenario.launch(MainActivity::class.java).use {
            onNodeWithContentDescription(openLabel).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("pattern-start-date").assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag("pattern-start-date")
                .performTextReplacement(dates.first().format(dateFormatter))
            onNodeWithTag("pattern-end-date")
                .performTextReplacement(dates.last().format(dateFormatter))
            onNodeWithTag("pattern-apply")
                .performScrollTo()
                .performClick()

            waitUntil(timeoutMillis = 5_000) {
                val entries = dates.map(::currentEntry)
                entries[0]?.type == WorkDayType.WORK && entries[0]?.workedMinutes == 480 &&
                    entries[1]?.type == WorkDayType.WORK && entries[1]?.workedMinutes == 480 &&
                    entries[2]?.type == WorkDayType.DAY_OFF && entries[2]?.workedMinutes == 0 &&
                    entries[3]?.type == WorkDayType.DAY_OFF && entries[3]?.workedMinutes == 0
            }

            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithText(undoLabel).assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithText(undoLabel).performClick()

            waitUntil(timeoutMillis = 5_000) {
                dates.all { currentEntry(it) == null }
            }
        }
    }

    @Test
    fun previewShowsCalculatedWorkAndOffCounts() = runEmptyComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val openLabel = context.getString(R.string.pattern_open)

        ActivityScenario.launch(MainActivity::class.java).use {
            onNodeWithContentDescription(openLabel).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("pattern-start-date").assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag("pattern-start-date")
                .performTextReplacement(dates.first().format(dateFormatter))
            onNodeWithTag("pattern-end-date")
                .performTextReplacement(dates.last().format(dateFormatter))

            onNodeWithText("Рабочих: 2 · Выходных: 2 · Всего: 4")
                .performScrollTo()
                .assertTextContains("Рабочих: 2")
        }
    }

    private fun currentEntry(date: LocalDate): WorkDay? = runBlocking {
        repository.observeMonth(YearMonth.from(date))
            .first()
            .firstOrNull { it.date == date }
    }
}
