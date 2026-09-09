package com.arttvad.worktime.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arttvad.worktime.data.preferences.WorkPreferencesRepository
import com.arttvad.worktime.data.repository.WorkProfileRepository
import com.arttvad.worktime.domain.model.WorkProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val profiles: List<WorkProfile> = emptyList(),
    val activeProfileId: Long = 1L,
    val activeShiftRunning: Boolean = false,
) {
    val activeProfile: WorkProfile?
        get() = profiles.firstOrNull { profile -> profile.id == activeProfileId }
            ?: profiles.firstOrNull()
}

class ProfileViewModel(
    private val profileRepository: WorkProfileRepository,
    private val preferencesRepository: WorkPreferencesRepository,
) : ViewModel() {
    val uiState = combine(
        profileRepository.observeProfiles(),
        preferencesRepository.preferences,
    ) { profiles, preferences ->
        ProfileUiState(
            profiles = profiles,
            activeProfileId = preferences.activeProfileId,
            activeShiftRunning = preferences.activeShift != null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = ProfileUiState(),
    )

    suspend fun selectProfile(profileId: Long): Result<Unit> = runCatching {
        require(profileId > 0L) { "Profile id must be positive" }
        val preferences = preferencesRepository.preferences.first()
        if (preferences.activeProfileId == profileId) return@runCatching
        check(preferences.activeShift == null) {
            "Cannot switch work profile while a shift timer is active"
        }
        check(uiState.value.profiles.any { profile -> profile.id == profileId }) {
            "Unknown work profile"
        }
        preferencesRepository.selectProfile(profileId)
    }

    suspend fun createProfile(name: String): Result<Long> = runCatching {
        val preferences = preferencesRepository.preferences.first()
        check(preferences.activeShift == null) {
            "Cannot create and switch work profile while a shift timer is active"
        }
        val profile = profileRepository.create(name)
        preferencesRepository.selectProfile(profile.id)
        profile.id
    }

    companion object {
        fun factory(
            profileRepository: WorkProfileRepository,
            preferencesRepository: WorkPreferencesRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
                return ProfileViewModel(profileRepository, preferencesRepository) as T
            }
        }
    }
}
