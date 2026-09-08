package com.arttvad.worktime.exporting

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.arttvad.worktime.domain.exporting.MonthReportData
import com.arttvad.worktime.domain.exporting.MonthReportDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.io.OutputStream
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

object MonthPdfExporter {
    private const val PageWidth = 595
    private const val PageHeight = 842
    private const val Margin = 32f
    private const val TableTop = 154f
    private const val RowHeight = 18f

    fun write(output: OutputStream, report: MonthReportData) {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PageWidth, PageHeight, 1).create()
            val page = document.startPage(pageInfo)
            drawReport(page.canvas, report)
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun drawReport(canvas: Canvas, report: MonthReportData) {
        val titlePaint = textPaint(size = 19f, bold = true)
        val subtitlePaint = textPaint(size = 11f)
        val sectionPaint = textPaint(size = 10f, bold = true)
        val bodyPaint = textPaint(size = 8.5f)
        val mutedPaint = textPaint(size = 8.5f, color = Color.DKGRAY)
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        canvas.drawText("Рабочее время", Margin, 40f, titlePaint)
        canvas.drawText(report.month.toString(), Margin, 60f, subtitlePaint)

        val statistics = report.statistics
        canvas.drawText(
            "Отработано: ${formatDuration(statistics.workedMinutes)}   Переработка: ${formatDuration(statistics.overtimeMinutes)}",
            Margin,
            84f,
            subtitlePaint,
        )
        canvas.drawText(
            "Заработано: ${formatMoney(statistics.earningsMinor, report.currencyCode)}",
            Margin,
            102f,
            subtitlePaint,
        )
        canvas.drawText(
            "Работа: ${statistics.workDays}   Выходной: ${statistics.daysOff}   Отпуск: ${statistics.vacationDays}   Больничный: ${statistics.sickDays}",
            Margin,
            120f,
            mutedPaint,
        )

        canvas.drawLine(Margin, 134f, PageWidth - Margin, 134f, dividerPaint)

        val dateX = Margin
        val typeX = 88f
        val workedX = 165f
        val overtimeX = 225f
        val rateX = 286f
        val earningsX = 348f
        val noteX = 424f

        canvas.drawText("Дата", dateX, TableTop, sectionPaint)
        canvas.drawText("Тип", typeX, TableTop, sectionPaint)
        canvas.drawText("Время", workedX, TableTop, sectionPaint)
        canvas.drawText("Сверх", overtimeX, TableTop, sectionPaint)
        canvas.drawText("Ставка", rateX, TableTop, sectionPaint)
        canvas.drawText("Доход", earningsX, TableTop, sectionPaint)
        canvas.drawText("Заметка", noteX, TableTop, sectionPaint)
        canvas.drawLine(Margin, TableTop + 5f, PageWidth - Margin, TableTop + 5f, dividerPaint)

        if (report.days.isEmpty()) {
            canvas.drawText("Нет записей", Margin, TableTop + RowHeight + 3f, mutedPaint)
        } else {
            report.days.forEachIndexed { index, day ->
                val baseline = TableTop + RowHeight * (index + 1) + 3f
                drawDayRow(
                    canvas = canvas,
                    day = day,
                    currencyCode = report.currencyCode,
                    baseline = baseline,
                    bodyPaint = bodyPaint,
                    mutedPaint = mutedPaint,
                    dateX = dateX,
                    typeX = typeX,
                    workedX = workedX,
                    overtimeX = overtimeX,
                    rateX = rateX,
                    earningsX = earningsX,
                    noteX = noteX,
                )
                canvas.drawLine(
                    Margin,
                    baseline + 5f,
                    PageWidth - Margin,
                    baseline + 5f,
                    dividerPaint,
                )
            }
        }

        canvas.drawText(
            "WorkTime · ${report.currencyCode}",
            Margin,
            PageHeight - 22f,
            mutedPaint,
        )
    }

    private fun drawDayRow(
        canvas: Canvas,
        day: MonthReportDay,
        currencyCode: String,
        baseline: Float,
        bodyPaint: Paint,
        mutedPaint: Paint,
        dateX: Float,
        typeX: Float,
        workedX: Float,
        overtimeX: Float,
        rateX: Float,
        earningsX: Float,
        noteX: Float,
    ) {
        canvas.drawText(day.date.toString().substring(5), dateX, baseline, bodyPaint)
        canvas.drawText(day.type.displayName(), typeX, baseline, bodyPaint)
        canvas.drawText(formatClockDuration(day.workedMinutes), workedX, baseline, bodyPaint)
        canvas.drawText(formatClockDuration(day.overtimeMinutes), overtimeX, baseline, bodyPaint)
        canvas.drawText(
            formatMoney(day.effectiveHourlyRateMinor, currencyCode),
            rateX,
            baseline,
            if (day.effectiveHourlyRateMinor == null) mutedPaint else bodyPaint,
        )
        canvas.drawText(
            formatMoney(day.earningsMinor, currencyCode),
            earningsX,
            baseline,
            if (day.earningsMinor == null) mutedPaint else bodyPaint,
        )
        canvas.drawText(
            ellipsize(day.note, bodyPaint, PageWidth - Margin - noteX),
            noteX,
            baseline,
            mutedPaint,
        )
    }

    private fun textPaint(
        size: Float,
        bold: Boolean = false,
        color: Int = Color.BLACK,
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    private fun formatDuration(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes == 0) "$hours ч" else "$hours ч $minutes мин"
    }

    private fun formatClockDuration(totalMinutes: Int): String =
        "%d:%02d".format(Locale.ROOT, totalMinutes / 60, totalMinutes % 60)

    private fun formatMoney(amountMinor: Long?, currencyCode: String): String {
        if (amountMinor == null) return "—"
        val currency = Currency.getInstance(currencyCode)
        val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
        val value = BigDecimal.valueOf(amountMinor, fractionDigits)
            .setScale(fractionDigits)
            .toPlainString()
        return "$value ${currency.currencyCode}"
    }

    private fun WorkDayType.displayName(): String = when (this) {
        WorkDayType.WORK -> "Работа"
        WorkDayType.DAY_OFF -> "Выходной"
        WorkDayType.VACATION -> "Отпуск"
        WorkDayType.SICK -> "Больничный"
    }

    private fun ellipsize(value: String, paint: Paint, maxWidth: Float): String {
        val normalized = value.replace(Regex("\\s+"), " ").trim()
        if (normalized.isEmpty() || paint.measureText(normalized) <= maxWidth) return normalized
        val ellipsis = "…"
        val available = (maxWidth - paint.measureText(ellipsis)).coerceAtLeast(0f)
        val count = paint.breakText(normalized, true, available, null)
        return normalized.take(count.coerceAtLeast(0)) + ellipsis
    }
}
