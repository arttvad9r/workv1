package com.arttvad.worktime.ui.profile

import androidx.compose.runtime.staticCompositionLocalOf
import com.arttvad.worktime.domain.calculation.CombinedMonthStatistics
import com.arttvad.worktime.domain.calculation.CombinedYearStatistics
import com.arttvad.worktime.domain.calculation.ProfilePeriodStatistics
import java.time.Year

data class ProfileReportEnvironment(
    val activeProfileId: Long = 1L,
    val profiles: List<ProfilePeriodStatistics> = emptyList(),
    val combinedMonthStatistics: CombinedMonthStatistics = CombinedMonthStatistics(),
    val combinedYearStatistics: CombinedYearStatistics = CombinedYearStatistics(Year.now()),
)

val LocalProfileReportEnvironment = staticCompositionLocalOf { ProfileReportEnvironment() }
