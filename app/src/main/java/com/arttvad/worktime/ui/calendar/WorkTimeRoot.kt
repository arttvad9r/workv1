package com.arttvad.worktime.ui.calendar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arttvad.worktime.R

@Composable
fun WorkTimeRoot(viewModel: CalendarViewModel) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    val dataError = stringResource(R.string.data_error)
    val saveError = stringResource(R.string.save_error)
    val deleteError = stringResource(R.string.delete_error)
    val restoreError = stringResource(R.string.restore_error)
    val settingsError = stringResource(R.string.settings_error)
    val entryDeleted = stringResource(R.string.entry_deleted)
    val undo = stringResource(R.string.undo)

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                CalendarEvent.DataError -> snackbarHostState.showSnackbar(dataError)
                CalendarEvent.SaveError -> snackbarHostState.showSnackbar(saveError)
                CalendarEvent.DeleteError -> snackbarHostState.showSnackbar(deleteError)
                CalendarEvent.RestoreError -> snackbarHostState.showSnackbar(restoreError)
                CalendarEvent.SettingsError -> snackbarHostState.showSnackbar(settingsError)
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
            }
        }
    }

    WorkTimeScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onVisibleMonthChanged = viewModel::setVisibleMonth,
        onDaySelected = viewModel::selectDay,
        onDismissDayEditor = viewModel::dismissDayEditor,
        onSaveDay = viewModel::saveDay,
        onDeleteDay = viewModel::deleteDay,
        onUpdatePayment = viewModel::updatePayment,
    )
}
