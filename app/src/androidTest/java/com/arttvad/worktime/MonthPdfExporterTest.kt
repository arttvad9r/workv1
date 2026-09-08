package com.arttvad.worktime

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arttvad.worktime.domain.exporting.MonthReportDataBuilder
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import com.arttvad.worktime.exporting.MonthPdfExporter
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonthPdfExporterTest {
    @Test
    fun writesReadableSinglePagePdf() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "worktime-report-${System.nanoTime()}.pdf")
        val report = MonthReportDataBuilder.build(
            month = YearMonth.of(2026, 9),
            days = listOf(
                WorkDay(
                    date = LocalDate.of(2026, 9, 1),
                    workedMinutes = 480,
                    overtimeMinutes = 60,
                    note = "Обычная смена с длинной заметкой, которая должна безопасно уместиться в строке отчёта",
                    updatedAtEpochMillis = 1L,
                    hourlyRateOverrideMinor = 2_000L,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 9, 2),
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "Отпуск",
                    updatedAtEpochMillis = 2L,
                    type = WorkDayType.VACATION,
                ),
            ),
            hourlyRateMinor = 1_500L,
            currencyCode = "EUR",
        )

        try {
            file.outputStream().use { output -> MonthPdfExporter.write(output, report) }
            assertTrue(file.length() > 1_000L)
            val header = ByteArray(5)
            file.inputStream().use { input ->
                assertEquals(header.size, input.read(header))
            }
            assertEquals("%PDF-", header.toString(Charsets.US_ASCII))

            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    assertEquals(1, renderer.pageCount)
                    val page = renderer.openPage(0)
                    try {
                        assertEquals(595, page.width)
                        assertEquals(842, page.height)
                    } finally {
                        page.close()
                    }
                }
            }
        } finally {
            file.delete()
        }
    }
}
