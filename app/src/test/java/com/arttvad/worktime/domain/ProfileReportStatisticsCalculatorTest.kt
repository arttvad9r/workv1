package com.arttvad.worktime.domain

import com.arttvad.worktime.domain.calculation.CurrencyEarningsStatistics
import com.arttvad.worktime.domain.calculation.DetailedMonthStatistics
import com.arttvad.worktime.domain.calculation.DetailedYearStatistics
import com.arttvad.worktime.domain.calculation.ProfilePeriodStatistics
import com.arttvad.worktime.domain.calculation.ProfileReportStatisticsCalculator
import com.arttvad.worktime.domain.calculation.YearMonthStatistics
import java.time.Year
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileReportStatisticsCalculatorTest {
    private val year = Year.of(2026)

    @Test
    fun combinedMonthAggregatesTimeAndKeepsCurrenciesSeparate() {
        val reports = listOf(
            report(
                id = 1L,
                name = "Основная работа",
                currency = "EUR",
                month = DetailedMonthStatistics(
                    workDays = 2,
                    daysOff = 1,
                    workedMinutes = 960,
                    overtimeMinutes = 60,
                    averageWorkedMinutes = 480,
                    longestWorkedMinutes = 540,
                    earningsMinor = 24_000L,
                ),
            ),
            report(
                id = 2L,
                name = "Подработка",
                currency = "USD",
                month = DetailedMonthStatistics(
                    workDays = 1,
                    vacationDays = 1,
                    workedMinutes = 600,
                    overtimeMinutes = 30,
                    averageWorkedMinutes = 600,
                    longestWorkedMinutes = 600,
                    earningsMinor = 30_000L,
                ),
            ),
        )

        val combined = ProfileReportStatisticsCalculator.combineMonth(reports)

        assertEquals(3, combined.statistics.workDays)
        assertEquals(1, combined.statistics.daysOff)
        assertEquals(1, combined.statistics.vacationDays)
        assertEquals(1_560, combined.statistics.workedMinutes)
        assertEquals(90, combined.statistics.overtimeMinutes)
        assertEquals(520, combined.statistics.averageWorkedMinutes)
        assertEquals(600, combined.statistics.longestWorkedMinutes)
        assertNull(combined.statistics.earningsMinor)
        assertEquals(
            listOf(
                CurrencyEarningsStatistics("EUR", 24_000L),
                CurrencyEarningsStatistics("USD", 30_000L),
            ),
            combined.earningsByCurrency,
        )
    }

    @Test
    fun missingRateMakesOnlyItsCurrencyGroupUnknown() {
        val reports = listOf(
            report(
                id = 1L,
                name = "A",
                currency = "EUR",
                month = DetailedMonthStatistics(workDays = 1, workedMinutes = 480, earningsMinor = 10_000L),
            ),
            report(
                id = 2L,
                name = "B",
                currency = "EUR",
                month = DetailedMonthStatistics(workDays = 1, workedMinutes = 480, earningsMinor = null),
            ),
            report(
                id = 3L,
                name = "C",
                currency = "USD",
                month = DetailedMonthStatistics(workDays = 1, workedMinutes = 480, earningsMinor = 20_000L),
            ),
        )

        val earnings = ProfileReportStatisticsCalculator.combineMonth(reports).earningsByCurrency

        assertEquals(2, earnings.size)
        assertEquals("EUR", earnings[0].currencyCode)
        assertNull(earnings[0].earningsMinor)
        assertEquals(CurrencyEarningsStatistics("USD", 20_000L), earnings[1])
    }

    @Test
    fun combinedYearAggregatesEachMonthAcrossProfiles() {
        val january = YearMonth.of(2026, 1)
        val first = report(
            id = 1L,
            name = "A",
            currency = "EUR",
            month = DetailedMonthStatistics(),
            yearStatistics = DetailedYearStatistics(
                year = year,
                totals = DetailedMonthStatistics(
                    workDays = 1,
                    workedMinutes = 480,
                    earningsMinor = 12_000L,
                ),
                months = listOf(
                    YearMonthStatistics(
                        month = january,
                        workedMinutes = 480,
                        workDays = 1,
                        earningsMinor = 12_000L,
                    ),
                ),
            ),
        )
        val second = report(
            id = 2L,
            name = "B",
            currency = "USD",
            month = DetailedMonthStatistics(),
            yearStatistics = DetailedYearStatistics(
                year = year,
                totals = DetailedMonthStatistics(
                    workDays = 1,
                    workedMinutes = 600,
                    overtimeMinutes = 60,
                    earningsMinor = 30_000L,
                ),
                months = listOf(
                    YearMonthStatistics(
                        month = january,
                        workedMinutes = 600,
                        overtimeMinutes = 60,
                        workDays = 1,
                        earningsMinor = 30_000L,
                    ),
                ),
            ),
        )

        val combined = ProfileReportStatisticsCalculator.combineYear(year, listOf(first, second))
        val januaryCombined = combined.months.first()

        assertEquals(1_080, combined.totals.workedMinutes)
        assertEquals(2, combined.totals.workDays)
        assertEquals(540, combined.totals.averageWorkedMinutes)
        assertEquals(1_080, januaryCombined.workedMinutes)
        assertEquals(60, januaryCombined.overtimeMinutes)
        assertEquals(2, januaryCombined.workDays)
        assertEquals(
            listOf(
                CurrencyEarningsStatistics("EUR", 12_000L),
                CurrencyEarningsStatistics("USD", 30_000L),
            ),
            januaryCombined.earningsByCurrency,
        )
    }

    private fun report(
        id: Long,
        name: String,
        currency: String,
        month: DetailedMonthStatistics,
        yearStatistics: DetailedYearStatistics = DetailedYearStatistics(
            year = year,
            totals = month,
            months = emptyList(),
        ),
    ) = ProfilePeriodStatistics(
        profileId = id,
        profileName = name,
        currencyCode = currency,
        month = month,
        year = yearStatistics,
    )
}
