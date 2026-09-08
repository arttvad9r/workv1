package com.arttvad.worktime.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.domain.calculation.MoneyFormatter
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSettingsSheet(
    preferences: WorkPreferences,
    onDismiss: () -> Unit,
    onSave: (Long?, String) -> Unit,
) {
    var currencyCode by rememberSaveable(preferences.currencyCode) {
        mutableStateOf(preferences.currencyCode)
    }
    var rateInput by rememberSaveable(preferences.hourlyRateMinor, preferences.currencyCode) {
        mutableStateOf(
            runCatching {
                MoneyFormatter.formatRateInput(preferences.hourlyRateMinor, preferences.currencyCode)
            }.getOrDefault(""),
        )
    }

    val normalizedCurrency = currencyCode.trim().uppercase(Locale.ROOT)
    val currencyValid = normalizedCurrency.length == 3 && runCatching {
        Currency.getInstance(normalizedCurrency)
    }.isSuccess
    val parsedRate = if (rateInput.isBlank()) {
        null
    } else if (currencyValid) {
        runCatching { MoneyFormatter.parseMajorToMinor(rateInput, normalizedCurrency) }.getOrNull()
    } else {
        null
    }
    val rateValid = rateInput.isBlank() || parsedRate != null
    val canSave = currencyValid && rateValid
    val focusManager = LocalFocusManager.current

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_rate_title),
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = rateInput,
                onValueChange = { value ->
                    rateInput = value.filter { it.isDigit() || it == ',' || it == '.' }.take(16)
                },
                label = { Text(stringResource(R.string.hourly_rate)) },
                placeholder = { Text(stringResource(R.string.rate_hint)) },
                singleLine = true,
                isError = !rateValid,
                supportingText = if (!rateValid) {
                    { Text(stringResource(R.string.invalid_rate)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-hourly-rate"),
            )

            OutlinedTextField(
                value = currencyCode,
                onValueChange = { value ->
                    currencyCode = value.filter(Char::isLetter).uppercase(Locale.ROOT).take(3)
                },
                label = { Text(stringResource(R.string.currency_code)) },
                placeholder = { Text(stringResource(R.string.currency_hint)) },
                singleLine = true,
                isError = !currencyValid,
                supportingText = if (!currencyValid) {
                    { Text(stringResource(R.string.invalid_currency)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings-currency"),
            )

            if (rateInput.isNotBlank()) {
                TextButton(onClick = { rateInput = "" }) {
                    Text(stringResource(R.string.clear_rate))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSave(parsedRate, normalizedCurrency)
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings-save"),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
