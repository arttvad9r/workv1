package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.exporting.MonthReportDataBuilder
import com.arttvad.worktime.domain.exporting.MonthXlsxExporter
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.YearMonth
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthXlsxExporterTest {
    @Test
    fun generatedWorkbookOpensWithApachePoiAndPreservesValues() {
        val report = MonthReportDataBuilder.build(
            month = YearMonth.of(2026, 9),
            days = listOf(
                WorkDay(
                    date = LocalDate.of(2026, 10, 1),
                    workedMinutes = 480,
                    overtimeMinutes = 0,
                    note = "outside",
                    updatedAtEpochMillis = 3L,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 9, 2),
                    workedMinutes = 0,
                    overtimeMinutes = 0,
                    note = "Отпуск",
                    updatedAtEpochMillis = 2L,
                    type = WorkDayType.VACATION,
                ),
                WorkDay(
                    date = LocalDate.of(2026, 9, 1),
                    workedMinutes = 480,
                    overtimeMinutes = 60,
                    note = "A&B <note>",
                    updatedAtEpochMillis = 1L,
                    hourlyRateOverrideMinor = 2_000L,
                ),
            ),
            hourlyRateMinor = 1_500L,
            currencyCode = "EUR",
        )
        val output = ByteArrayOutputStream()
        MonthXlsxExporter.write(output, report)
        val bytes = output.toByteArray()

        assertTrue(bytes.size > 1_000)
        assertEquals('P'.code.toByte(), bytes[0])
        assertEquals('K'.code.toByte(), bytes[1])

        XSSFWorkbook(ByteArrayInputStream(bytes)).use { workbook ->
            assertEquals(1, workbook.numberOfSheets)
            val sheet = workbook.getSheet("Месяц")
            assertNotNull(sheet)
            requireNotNull(sheet)

            assertEquals("WorkTime", sheet.getRow(0).getCell(0).stringCellValue)
            assertEquals("2026-09", sheet.getRow(0).getCell(1).stringCellValue)
            assertEquals("EUR", sheet.getRow(0).getCell(2).stringCellValue)

            assertEquals(480.0, sheet.getRow(1).getCell(1).numericCellValue, 0.0)
            assertEquals(60.0, sheet.getRow(1).getCell(3).numericCellValue, 0.0)
            assertEquals(16.0, sheet.getRow(1).getCell(5).numericCellValue, 0.0)

            val workRow = sheet.getRow(4)
            assertEquals("2026-09-01", workRow.getCell(0).stringCellValue)
            assertEquals("Работа", workRow.getCell(1).stringCellValue)
            assertEquals(480.0, workRow.getCell(2).numericCellValue, 0.0)
            assertEquals(60.0, workRow.getCell(3).numericCellValue, 0.0)
            assertEquals(20.0, workRow.getCell(4).numericCellValue, 0.0)
            assertEquals(16.0, workRow.getCell(5).numericCellValue, 0.0)
            assertEquals("A&B <note>", workRow.getCell(6).stringCellValue)

            val vacationRow = sheet.getRow(5)
            assertEquals("2026-09-02", vacationRow.getCell(0).stringCellValue)
            assertEquals("Отпуск", vacationRow.getCell(1).stringCellValue)
            assertEquals(0.0, vacationRow.getCell(2).numericCellValue, 0.0)
            assertEquals(5, sheet.lastRowNum)
        }
    }
}
