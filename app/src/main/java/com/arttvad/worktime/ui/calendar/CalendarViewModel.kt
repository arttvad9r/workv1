package com.arttvad.worktime.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.data.preferences.WorkPreferencesRepository
import com.arttvad.worktime.data.repository.WorkDayRepository
import com.arttvad.worktime.domain.calculation.MonthSummaryCalculator
import com.arttvad.worktime.domain.calculation.WorkDayValidator
import com.arttvad.worktime.domain.model.MonthSummary
import com.arttvad.worktime.domain.model.WorkDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val entries: Map<LocalDate, WorkDay> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val summary: MonthSummary = MonthSummary(),
    val preferences: WorkPreferences = WorkPreferences(),
)

enum class CalendarMessage {
    DATA_ERROR,
    SAVE_ERROR,
    DELETE_ERROR,
    SETTINGS_ERROR,
}

class CalendarViewModel(
    private val workDayRepository: WorkDayRepository,
    private val preferencesRepository: WorkPreferencesRepository,
) : ViewModel() {
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val mutableMessages = MutableSharedFlow<CalendarMessage>(extraBufferCapacity = 1)

    val messages: Flow<CalendarMessage> = mutableMessages

    private val monthEntries = visibleMonth.flatMapLatest { month ->
        workDayRepository.observeMonth(month)
            .catch {
                mutableMessages.emit(CalendarMessage.DATA_ERROR)
                emit(emptyList())
            }
    }

    val uiState = combine(
        visibleMonth,
        monthEntries,
        preferencesRepository.preferences,
        selectedDate,
    ) { month, days, preferences, selected ->
        CalendarUiState(
            visibleMonth = month,
            entries = days.associateBy(WorkDay::date),
            selectedDate = selected,
            summary = MonthSummaryCalculator.calculate(days, preferences.hourlyRateMinor),
            preferences = preferences,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CalendarUiState(),
    )

    fun setVisibleMonth(month: YearMonth) {
        if (visibleMonth.value != month) visibleMonth.value = month
    }

    fun selectDay(date: LocalDate) {
        selectedDate.value = date
    }

    fun dismissDayEditor() {
        selectedDate.value = null
    }

    fun saveDay(date: LocalDate, workedMinutes: Int, overtimeMinutes: Int, note: String) {
        if (WorkDayValidator.validate(workedMinutes, overtimeMinutes) != null) return

        viewModelScope.launch {
            runCatching {
                workDayRepository.upsert(
                    WorkDay(
                        date = date,
                        workedMinutes = workedMinutes,
                        overtimeMinutes = overtimeMinutes,
                        note = note.trim(),
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }.onSuccess {
                selectedDate.value = null
            }.onFailure {
                mutableMessages.emit(CalendarMessage.SAVE_ERROR)
            }
        }
    }

    fun deleteDay(date: LocalDate) {
        viewModelScope.launch {
            runCatching { workDayRepository.delete(date) }
                .onSuccess { selectedDate.value = null }
                .onFailure { mutableMessages.emit(CalendarMessage.DELETE_ERROR) }
        }
    }

    fun updatePayment(hourlyRateMinor: Long?, currencyCode: String) {
        viewModelScope.launch {
            runCatching {
                preferencesRepository.updatePayment(hourlyRateMinor, currencyCode)
            }.onFailure {
                mutableMessages.emit(CalendarMessage.SETTINGS_ERROR)
            }
        }
    }

    companion object {
        fun factory(
            workDayRepository: WorkDayRepository,
            preferencesRepository: WorkPreferencesRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(CalendarViewModel::class.java))
                return CalendarViewModel(workDayRepository, preferencesRepository) as T
            }
        }
    }
}
