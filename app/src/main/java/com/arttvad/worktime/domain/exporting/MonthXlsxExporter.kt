package com.arttvad.worktime.domain.exporting

import com.arttvad.worktime.domain.model.WorkDayType
import java.io.FilterOutputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.Currency
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object MonthXlsxExporter {
    fun write(output: OutputStream, report: MonthReportData) {
        ZipOutputStream(NonClosingOutputStream(output)).use { zip ->
            zip.writeTextEntry("[Content_Types].xml", contentTypesXml())
            zip.writeTextEntry("_rels/.rels", rootRelationshipsXml())
            zip.writeTextEntry("xl/workbook.xml", workbookXml())
            zip.writeTextEntry("xl/_rels/workbook.xml.rels", workbookRelationshipsXml())
            zip.writeTextEntry("xl/styles.xml", stylesXml(report.currencyCode))
            zip.writeTextEntry("xl/worksheets/sheet1.xml", worksheetXml(report))
        }
    }

    private fun contentTypesXml(): String = xmlDocument(
        """
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
        """.trimIndent(),
    )

    private fun rootRelationshipsXml(): String = xmlDocument(
        """
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
        """.trimIndent(),
    )

    private fun workbookXml(): String = xmlDocument(
        """
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Месяц" sheetId="1" r:id="rId1"/>
          </sheets>
        </workbook>
        """.trimIndent(),
    )

    private fun workbookRelationshipsXml(): String = xmlDocument(
        """
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
        """.trimIndent(),
    )

    private fun stylesXml(currencyCode: String): String {
        val fractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
        val numberFormat = if (fractionDigits == 0) "0" else "0.${"0".repeat(fractionDigits)}"
        return xmlDocument(
            """
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <numFmts count="1"><numFmt numFmtId="164" formatCode="$numberFormat"/></numFmts>
              <fonts count="2">
                <font><sz val="11"/><name val="Calibri"/><family val="2"/></font>
                <font><b/><sz val="11"/><name val="Calibri"/><family val="2"/></font>
              </fonts>
              <fills count="2">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
              </fills>
              <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
              <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
              <cellXfs count="3">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
              </cellXfs>
              <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
            </styleSheet>
            """.trimIndent(),
        )
    }

    private fun worksheetXml(report: MonthReportData): String {
        val rows = buildString {
            append(row(1, listOf(
                inlineCell("A1", "WorkTime", style = 1),
                inlineCell("B1", report.month.toString()),
                inlineCell("C1", report.currencyCode),
            )))
            append(row(2, buildList {
                add(inlineCell("A2", "Отработано, мин", style = 1))
                add(numberCell("B2", report.statistics.workedMinutes.toString()))
                add(inlineCell("C2", "Переработка, мин", style = 1))
                add(numberCell("D2", report.statistics.overtimeMinutes.toString()))
                add(inlineCell("E2", "Заработано", style = 1))
                report.statistics.earningsMinor?.let { earnings ->
                    add(numberCell("F2", formatMinor(earnings, report.currencyCode), style = 2))
                }
            }))
            append(row(4, listOf(
                inlineCell("A4", "Дата", style = 1),
                inlineCell("B4", "Тип", style = 1),
                inlineCell("C4", "Отработано, мин", style = 1),
                inlineCell("D4", "Переработка, мин", style = 1),
                inlineCell("E4", "Эффективная ставка", style = 1),
                inlineCell("F4", "Заработано", style = 1),
                inlineCell("G4", "Заметка", style = 1),
            )))
            report.days.forEachIndexed { index, day ->
                val rowNumber = index + 5
                append(row(rowNumber, buildList {
                    add(inlineCell("A$rowNumber", day.date.toString()))
                    add(inlineCell("B$rowNumber", day.type.displayName()))
                    add(numberCell("C$rowNumber", day.workedMinutes.toString()))
                    add(numberCell("D$rowNumber", day.overtimeMinutes.toString()))
                    day.effectiveHourlyRateMinor?.let { rate ->
                        add(numberCell("E$rowNumber", formatMinor(rate, report.currencyCode), style = 2))
                    }
                    day.earningsMinor?.let { earnings ->
                        add(numberCell("F$rowNumber", formatMinor(earnings, report.currencyCode), style = 2))
                    }
                    add(inlineCell("G$rowNumber", day.note))
                }))
            }
        }
        val lastRow = (report.days.size + 4).coerceAtLeast(4)

        return xmlDocument(
            """
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <cols>
                <col min="1" max="1" width="12" customWidth="1"/>
                <col min="2" max="2" width="14" customWidth="1"/>
                <col min="3" max="4" width="18" customWidth="1"/>
                <col min="5" max="6" width="22" customWidth="1"/>
                <col min="7" max="7" width="40" customWidth="1"/>
              </cols>
              <sheetData>$rows</sheetData>
              <autoFilter ref="A4:G$lastRow"/>
            </worksheet>
            """.trimIndent(),
        )
    }

    private fun row(number: Int, cells: List<String>): String =
        "<row r=\"$number\">${cells.joinToString(separator = "")}</row>"

    private fun inlineCell(reference: String, value: String, style: Int? = null): String {
        val styleAttribute = style?.let { " s=\"$it\"" }.orEmpty()
        return "<c r=\"$reference\" t=\"inlineStr\"$styleAttribute><is><t xml:space=\"preserve\">${escapeXml(value)}</t></is></c>"
    }

    private fun numberCell(reference: String, value: String, style: Int? = null): String {
        val styleAttribute = style?.let { " s=\"$it\"" }.orEmpty()
        return "<c r=\"$reference\"$styleAttribute><v>$value</v></c>"
    }

    private fun formatMinor(amountMinor: Long, currencyCode: String): String {
        val fractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
        return BigDecimal.valueOf(amountMinor, fractionDigits)
            .setScale(fractionDigits)
            .toPlainString()
    }

    private fun WorkDayType.displayName(): String = when (this) {
        WorkDayType.WORK -> "Работа"
        WorkDayType.DAY_OFF -> "Выходной"
        WorkDayType.VACATION -> "Отпуск"
        WorkDayType.SICK -> "Больничный"
    }

    private fun escapeXml(value: String): String = buildString(value.length) {
        value.forEach { character ->
            append(
                when (character) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&apos;"
                    else -> character
                },
            )
        }
    }

    private fun xmlDocument(body: String): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>$body"

    private fun ZipOutputStream.writeTextEntry(name: String, content: String) {
        val entry = ZipEntry(name).apply { time = 0L }
        putNextEntry(entry)
        write(content.toByteArray(StandardCharsets.UTF_8))
        closeEntry()
    }

    private class NonClosingOutputStream(output: OutputStream) : FilterOutputStream(output) {
        override fun close() {
            flush()
        }
    }
}
