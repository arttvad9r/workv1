package com.arttvad.worktime

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arttvad.worktime.data.repository.WorkDayRepository
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CalendarSearchFlowTest {
    private val month = YearMonth.now()
    private val workDate = month.atDay(1)
    private val vacationDate = month.atDay(2)

    private val application: WorkTimeApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as WorkTimeApplication

    private val repository: WorkDayRepository
        get() = application.container.workDayRepository

    @Before
    fun seedEntries() {
        runBlocking {
            repository.delete(workDate)
            repository.delete(vacationDate)
            repository.upsert(
                WorkDay(
                    date = workDate,
                    workedMinutes = 8 * 60,
                    overtimeMinutes = 30,
                    note = "Командировка в офис",
                    updatedAtEpochMillis = 1L,
                    type = WorkDayType.WORK,
                ),
            )
            repository.upsert(
                WorkDay(
                    date = vacationDate,
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "Море",
                    updatedAtEpochMillis = 2L,
                    type = WorkDayType.VACATION,
                ),
            )
        }
    }

    @After
    fun cleanupEntries() {
        runBlocking {
            repository.delete(workDate)
            repository.delete(vacationDate)
        }
    }

    @Test
    fun noteSearchAndDayTypeFilterOpenExistingEntry() = runEmptyComposeUiTest {
        enableAccessibilityChecks()
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val searchLabel = targetContext.getString(R.string.calendar_search_open)
        val workResultTag = "calendar-search-result-$workDate"
        val vacationResultTag = "calendar-search-result-$vacationDate"

        ActivityScenario.launch(MainActivity::class.java).use {
            onNodeWithContentDescription(searchLabel).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("calendar-search-query").assertExists()
                    true
                }.getOrDefault(false)
            }
            onRoot().tryPerformAccessibilityChecks()

            onNodeWithTag("calendar-search-query").performTextInput("КОМАНДИРОВКА")
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag(workResultTag).assertExists()
                    true
                }.getOrDefault(false)
            }
            assertTrue(onAllNodesWithTag(vacationResultTag).fetchSemanticsNodes().isEmpty())

            onNodeWithTag("calendar-search-query").performTextReplacement("")
            onNodeWithTag("calendar-search-filter-vacation")
                .performScrollTo()
                .performClick()

            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag(vacationResultTag).assertExists()
                    true
                }.getOrDefault(false)
            }
            assertTrue(onAllNodesWithTag(workResultTag).fetchSemanticsNodes().isEmpty())

            onNodeWithTag(vacationResultTag)
                .performScrollTo()
                .performClick()

            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-type-vacation").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("day-editor-type-vacation").assertIsSelected()
        }
    }
}
