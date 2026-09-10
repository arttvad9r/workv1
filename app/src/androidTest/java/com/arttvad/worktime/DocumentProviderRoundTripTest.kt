package com.arttvad.worktime

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runEmptyComposeUiTest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.ActivityResult
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arttvad.worktime.data.repository.WorkDayRepository
import com.arttvad.worktime.domain.model.WorkDay
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DocumentProviderRoundTripTest {
    private val csvUri = DocumentRoundTripProvider.uri("month.csv")
    private val xlsxUri = DocumentRoundTripProvider.uri("month.xlsx")
    private val pdfUri = DocumentRoundTripProvider.uri("month.pdf")
    private val backupUri = DocumentRoundTripProvider.uri("backup.wtbk")
    private val restoreDate = LocalDate.of(2099, 12, 31)

    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val application: WorkTimeApplication
        get() = targetContext.applicationContext as WorkTimeApplication

    private val repository: WorkDayRepository
        get() = application.container.workDayRepository

    @Before
    fun setUp() {
        Intents.init()
        listOf(csvUri, xlsxUri, pdfUri, backupUri).forEach { uri ->
            runCatching { targetContext.contentResolver.delete(uri, null, null) }
        }
        runBlocking { repository.delete(restoreDate) }
    }

    @After
    fun tearDown() {
        runBlocking { repository.delete(restoreDate) }
        listOf(csvUri, xlsxUri, pdfUri, backupUri).forEach { uri ->
            runCatching { targetContext.contentResolver.delete(uri, null, null) }
        }
        Intents.release()
    }

    @Test
    fun monthExportsWriteThroughReturnedDocumentProviderUris() = runEmptyComposeUiTest {
        val month = YearMonth.now()
        stubCreateDocument("text/csv", csvUri)
        stubCreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            xlsxUri,
        )
        stubCreateDocument("application/pdf", pdfUri)

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForNode("month-summary")
            onNodeWithTag("month-summary").performClick()
            waitForNode("month-statistics")

            onNodeWithTag("statistics-export-csv")
                .performScrollTo()
                .performClick()
            waitForBytes(csvUri)

            onNodeWithTag("statistics-export-xlsx")
                .performScrollTo()
                .performClick()
            waitForBytes(xlsxUri)

            onNodeWithTag("statistics-export-pdf")
                .performScrollTo()
                .performClick()
            waitForBytes(pdfUri)
        }

        val csv = readBytes(csvUri)
        val xlsx = readBytes(xlsxUri)
        val pdf = readBytes(pdfUri)

        assertTrue(
            String(csv, StandardCharsets.UTF_8).startsWith(
                "record_type,period,currency,date,day_type,worked_minutes,",
            ),
        )
        assertArrayEquals(byteArrayOf('P'.code.toByte(), 'K'.code.toByte()), xlsx.copyOf(2))
        assertTrue(String(pdf.copyOfRange(0, 4), StandardCharsets.US_ASCII) == "%PDF")

        intended(
            allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasType("text/csv"),
                hasExtra(Intent.EXTRA_TITLE, "worktime-$month.csv"),
            ),
        )
        intended(
            allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                hasExtra(Intent.EXTRA_TITLE, "worktime-$month.xlsx"),
            ),
        )
        intended(
            allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasType("application/pdf"),
                hasExtra(Intent.EXTRA_TITLE, "worktime-$month.pdf"),
            ),
        )
    }

    @Test
    fun backupCreateAndRestoreRoundTripThroughDocumentProviderUri() = runEmptyComposeUiTest {
        runBlocking {
            repository.upsert(
                WorkDay(
                    date = restoreDate,
                    workedMinutes = 480,
                    overtimeMinutes = 30,
                    note = "document-provider-roundtrip",
                    updatedAtEpochMillis = 123L,
                ),
            )
        }

        stubCreateDocument("application/octet-stream", backupUri)
        intending(
            allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("*/*"),
            ),
        ).respondWith(
            ActivityResult(Activity.RESULT_OK, Intent().setData(backupUri)),
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            val settings = targetContext.getString(R.string.settings)
            onNodeWithContentDescription(settings).performClick()
            waitForNode("settings-data-open")
            onNodeWithTag("settings-data-open").performClick()
            waitForNode("settings-backup-create")

            onNodeWithTag("settings-backup-create")
                .performScrollTo()
                .performClick()
            waitForBytes(backupUri)

            val backupBytes = readBytes(backupUri)
            assertArrayEquals(
                byteArrayOf(0x57, 0x54, 0x42, 0x4B),
                backupBytes.copyOf(4),
            )

            runBlocking { repository.delete(restoreDate) }
            waitUntil(timeoutMillis = 5_000) {
                runBlocking { repository.snapshotAll().none { day -> day.date == restoreDate } }
            }

            onNodeWithTag("settings-backup-restore")
                .performScrollTo()
                .performClick()
            waitForNode("backup-restore-confirm")
            onNodeWithTag("backup-restore-confirm").performClick()

            waitUntil(timeoutMillis = 10_000) {
                runBlocking {
                    repository.snapshotAll().any { day ->
                        day.date == restoreDate &&
                            day.workedMinutes == 480 &&
                            day.overtimeMinutes == 30 &&
                            day.note == "document-provider-roundtrip"
                    }
                }
            }
        }

        intended(
            allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasType("application/octet-stream"),
            ),
        )
        intended(
            allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("*/*"),
            ),
        )
    }

    private fun stubCreateDocument(mimeType: String, uri: Uri) {
        intending(
            allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasType(mimeType),
            ),
        ).respondWith(
            ActivityResult(Activity.RESULT_OK, Intent().setData(uri)),
        )
    }

    private fun readBytes(uri: Uri): ByteArray = targetContext.contentResolver
        .openInputStream(uri)
        ?.use { input -> input.readBytes() }
        ?: error("Document provider returned no input stream for $uri")

    private fun androidx.compose.ui.test.ComposeUiTest.waitForBytes(uri: Uri) {
        waitUntil(timeoutMillis = 10_000) {
            runCatching { readBytes(uri).isNotEmpty() }.getOrDefault(false)
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitForNode(tag: String) {
        waitUntil(timeoutMillis = 5_000) {
            runCatching {
                onNodeWithTag(tag).assertExists()
                true
            }.getOrDefault(false)
        }
    }
}
