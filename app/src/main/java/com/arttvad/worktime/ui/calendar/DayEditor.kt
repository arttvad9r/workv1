package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.domain.calculation.MoneyFormatter
import com.arttvad.worktime.domain.calculation.ShiftDurationCalculator
import com.arttvad.worktime.domain.calculation.WorkDayValidator
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorSheet(
    date: LocalDate,
    entry: WorkDay?,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (LocalDate, WorkDayType, Int, Int, String, Long?) -> Unit,
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
            currencyCode = currencyCode,
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
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (LocalDate, WorkDayType, Int, Int, String, Long?) -> Unit,
    onDelete: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTypeName by rememberSaveable(date.toString(), entry?.updatedAtEpochMillis) {
        mutableStateOf(entry?.type?.name ?: WorkDayType.WORK.name)
    }
    val selectedType = WorkDayType.valueOf(selectedTypeName)

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
    var rateOverrideExpanded by rememberSaveable(date.toString(), entry?.updatedAtEpochMillis) {
        mutableStateOf(entry?.hourlyRateOverrideMinor != null)
    }
    var rateOverrideInput by rememberSaveable(date.toString(), entry?.updatedAtEpochMillis) {
        mutableStateOf(
            runCatching {
                MoneyFormatter.formatRateInput(entry?.hourlyRateOverrideMinor, currencyCode)
            }.getOrDefault(""),
        )
    }
    var shiftCalculatorExpanded by rememberSaveable(date.toString()) { mutableStateOf(false) }
    var shiftStart by rememberSaveable(date.toString()) { mutableStateOf("") }
    var shiftEnd by rememberSaveable(date.toString()) { mutableStateOf("") }
    var shiftBreak by rememberSaveable(date.toString()) { mutableStateOf("") }

    val workedTotal = parseDuration(workedHours, workedMinutes)
    val overtimeTotal = parseDuration(overtimeHours, overtimeMinutes, allowZero = true)
    val workedTouched = workedHours.isNotBlank() || workedMinutes.isNotBlank()
    val overtimeTouched = overtimeHours.isNotBlank() || overtimeMinutes.isNotBlank()
    val rateOverrideMinor = if (!rateOverrideExpanded || rateOverrideInput.isBlank()) {
        null
    } else {
        runCatching {
            MoneyFormatter.parseMajorToMinor(rateOverrideInput, currencyCode)
        }.getOrNull()
    }
    val rateOverrideValid = !rateOverrideExpanded ||
        rateOverrideInput.isBlank() ||
        rateOverrideMinor != null

    val validationMessage = if (selectedType == WorkDayType.WORK) {
        when {
            workedTouched && workedTotal == null -> stringResource(R.string.invalid_duration)
            workedTouched && workedTotal == 0 -> stringResource(R.string.empty_day_error)
            overtimeTouched && overtimeTotal == null -> stringResource(R.string.invalid_duration)
            workedTotal != null && overtimeTotal != null && overtimeTotal > workedTotal ->
                stringResource(R.string.invalid_overtime)
            else -> null
        }
    } else {
        null
    }

    val canSave = if (selectedType == WorkDayType.WORK) {
        workedTotal != null &&
            workedTotal > 0 &&
            overtimeTotal != null &&
            WorkDayValidator.validate(selectedType, workedTotal, overtimeTotal) == null &&
            rateOverrideValid
    } else {
        WorkDayValidator.validate(selectedType, 0, 0) == null
    }

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

        DayTypeSelector(
            selectedType = selectedType,
            onTypeSelected = { type ->
                selectedTypeName = type.name
                focusManager.clearFocus()
            },
        )

        if (selectedType == WorkDayType.WORK) {
            DurationFields(
                title = stringResource(R.string.worked),
                tagPrefix = "worked",
                hours = workedHours,
                minutes = workedMinutes,
                onHoursChanged = { workedHours = it.onlyDigits(maxLength = 2) },
                onMinutesChanged = { workedMinutes = it.onlyDigits(maxLength = 2) },
            )

            WorkDurationPresets(
                workedHours = workedHours,
                workedMinutes = workedMinutes,
                shiftCalculatorExpanded = shiftCalculatorExpanded,
                onPresetSelected = { hours ->
                    workedHours = hours.toString()
                    workedMinutes = ""
                    shiftCalculatorExpanded = false
                    focusManager.clearFocus()
                },
                onShiftCalculatorToggle = {
                    shiftCalculatorExpanded = !shiftCalculatorExpanded
                    focusManager.clearFocus()
                },
            )

            if (shiftCalculatorExpanded) {
                ShiftTimeCalculator(
                    start = shiftStart,
                    end = shiftEnd,
                    breakInput = shiftBreak,
                    onStartChanged = { shiftStart = it.clockInput() },
                    onEndChanged = { shiftEnd = it.clockInput() },
                    onBreakChanged = { shiftBreak = it.onlyDigits(maxLength = 4) },
                    onApply = { calculatedMinutes ->
                        workedHours = (calculatedMinutes / 60).toString()
                        workedMinutes = (calculatedMinutes % 60)
                            .takeIf { it != 0 }
                            ?.toString()
                            .orEmpty()
                        focusManager.clearFocus()
                    },
                )
            }

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

        if (selectedType == WorkDayType.WORK) {
            if (rateOverrideExpanded) {
                OutlinedTextField(
                    value = rateOverrideInput,
                    onValueChange = { value ->
                        rateOverrideInput = value
                            .filter { it.isDigit() || it == ',' || it == '.' }
                            .take(16)
                    },
                    label = { Text(stringResource(R.string.day_rate_override)) },
                    suffix = { Text(currencyCode) },
                    singleLine = true,
                    isError = !rateOverrideValid,
                    supportingText = if (!rateOverrideValid) {
                        { Text(stringResource(R.string.invalid_day_rate)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("day-editor-rate-override"),
                )
                TextButton(
                    onClick = {
                        rateOverrideInput = ""
                        rateOverrideExpanded = false
                    },
                    modifier = Modifier.testTag("day-editor-use-base-rate"),
                ) {
                    Text(stringResource(R.string.use_base_rate))
                }
            } else {
                TextButton(
                    onClick = { rateOverrideExpanded = true },
                    modifier = Modifier.testTag("day-editor-add-rate-override"),
                ) {
                    Text(stringResource(R.string.add_day_rate_override))
                }
            }
        }

        Button(
            onClick = {
                val work = if (selectedType == WorkDayType.WORK) workedTotal ?: return@Button else 0
                val overtime = if (selectedType == WorkDayType.WORK) overtimeTotal ?: return@Button else 0
                val override = if (selectedType == WorkDayType.WORK) rateOverrideMinor else null
                focusManager.clearFocus()
                onSave(date, selectedType, work, overtime, note, override)
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
private fun DayTypeSelector(
    selectedType: WorkDayType,
    onTypeSelected: (WorkDayType) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WorkDayType.entries.forEach { type ->
            val fullLabel = dayTypeLabel(type)
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = {
                    Text(
                        text = dayTypeSelectorLabel(type),
                        maxLines = 1,
                    )
                },
                modifier = Modifier
                    .testTag("day-editor-type-${type.name.lowercase()}")
                    .semantics { contentDescription = fullLabel },
            )
        }
    }
}

@Composable
private fun dayTypeLabel(type: WorkDayType): String = stringResource(
    when (type) {
        WorkDayType.WORK -> R.string.day_type_work
        WorkDayType.DAY_OFF -> R.string.day_type_day_off
        WorkDayType.VACATION -> R.string.day_type_vacation
        WorkDayType.SICK -> R.string.day_type_sick
    },
)

@Composable
private fun dayTypeSelectorLabel(type: WorkDayType): String = stringResource(
    when (type) {
        WorkDayType.WORK -> R.string.day_type_work
        WorkDayType.DAY_OFF -> R.string.day_type_day_off_short
        WorkDayType.VACATION -> R.string.day_type_vacation_short
        WorkDayType.SICK -> R.string.day_type_sick_short
    },
)

@Composable
private fun WorkDurationPresets(
    workedHours: String,
    workedMinutes: String,
    shiftCalculatorExpanded: Boolean,
    onPresetSelected: (Int) -> Unit,
    onShiftCalculatorToggle: () -> Unit,
) {
    val hoursSuffix = stringResource(R.string.hours_short)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(8, 10, 12).forEach { hours ->
            FilterChip(
                selected = workedHours == hours.toString() &&
                    (workedMinutes.isBlank() || workedMinutes == "0") &&
                    !shiftCalculatorExpanded,
                onClick = { onPresetSelected(hours) },
                label = { Text("$hours $hoursSuffix") },
                modifier = Modifier.testTag("day-editor-preset-$hours"),
            )
        }
        FilterChip(
            selected = shiftCalculatorExpanded,
            onClick = onShiftCalculatorToggle,
            label = { Text(stringResource(R.string.calculate_by_time)) },
            modifier = Modifier.testTag("day-editor-time-calculator"),
        )
    }
}

@Composable
private fun ShiftTimeCalculator(
    start: String,
    end: String,
    breakInput: String,
    onStartChanged: (String) -> Unit,
    onEndChanged: (String) -> Unit,
    onBreakChanged: (String) -> Unit,
    onApply: (Int) -> Unit,
) {
    val breakMinutes = if (breakInput.isBlank()) 0 else breakInput.toIntOrNull()
    val calculatedMinutes = breakMinutes?.let {
        ShiftDurationCalculator.calculate(start = start, end = end, breakMinutes = it)
    }
    val hasEnoughInput = start.isNotBlank() && end.isNotBlank()
    val invalid = hasEnoughInput && calculatedMinutes == null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = start,
            onValueChange = onStartChanged,
            label = { Text(stringResource(R.string.shift_start)) },
            placeholder = { Text(stringResource(R.string.shift_time_hint)) },
            singleLine = true,
            isError = invalid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("day-editor-shift-start"),
        )
        OutlinedTextField(
            value = end,
            onValueChange = onEndChanged,
            label = { Text(stringResource(R.string.shift_end)) },
            placeholder = { Text(stringResource(R.string.shift_time_hint)) },
            singleLine = true,
            isError = invalid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("day-editor-shift-end"),
        )
        OutlinedTextField(
            value = breakInput,
            onValueChange = onBreakChanged,
            label = { Text(stringResource(R.string.shift_break)) },
            placeholder = { Text(stringResource(R.string.shift_break_hint)) },
            suffix = { Text(stringResource(R.string.minutes_short)) },
            singleLine = true,
            isError = invalid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("day-editor-shift-break"),
        )

        if (invalid) {
            Text(
                text = stringResource(R.string.invalid_shift_time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (calculatedMinutes != null) {
            Text(
                text = stringResource(
                    R.string.shift_total,
                    formatDurationShort(calculatedMinutes),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedButton(
            onClick = { calculatedMinutes?.let(onApply) },
            enabled = calculatedMinutes != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("day-editor-shift-apply"),
        ) {
            Text(stringResource(R.string.apply_shift_duration))
        }
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

private fun String.clockInput(): String {
    val filtered = filter { it.isDigit() || it == ':' }
    if (':' in filtered) return filtered.take(5)
    val digits = filtered.filter(Char::isDigit).take(4)
    return if (digits.length <= 2) digits else "${digits.take(2)}:${digits.drop(2)}"
}
