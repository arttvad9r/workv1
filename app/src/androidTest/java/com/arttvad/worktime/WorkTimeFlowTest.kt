package com.arttvad.worktime

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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

    private companion object {
        const val PERSISTENCE_TIMEOUT_MILLIS = 10_000L
    }

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
    fun paymentSettingsAreAccessibleAndPersistAcrossRelaunch() = runEmptyComposeUiTest {
        enableAccessibilityChecks()
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsLabel = targetContext.getString(R.string.settings)
        val originalPayment = currentPayment()

        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                onNodeWithContentDescription(settingsLabel).performClick()
                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("settings-hourly-rate").assertExists()
                        true
                    }.getOrDefault(false)
                }
                onRoot().tryPerformAccessibilityChecks()

                onNodeWithTag("settings-hourly-rate").performTextReplacement("12.50")
                onNodeWithTag("settings-currency").performTextReplacement("EUR")
                onNodeWithTag("settings-save").performScrollTo().performClick()

                waitUntil(timeoutMillis = PERSISTENCE_TIMEOUT_MILLIS) {
                    currentPayment() == (1_250L to "EUR")
                }
            }

            ActivityScenario.launch(MainActivity::class.java).use {
                onNodeWithContentDescription(settingsLabel).performClick()
                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("settings-currency").assertTextContains("EUR")
                        true
                    }.getOrDefault(false)
                }
                onNodeWithTag("settings-hourly-rate").assertTextContains("12.5")
            }
        } finally {
            updateActiveProfilePayment(originalPayment.first, originalPayment.second)
        }
    }

    @Test
    fun dayRateOverridePersistsAcrossRelaunch() = runEmptyComposeUiTest {
        val originalPayment = currentPayment()

        try {
            updateActiveProfilePayment(originalPayment.first, "EUR")

            ActivityScenario.launch(MainActivity::class.java).use {
                onNodeWithTag(dayTag).performClick()
                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("day-editor-worked-hours").assertExists()
                        true
                    }.getOrDefault(false)
                }
                onNodeWithTag("day-editor-worked-hours").performTextInput("8")
                onNodeWithTag("day-editor-add-rate-override").performScrollTo().performClick()
                onNodeWithTag("day-editor-rate-override").performScrollTo().performTextInput("20")
                onNodeWithTag("day-editor-save").performScrollTo().performClick()

                waitUntil(timeoutMillis = PERSISTENCE_TIMEOUT_MILLIS) {
                    currentEntry()?.hourlyRateOverrideMinor == 2_000L
                }
            }

            ActivityScenario.launch(MainActivity::class.java).use {
                onNodeWithTag(dayTag).performClick()
                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("day-editor-rate-override").assertExists()
                        true
                    }.getOrDefault(false)
                }
                onNodeWithTag("day-editor-rate-override").assertTextContains("20")
            }
        } finally {
            updateActiveProfilePayment(originalPayment.first, originalPayment.second)
        }
    }

    @Test
    fun workDurationPresetSavesTwelveHourDay() = runEmptyComposeUiTest {
        ActivityScenario.launch(MainActivity::class.java).use {
            onNodeWithTag(dayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-preset-12").assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag("day-editor-preset-12").performScrollTo().performClick()
            onNodeWithTag("day-editor-worked-hours").assertTextContains("12")
            onNodeWithTag("day-editor-save").performScrollTo().performClick()

            waitUntil(timeoutMillis = PERSISTENCE_TIMEOUT_MILLIS) {
                currentEntry()?.workedMinutes == 12 * 60
            }
        }
    }

    @Test
    fun dayOffPersistsAcrossRelaunch() = runEmptyComposeUiTest {
        ActivityScenario.launch(MainActivity::class.java).use {
            onNodeWithTag(dayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-type-day_off").assertExists()
                    true
                }.getOrDefault(false)
            }

            onNodeWithTag("day-editor-type-day_off").performScrollTo().performClick()
            onNodeWithTag("day-editor-save").performScrollTo().performClick()

            waitUntil(timeoutMillis = PERSISTENCE_TIMEOUT_MILLIS) {
                currentEntry()?.let { entry ->
                    entry.type == WorkDayType.DAY_OFF &&
                        entry.workedMinutes == 0 &&
                        entry.overtimeMinutes == 0 &&
                        entry.hourlyRateOverrideMinor == null
                } == true
            }
        }

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
                    onNodeWithTag("day-editor-type-day_off").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("day-editor-type-day_off").assertIsSelected()
        }
    }

    @Test
    fun saveEditDeleteUndoAndRelaunch() = runEmptyComposeUiTest {
        val undoLabel = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.undo)

        ActivityScenario.launch(MainActivity::class.java).use {
            onNodeWithTag(dayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-worked-hours").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("day-editor-worked-hours").performTextInput("8")
            onNodeWithTag("day-editor-worked-minutes").performTextInput("30")
            onNodeWithTag("day-editor-save").performScrollTo().performClick()

            waitUntil(timeoutMillis = PERSISTENCE_TIMEOUT_MILLIS) {
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
            onNodeWithTag("day-editor-save").performScrollTo().performClick()

            waitUntil(timeoutMillis = PERSISTENCE_TIMEOUT_MILLIS) {
                currentEntry()?.workedMinutes == 9 * 60
            }

            onNodeWithTag(dayTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-delete").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("day-editor-delete").performScrollTo().performClick()

            waitUntil(timeoutMillis = PERSISTENCE_TIMEOUT_MILLIS) { currentEntry() == null }
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithText(undoLabel).assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithText(undoLabel).performClick()
            waitUntil(timeoutMillis = PERSISTENCE_TIMEOUT_MILLIS) {
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
            waitUntil(timeoutMillis = 5_000) {
                runCatching {
                    onNodeWithTag("day-editor-worked-hours").assertExists()
                    true
                }.getOrDefault(false)
            }
            onNodeWithTag("day-editor-worked-hours").performTextInput("7")
            onNodeWithTag("day-editor-worked-minutes").performTextInput("45")
            onNodeWithTag("day-editor-add-note").performScrollTo().performClick()
            onNodeWithTag("day-editor-note").performScrollTo().performTextInput("черновик")

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

    private fun currentPayment(): Pair<Long?, String> = runBlocking {
        val preferences = application.container.preferencesRepository.preferences.first()
        val profile = application.container.workProfileRepository.observeProfiles()
            .first()
            .first { it.id == preferences.activeProfileId }
        (profile.hourlyRateMinor ?: preferences.hourlyRateMinor) to
            (profile.currencyCode ?: preferences.currencyCode)
    }

    private fun updateActiveProfilePayment(hourlyRateMinor: Long?, currencyCode: String) {
        runBlocking {
            val profileId = application.container.preferencesRepository.preferences.first().activeProfileId
            application.container.workProfileRepository.updatePayment(
                profileId = profileId,
                hourlyRateMinor = hourlyRateMinor,
                currencyCode = currencyCode,
            )
        }
    }
}
