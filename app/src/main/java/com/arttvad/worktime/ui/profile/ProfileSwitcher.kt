package com.arttvad.worktime.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.arttvad.worktime.R
import com.arttvad.worktime.domain.model.WorkProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherSheet(
    profiles: List<WorkProfile>,
    activeProfileId: Long,
    switchingEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelectProfile: suspend (Long) -> Result<Unit>,
    onCreateProfile: suspend (String) -> Result<Long>,
) {
    val scope = rememberCoroutineScope()
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }
    var errorVisible by rememberSaveable { mutableStateOf(false) }
    val normalizedName = name.trim()
    val canCreate = switchingEnabled && !busy && normalizedName.isNotBlank() && normalizedName.length <= 40

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { BottomSheetDefaults.DragHandle() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag("profile-switcher"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.profile_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }

            Text(
                text = stringResource(R.string.profile_sheet_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!switchingEnabled) {
                Text(
                    text = stringResource(R.string.profile_timer_block),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("profile-switch-blocked"),
                )
            }

            profiles.forEach { profile ->
                val selected = profile.id == activeProfileId
                val enabled = selected || (switchingEnabled && !busy)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(
                            enabled = enabled,
                            role = Role.RadioButton,
                        ) {
                            if (selected) {
                                onDismiss()
                            } else {
                                errorVisible = false
                                busy = true
                                scope.launch {
                                    onSelectProfile(profile.id)
                                        .onSuccess { onDismiss() }
                                        .onFailure { errorVisible = true }
                                    busy = false
                                }
                            }
                        }
                        .padding(horizontal = 4.dp)
                        .testTag("profile-row-${profile.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        enabled = enabled,
                    )
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (creating) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { value -> name = value.take(40) },
                    label = { Text(stringResource(R.string.profile_name)) },
                    singleLine = true,
                    enabled = switchingEnabled && !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile-name"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = {
                            name = ""
                            creating = false
                            errorVisible = false
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    OutlinedButton(
                        onClick = {
                            errorVisible = false
                            busy = true
                            scope.launch {
                                onCreateProfile(normalizedName)
                                    .onSuccess { onDismiss() }
                                    .onFailure { errorVisible = true }
                                busy = false
                            }
                        },
                        enabled = canCreate,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile-create"),
                    ) {
                        Text(stringResource(R.string.profile_create))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        creating = true
                        errorVisible = false
                    },
                    enabled = switchingEnabled && !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile-add"),
                ) {
                    Text(stringResource(R.string.profile_add))
                }
            }

            if (errorVisible) {
                Text(
                    text = stringResource(R.string.profile_action_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("profile-error"),
                )
            }
        }
    }
}
