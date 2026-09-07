package com.arttvad.worktime.ui.calendar

import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal val PagerStartMonth: YearMonth = YearMonth.of(1900, 1)
internal const val PagerMonthCount = 3_600

internal fun pageForMonth(month: YearMonth): Int =
    ChronoUnit.MONTHS.between(PagerStartMonth, month)
        .toInt()
        .coerceIn(0, PagerMonthCount - 1)

internal fun monthForPage(page: Int): YearMonth = PagerStartMonth.plusMonths(page.toLong())
