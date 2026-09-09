package com.arttvad.worktime

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SystemBackFlowTest {
    @Test
    fun backClosesSearchAndDayEditorWithoutResettingVisibleMonth() = runEmptyComposeUiTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nextMonthLabel = context.getString(R.string.next_month)
        val nextMonth = YearMonth.now().plusMonths(1)
        val nextMonthDayTag = "day-${nextMonth.atDay(1)}"

        ActivityScenario.launch(MainActivity::class.java).use {
            onNodeWithContentDescription(nextMonthLabel).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag(nextMonthDayTag).assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag("calendar-search-open").performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("calendar-search-query").assertExists()
                    true
                }.getOrDefault(false)
            }

            pressBack()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("calendar-search-query").fetchSemanticsNodes().isEmpty()
            }
            onNodeWithTag(nextMonthDayTag).assertExists()

            onNodeWithTag(nextMonthDayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-worked-hours").assertExists()
                    true
                }.getOrDefault(false)
            }

            pressBack()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("day-editor-worked-hours").fetchSemanticsNodes().isEmpty()
            }
            onNodeWithTag(nextMonthDayTag).assertExists()
        }
    }

    @Test
    fun backFromRootFinishesActivity() = runEmptyComposeUiTest {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("calendar-search-open").assertExists()
                    true
                }.getOrDefault(false)
            }

            pressBack()
            waitUntil(timeoutMillis = 5_000) {
                scenario.state == Lifecycle.State.DESTROYED
            }
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }
}
