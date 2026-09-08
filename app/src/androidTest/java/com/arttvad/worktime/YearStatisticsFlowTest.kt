package com.arttvad.worktime

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arttvad.worktime.data.repository.WorkDayRepository
import com.arttvad.worktime.domain.model.WorkDay
import java.time.LocalDate
import java.time.Year
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class YearStatisticsFlowTest {
    private val year = Year.now()
    private val januaryDate = LocalDate.of(year.value, 1, 24)
    private val februaryDate = LocalDate.of(year.value, 2, 24)
    private val testDates = listOf(januaryDate, februaryDate)

    private val application: WorkTimeApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as WorkTimeApplication

    private val repository: WorkDayRepository
        get() = application.container.workDayRepository

    @Before
    fun seedYearDays() {
        runBlocking {
            testDates.forEach { repository.delete(it) }
            repository.upsert(
                WorkDay(
                    date = januaryDate,
                    workedMinutes = 480,
                    overtimeMinutes = 60,
                    note = "",
                    updatedAtEpochMillis = 1L,
                ),
            )
            repository.upsert(
                WorkDay(
                    date = februaryDate,
                    workedMinutes = 600,
                    overtimeMinutes = 0,
                    note = "",
                    updatedAtEpochMillis = 2L,
                ),
            )
        }
    }

    @After
    fun cleanupYearDays() {
        runBlocking { testDates.forEach { repository.delete(it) } }
    }

    @Test
    fun yearScopeReadsEntriesOutsideVisibleMonth() = runEmptyComposeUiTest {
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
                    onNodeWithTag("statistics-scope-year").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("statistics-scope-year").performClick()

            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("statistics-year-worked-value")
                        .assertTextEquals("18 ч")
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("statistics-year-work-days-value")
                .assertTextEquals("2")
            onNodeWithTag("statistics-year-month-12")
                .performScrollTo()
                .assertExists()
        }
    }
}
