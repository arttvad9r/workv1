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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.domain.model.WorkDayType
import com.arttvad.worktime.domain.pattern.ShiftPatternDay
import com.arttvad.worktime.domain.pattern.ShiftPatternGenerator
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val PatternDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)

private enum class PatternPreset(
    val workDays: Int?,
    val offDays: Int?,
) {
    TWO_TWO(2, 2),
    THREE_THREE(3, 3),
    FIVE_TWO(5, 2),
    CUSTOM(null, null),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternGeneratorSheet(
    visibleMonth: YearMonth,
    onDismiss: () -> Unit,
    onApply: (List<ShiftPatternDay>) -> Unit,
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
        PatternGeneratorContent(
            visibleMonth = visibleMonth,
            onDismiss = onDismiss,
            onApply = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        )
    }
}

@Composable
fun PatternGeneratorContent(
    visibleMonth: YearMonth,
    onDismiss: () -> Unit,
    onApply: (List<ShiftPatternDay>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var startInput by rememberSaveable(visibleMonth.toString()) {
        mutableStateOf(visibleMonth.atDay(1).format(PatternDateFormatter))
    }
    var endInput by rememberSaveable(visibleMonth.toString()) {
        mutableStateOf(visibleMonth.atEndOfMonth().format(PatternDateFormatter))
    }
    var presetName by rememberSaveable { mutableStateOf(PatternPreset.TWO_TWO.name) }
    var customWorkDays by rememberSaveable { mutableStateOf("2") }
    var customOffDays by rememberSaveable { mutableStateOf("2") }
    var shiftHours by rememberSaveable { mutableStateOf(8) }

    val preset = PatternPreset.valueOf(presetName)
    val workDays = preset.workDays ?: customWorkDays.toIntOrNull()
    val offDays = preset.offDays ?: customOffDays.toIntOrNull()
    val startDate = parsePatternDate(startInput)
    val endDate = parsePatternDate(endInput)
    val preview = if (startDate != null && endDate != null && workDays != null && offDays != null) {
        ShiftPatternGenerator.generate(
            startDate = startDate,
            endDate = endDate,
            workDays = workDays,
            offDays = offDays,
            workedMinutes = shiftHours * 60,
        )
    } else {
        null
    }
    val workCount = preview?.count { it.type == WorkDayType.WORK } ?: 0
    val offCount = preview?.count { it.type == WorkDayType.DAY_OFF } ?: 0
    val hasDateInput = startInput.isNotBlank() && endInput.isNotBlank()
    val invalid = hasDateInput && preview == null

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pattern_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.pattern_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = startInput,
                onValueChange = { startInput = it.take(10) },
                label = { Text(stringResource(R.string.pattern_start_date)) },
                placeholder = { Text(stringResource(R.string.pattern_date_hint)) },
                isError = invalid && startDate == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pattern-start-date"),
            )
            OutlinedTextField(
                value = endInput,
                onValueChange = { endInput = it.take(10) },
                label = { Text(stringResource(R.string.pattern_end_date)) },
                placeholder = { Text(stringResource(R.string.pattern_date_hint)) },
                isError = invalid && endDate == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pattern-end-date"),
            )
        }

        Text(
            text = stringResource(R.string.pattern_cycle),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PatternPreset.entries.forEach { option ->
                FilterChip(
                    selected = preset == option,
                    onClick = { presetName = option.name },
                    label = {
                        Text(
                            when (option) {
                                PatternPreset.TWO_TWO -> "2/2"
                                PatternPreset.THREE_THREE -> "3/3"
                                PatternPreset.FIVE_TWO -> "5/2"
                                PatternPreset.CUSTOM -> stringResource(R.string.pattern_custom)
                            },
                        )
                    },
                    modifier = Modifier.testTag("pattern-${option.name.lowercase()}"),
                )
            }
        }

        if (preset == PatternPreset.CUSTOM) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = customWorkDays,
                    onValueChange = { customWorkDays = it.filter(Char::isDigit).take(2) },
                    label = { Text(stringResource(R.string.pattern_work_days)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pattern-custom-work"),
                )
                OutlinedTextField(
                    value = customOffDays,
                    onValueChange = { customOffDays = it.filter(Char::isDigit).take(2) },
                    label = { Text(stringResource(R.string.pattern_off_days)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pattern-custom-off"),
                )
            }
        }

        Text(
            text = stringResource(R.string.pattern_shift_duration),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(8, 10, 12).forEach { hours ->
                FilterChip(
                    selected = shiftHours == hours,
                    onClick = { shiftHours = hours },
                    label = { Text("$hours ${stringResource(R.string.hours_short)}") },
                    modifier = Modifier.testTag("pattern-shift-$hours"),
                )
            }
        }

        if (invalid) {
            Text(
                text = stringResource(R.string.pattern_invalid),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        preview?.let { days ->
            PatternPreview(
                days = days,
                workCount = workCount,
                offCount = offCount,
            )
        }

        Text(
            text = stringResource(R.string.pattern_existing_preserved),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                val days = preview ?: return@Button
                onApply(days)
                onDismiss()
            },
            enabled = preview?.isNotEmpty() == true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pattern-apply"),
        ) {
            Text(stringResource(R.string.pattern_apply))
        }

        Spacer(modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun PatternPreview(
    days: List<ShiftPatternDay>,
    workCount: Int,
    offCount: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.pattern_preview),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.pattern_preview_summary, workCount, offCount, days.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        days.take(14).forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = day.date.format(PatternDateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (day.type == WorkDayType.WORK) {
                        stringResource(
                            R.string.pattern_preview_work,
                            formatDurationShort(day.workedMinutes),
                        )
                    } else {
                        stringResource(R.string.day_type_day_off)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (days.size > 14) {
            Text(
                text = stringResource(R.string.pattern_preview_more, days.size - 14),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun parsePatternDate(value: String): LocalDate? = runCatching {
    LocalDate.parse(value.trim(), PatternDateFormatter)
}.getOrNull()
