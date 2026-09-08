package com.arttvad.worktime.ui.calendar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arttvad.worktime.R
import com.arttvad.worktime.reminder.WorkReminderScheduler
import com.arttvad.worktime.widget.WorkTimeWidget
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun WorkTimeRoot(
    viewModel: CalendarViewModel,
    openTodayRequestToken: Long = 0L,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val dataError = stringResource(R.string.data_error)
    val saveError = stringResource(R.string.save_error)
    val deleteError = stringResource(R.string.delete_error)
    val restoreError = stringResource(R.string.restore_error)
    val settingsError = stringResource(R.string.settings_error)
    val entryDeleted = stringResource(R.string.entry_deleted)
    val patternApplied = stringResource(R.string.pattern_applied)
    val patternNoChanges = stringResource(R.string.pattern_no_changes)
    val patternApplyError = stringResource(R.string.pattern_apply_error)
    val patternUndoError = stringResource(R.string.pattern_undo_error)
    val undo = stringResource(R.string.undo)

    LaunchedEffect(openTodayRequestToken) {
        if (openTodayRequestToken > 0L) {
            val today = LocalDate.now()
            viewModel.setVisibleMonth(YearMonth.from(today))
            viewModel.selectDay(today)
        }
    }

    LaunchedEffect(uiState.detailedYearStatistics, uiState.preferences) {
        runCatching { WorkTimeWidget().updateAll(context) }
    }

    LaunchedEffect(
        uiState.preferences.reminderEnabled,
        uiState.preferences.reminderHour,
        uiState.preferences.reminderMinute,
    ) {
        runCatching {
            WorkReminderScheduler.sync(
                context = context,
                enabled = uiState.preferences.reminderEnabled,
                hour = uiState.preferences.reminderHour,
                minute = uiState.preferences.reminderMinute,
            )
        }
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                CalendarEvent.DataError -> snackbarHostState.showSnackbar(dataError)
                CalendarEvent.SaveError -> snackbarHostState.showSnackbar(saveError)
                CalendarEvent.DeleteError -> snackbarHostState.showSnackbar(deleteError)
                CalendarEvent.RestoreError -> snackbarHostState.showSnackbar(restoreError)
                CalendarEvent.SettingsError -> snackbarHostState.showSnackbar(settingsError)
                CalendarEvent.PatternApplyError -> snackbarHostState.showSnackbar(patternApplyError)
                CalendarEvent.PatternUndoError -> snackbarHostState.showSnackbar(patternUndoError)
                CalendarEvent.PatternNoChanges -> snackbarHostState.showSnackbar(patternNoChanges)
                is CalendarEvent.EntryDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = entryDeleted,
                        actionLabel = undo,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreDay(event.entry)
                    }
                }
                is CalendarEvent.PatternApplied -> {
                    val result = snackbarHostState.showSnackbar(
                        message = patternApplied,
                        actionLabel = undo,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoPattern(event.insertedDates)
                    }
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalShiftTimerEnvironment provides ShiftTimerEnvironment(
            activeShift = uiState.preferences.activeShift,
            onStartShift = viewModel::startShift,
            onStopShift = viewModel::stopShift,
            onResetShift = viewModel::resetShift,
        ),
    ) {
        WorkTimeScreen(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onVisibleMonthChanged = viewModel::setVisibleMonth,
            onDaySelected = viewModel::selectDay,
            onDismissDayEditor = viewModel::dismissDayEditor,
            onSaveDay = viewModel::saveDay,
            onDeleteDay = viewModel::deleteDay,
            onUpdatePayment = viewModel::updatePayment,
            onUpdateReminder = viewModel::updateReminder,
            onApplyPattern = viewModel::applyPattern,
            onWriteBackup = viewModel::writeBackup,
            onRestoreBackup = viewModel::restoreBackup,
        )
    }
}
