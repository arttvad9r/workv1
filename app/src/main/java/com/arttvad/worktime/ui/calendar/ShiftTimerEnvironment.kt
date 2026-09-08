package com.arttvad.worktime.ui.calendar

import androidx.compose.runtime.staticCompositionLocalOf
import com.arttvad.worktime.data.preferences.ActiveShiftSession
import java.time.LocalDate

data class ShiftTimerEnvironment(
    val activeShift: ActiveShiftSession?,
    val onStartShift: suspend (LocalDate) -> Result<Unit>,
    val onStopShift: suspend (LocalDate) -> Result<Int>,
    val onResetShift: suspend () -> Result<Unit>,
)

val LocalShiftTimerEnvironment = staticCompositionLocalOf<ShiftTimerEnvironment?> { null }
