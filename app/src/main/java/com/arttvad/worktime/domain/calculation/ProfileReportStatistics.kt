package com.arttvad.worktime.domain.calculation

import java.time.Year
import java.time.YearMonth

data class ProfilePeriodStatistics(
    val profileId: Long,
    val profileName: String,
    val currencyCode: String,
    val month: DetailedMonthStatistics,
    val year: DetailedYearStatistics,
)

data class CurrencyEarningsStatistics(
    val currencyCode: String,
    val earningsMinor: Long?,
)

data class CombinedMonthStatistics(
    val statistics: DetailedMonthStatistics = DetailedMonthStatistics(),
    val earningsByCurrency: List<CurrencyEarningsStatistics> = emptyList(),
)

data class CombinedYearMonthStatistics(
    val month: YearMonth,
    val workedMinutes: Int = 0,
    val overtimeMinutes: Int = 0,
    val workDays: Int = 0,
    val earningsByCurrency: List<CurrencyEarningsStatistics> = emptyList(),
)

data class CombinedYearStatistics(
    val year: Year,
    val totals: DetailedMonthStatistics = DetailedMonthStatistics(),
    val earningsByCurrency: List<CurrencyEarningsStatistics> = emptyList(),
    val months: List<CombinedYearMonthStatistics> = emptyList(),
)

object ProfileReportStatisticsCalculator {
    fun combineMonth(profiles: List<ProfilePeriodStatistics>): CombinedMonthStatistics =
        CombinedMonthStatistics(
            statistics = combineDetailed(profiles.map(ProfilePeriodStatistics::month)),
            earningsByCurrency = combineEarnings(
                profiles.map { profile ->
                    EarningsInput(
                        currencyCode = profile.currencyCode,
                        hasPaidWork = profile.month.workDays > 0,
                        earningsMinor = profile.month.earningsMinor,
                    )
                },
            ),
        )

    fun combineYear(
        year: Year,
        profiles: List<ProfilePeriodStatistics>,
    ): CombinedYearStatistics {
        val yearProfiles = profiles.filter { profile -> profile.year.year == year }
        val totals = combineDetailed(yearProfiles.map { profile -> profile.year.totals })
        val months = (1..12).map { monthValue ->
            val month = YearMonth.of(year.value, monthValue)
            val monthRows = yearProfiles.mapNotNull { profile ->
                profile.year.months.firstOrNull { row -> row.month == month }?.let { row ->
                    profile to row
                }
            }
            CombinedYearMonthStatistics(
                month = month,
                workedMinutes = monthRows.sumOf { (_, row) -> row.workedMinutes },
                overtimeMinutes = monthRows.sumOf { (_, row) -> row.overtimeMinutes },
                workDays = monthRows.sumOf { (_, row) -> row.workDays },
                earningsByCurrency = combineEarnings(
                    monthRows.map { (profile, row) ->
                        EarningsInput(
                            currencyCode = profile.currencyCode,
                            hasPaidWork = row.workDays > 0,
                            earningsMinor = row.earningsMinor,
                        )
                    },
                ),
            )
        }
        return CombinedYearStatistics(
            year = year,
            totals = totals,
            earningsByCurrency = combineEarnings(
                yearProfiles.map { profile ->
                    EarningsInput(
                        currencyCode = profile.currencyCode,
                        hasPaidWork = profile.year.totals.workDays > 0,
                        earningsMinor = profile.year.totals.earningsMinor,
                    )
                },
            ),
            months = months,
        )
    }

    private fun combineDetailed(
        statistics: List<DetailedMonthStatistics>,
    ): DetailedMonthStatistics {
        val workDays = statistics.sumOf(DetailedMonthStatistics::workDays)
        val workedMinutes = statistics.sumOf(DetailedMonthStatistics::workedMinutes)
        return DetailedMonthStatistics(
            workDays = workDays,
            daysOff = statistics.sumOf(DetailedMonthStatistics::daysOff),
            vacationDays = statistics.sumOf(DetailedMonthStatistics::vacationDays),
            sickDays = statistics.sumOf(DetailedMonthStatistics::sickDays),
            workedMinutes = workedMinutes,
            overtimeMinutes = statistics.sumOf(DetailedMonthStatistics::overtimeMinutes),
            averageWorkedMinutes = if (workDays == 0) 0 else (workedMinutes + workDays / 2) / workDays,
            longestWorkedMinutes = statistics.maxOfOrNull(DetailedMonthStatistics::longestWorkedMinutes) ?: 0,
            earningsMinor = null,
        )
    }

    private fun combineEarnings(
        inputs: List<EarningsInput>,
    ): List<CurrencyEarningsStatistics> = inputs
        .filter(EarningsInput::hasPaidWork)
        .groupBy(EarningsInput::currencyCode)
        .toSortedMap()
        .map { (currencyCode, currencyInputs) ->
            CurrencyEarningsStatistics(
                currencyCode = currencyCode,
                earningsMinor = if (currencyInputs.any { input -> input.earningsMinor == null }) {
                    null
                } else {
                    currencyInputs.fold(0L) { total, input ->
                        Math.addExact(total, requireNotNull(input.earningsMinor))
                    }
                },
            )
        }

    private data class EarningsInput(
        val currencyCode: String,
        val hasPaidWork: Boolean,
        val earningsMinor: Long?,
    )
}
