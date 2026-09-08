package com.arttvad.worktime

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MonthStatisticsFlowTest {
    private val month = YearMonth.now()
    private val firstWorkDate = month.atDay(20)
    private val secondWorkDate = month.atDay(21)
    private val vacationDate = month.atDay(22)
    private val testDates = listOf(firstWorkDate, secondWorkDate, vacationDate)

    private val application: WorkTimeApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as WorkTimeApplication

    private val repository: WorkDayRepository
        get() = application.container.workDayRepository

    @Before
    fun seedStatisticsDays() {
        runBlocking {
            testDates.forEach { repository.delete(it) }
            repository.upsert(
                WorkDay(
                    date = firstWorkDate,
                    workedMinutes = 480,
                    overtimeMinutes = 60,
                    note = "",
                    updatedAtEpochMillis = 1L,
                ),
            )
            repository.upsert(
                WorkDay(
                    date = secondWorkDate,
                    workedMinutes = 600,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 2L,
                ),
            )
            repository.upsert(
                WorkDay(
                    date = vacationDate,
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 3L,
                    type = WorkDayType.VACATION,
                ),
            )
        }
    }

    @After
    fun cleanupStatisticsDays() {
        runBlocking { testDates.forEach { repository.delete(it) } }
    }

    @Test
    fun summaryOpensDetailedStatisticsForPersistedMonth() = runEmptyComposeUiTest {
        enableAccessibilityChecks()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("month-summary").assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag("month-summary").performClick()

            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("month-statistics").assertExists()
                    true
                }.getOrDefault(false)
            }

            onRoot().tryPerformAccessibilityChecks()
            onNodeWithTag("statistics-average-shift-value")
                .assertIsDisplayed()
                .assertTextEquals("9 ч")
            onNodeWithTag("statistics-longest-shift-value")
                .assertTextEquals("10 ч")
            onNodeWithTag("statistics-work-days-value")
                .assertTextEquals("2")
            onNodeWithTag("statistics-vacation-days-value")
                .assertTextEquals("1")
        }
    }
}
