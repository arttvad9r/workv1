package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.domain.calculation.WorkDayValidator
import com.arttvad.worktime.domain.model.WorkDay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorSheet(
    date: LocalDate,
    entry: WorkDay?,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Int, Int, String) -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },
    ) {
        DayEditorContent(
            date = date,
            entry = entry,
            onDismiss = onDismiss,
            onSave = onSave,
            onDelete = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        )
    }
}

@Composable
fun DayEditorContent(
    date: LocalDate,
    entry: WorkDay?,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Int, Int, String) -> Unit,
    onDelete: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var workedHours by rememberSaveable(date.toString(), entry?.updatedAtEpochMillis) {
        mutableStateOf(entry?.workedMinutes?.div(60)?.toString().orEmpty())
    }
    var workedMinutes by rememberSaveable(date.toString(), entry?.updatedAtEpochMillis) {
        mutableStateOf(entry?.workedMinutes?.rem(60)?.takeIf { it != 0 }?.toString().orEmpty())
    }
    var overtimeHours by rememberSaveable(date.toString(), entry?.updatedAtEpochMillis) {
        mutableStateOf(entry?.overtimeMinutes?.div(60)?.takeIf { it != 0 }?.toString().orEmpty())
    }
    var overtimeMinutes by rememberSaveable(date.toString(), entry?.updatedAtEpochMillis) {
        mutableStateOf(entry?.overtimeMinutes?.rem(60)?.takeIf { it != 0 }?.toString().orEmpty())
    }
    var note by rememberSaveable(date.toString(), entry?.updatedAtEpochMillis) {
        mutableStateOf(entry?.note.orEmpty())
    }
    var noteExpanded by rememberSaveable(date.toString()) {
        mutableStateOf(entry?.note?.isNotBlank() == true)
    }

    val workedTotal = parseDuration(workedHours, workedMinutes)
    val overtimeTotal = parseDuration(overtimeHours, overtimeMinutes, allowZero = true)
    val workedTouched = workedHours.isNotBlank() || workedMinutes.isNotBlank()
    val overtimeTouched = overtimeHours.isNotBlank() || overtimeMinutes.isNotBlank()

    val validationMessage = when {
        workedTouched && workedTotal == null -> stringResource(R.string.invalid_duration)
        workedTouched && workedTotal == 0 -> stringResource(R.string.empty_day_error)
        overtimeTouched && overtimeTotal == null -> stringResource(R.string.invalid_duration)
        workedTotal != null && overtimeTotal != null && overtimeTotal > workedTotal ->
            stringResource(R.string.invalid_overtime)
        else -> null
    }

    val canSave = workedTotal != null &&
        workedTotal > 0 &&
        overtimeTotal != null &&
        WorkDayValidator.validate(workedTotal, overtimeTotal) == null

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDayTitle(date),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.day_editor_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }

        DurationFields(
            title = stringResource(R.string.worked),
            tagPrefix = "worked",
            hours = workedHours,
            minutes = workedMinutes,
            onHoursChanged = { workedHours = it.onlyDigits(maxLength = 2) },
            onMinutesChanged = { workedMinutes = it.onlyDigits(maxLength = 2) },
        )

        DurationFields(
            title = stringResource(R.string.overtime),
            tagPrefix = "overtime",
            hours = overtimeHours,
            minutes = overtimeMinutes,
            onHoursChanged = { overtimeHours = it.onlyDigits(maxLength = 2) },
            onMinutesChanged = { overtimeMinutes = it.onlyDigits(maxLength = 2) },
        )

        if (validationMessage != null) {
            Text(
                text = validationMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (noteExpanded) {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(500) },
                label = { Text(stringResource(R.string.note)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("day-editor-note"),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        } else {
            TextButton(
                onClick = { noteExpanded = true },
                modifier = Modifier.testTag("day-editor-add-note"),
            ) {
                Text(stringResource(R.string.add_note))
            }
        }

        Button(
            onClick = {
                val work = workedTotal ?: return@Button
                val overtime = overtimeTotal ?: return@Button
                focusManager.clearFocus()
                onSave(date, work, overtime, note)
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("day-editor-save"),
        ) {
            Text(stringResource(R.string.save))
        }

        if (entry != null) {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onDelete(date)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("day-editor-delete"),
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun DurationFields(
    title: String,
    tagPrefix: String,
    hours: String,
    minutes: String,
    onHoursChanged: (String) -> Unit,
    onMinutesChanged: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = hours,
                onValueChange = onHoursChanged,
                label = { Text(stringResource(R.string.hours)) },
                suffix = { Text(stringResource(R.string.hours_short)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("day-editor-$tagPrefix-hours"),
            )
            OutlinedTextField(
                value = minutes,
                onValueChange = onMinutesChanged,
                label = { Text(stringResource(R.string.minutes)) },
                suffix = { Text(stringResource(R.string.minutes_short)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("day-editor-$tagPrefix-minutes"),
            )
        }
    }
}

private fun parseDuration(
    hours: String,
    minutes: String,
    allowZero: Boolean = false,
): Int? {
    val parsedHours = if (hours.isBlank()) 0 else hours.toIntOrNull() ?: return null
    val parsedMinutes = if (minutes.isBlank()) 0 else minutes.toIntOrNull() ?: return null
    if (parsedHours !in 0..24) return null
    if (parsedMinutes !in 0..59) return null
    if (parsedHours == 24 && parsedMinutes != 0) return null
    val total = parsedHours * 60 + parsedMinutes
    if (!allowZero && total == 0) return 0
    return total
}

private fun String.onlyDigits(maxLength: Int): String = filter(Char::isDigit).take(maxLength)
