package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.data.preferences.ActiveShiftSession
import com.arttvad.worktime.domain.calculation.ShiftTimerCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val ShiftTimerClockFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun ShiftTimerControls(
    date: LocalDate,
    activeShift: ActiveShiftSession?,
    onStartShift: suspend (LocalDate) -> Result<Unit>,
    onStopShift: suspend (LocalDate) -> Result<Int>,
    onResetShift: suspend () -> Result<Unit>,
    onDurationReady: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var errorVisible by rememberSaveable(date.toString()) { mutableStateOf(false) }
    var nowMillis by rememberSaveable(activeShift?.startedAtEpochMillis) {
        mutableLongStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(activeShift?.startedAtEpochMillis) {
        if (activeShift == null) return@LaunchedEffect
        while (isActive) {
            nowMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            activeShift == null && date == LocalDate.now() -> {
                Text(
                    text = stringResource(R.string.shift_timer_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        errorVisible = false
                        scope.launch {
                            onStartShift(date).onFailure { errorVisible = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day-editor-timer-start"),
                ) {
                    Text(stringResource(R.string.shift_timer_start))
                }
            }

            activeShift?.date == date -> {
                val elapsedMinutes = ShiftTimerCalculator.elapsedMinutes(
                    startedAtEpochMillis = activeShift.startedAtEpochMillis,
                    endedAtEpochMillis = nowMillis,
                )
                val startedAt = Instant.ofEpochMilli(activeShift.startedAtEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(ShiftTimerClockFormatter)

                Text(
                    text = stringResource(R.string.shift_timer_started, startedAt),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(
                        R.string.shift_timer_elapsed,
                        formatDurationShort(elapsedMinutes ?: 0),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        errorVisible = false
                        scope.launch {
                            onStopShift(date).fold(
                                onSuccess = onDurationReady,
                                onFailure = { errorVisible = true },
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day-editor-timer-stop"),
                ) {
                    Text(stringResource(R.string.shift_timer_stop))
                }
                TextButton(
                    onClick = {
                        errorVisible = false
                        scope.launch {
                            onResetShift().onFailure { errorVisible = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day-editor-timer-reset"),
                ) {
                    Text(stringResource(R.string.shift_timer_reset))
                }
            }

            activeShift != null -> {
                Text(
                    text = stringResource(
                        R.string.shift_timer_other_date,
                        activeShift.date.toString(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("day-editor-timer-other-date"),
                )
                TextButton(
                    onClick = {
                        errorVisible = false
                        scope.launch {
                            onResetShift().onFailure { errorVisible = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day-editor-timer-reset"),
                ) {
                    Text(stringResource(R.string.shift_timer_reset))
                }
            }
        }

        if (errorVisible) {
            Text(
                text = stringResource(R.string.shift_timer_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("day-editor-timer-error"),
            )
        }
    }
}
