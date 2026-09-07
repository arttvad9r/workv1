package com.arttvad.worktime.ui.calendar

import androidx.compose.material3.SnackbarHostState
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
    val settingsError = stringResource(R.string.settings_error)

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                CalendarMessage.DATA_ERROR -> dataError
                CalendarMessage.SAVE_ERROR -> saveError
                CalendarMessage.DELETE_ERROR -> deleteError
                CalendarMessage.SETTINGS_ERROR -> settingsError
            }
            snackbarHostState.showSnackbar(text)
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
