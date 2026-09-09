package com.arttvad.worktime.ui.profile

import androidx.compose.runtime.staticCompositionLocalOf
import com.arttvad.worktime.domain.model.WorkProfile

data class ProfileSwitcherEnvironment(
    val profiles: List<WorkProfile> = emptyList(),
    val activeProfileId: Long = 1L,
    val switchingEnabled: Boolean = true,
    val onSelectProfile: suspend (Long) -> Result<Unit> = { Result.success(Unit) },
    val onCreateProfile: suspend (String) -> Result<Long> = { Result.failure(IllegalStateException()) },
)

val LocalProfileSwitcherEnvironment = staticCompositionLocalOf { ProfileSwitcherEnvironment() }
