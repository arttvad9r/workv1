package com.arttvad.worktime.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arttvad.worktime.data.preferences.WorkPreferences
import com.arttvad.worktime.data.preferences.WorkPreferencesRepository
import com.arttvad.worktime.data.repository.WorkDayRepository
import com.arttvad.worktime.domain.backup.BackupPayment
import com.arttvad.worktime.domain.backup.BackupRestoreCoordinator
import com.arttvad.worktime.domain.calculation.DetailedMonthStatistics
import com.arttvad.worktime.domain.calculation.DetailedMonthStatisticsCalculator
import com.arttvad.worktime.domain.calculation.DetailedYearStatistics
import com.arttvad.worktime.domain.calculation.DetailedYearStatisticsCalculator
import com.arttvad.worktime.domain.calculation.MonthSummaryCalculator
import com.arttvad.worktime.domain.calculation.WorkDayValidator
import com.arttvad.worktime.domain.model.MonthSummary
import com.arttvad.worktime.domain.model.WorkDay
import com.arttvad.worktime.domain.model.WorkDayType
import com.arttvad.worktime.domain.pattern.ShiftPatternDay
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val entries: Map<LocalDate, WorkDay> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val summary: MonthSummary = MonthSummary(),
    val detailedStatistics: DetailedMonthStatistics = DetailedMonthStatistics(),
    val detailedYearStatistics: DetailedYearStatistics = DetailedYearStatistics(Year.now()),
    val preferences: WorkPreferences = WorkPreferences(),
)

sealed interface CalendarEvent {
    data object DataError : CalendarEvent
    data object SaveError : CalendarEvent
    data object DeleteError : CalendarEvent
    data object RestoreError : CalendarEvent
    data object SettingsError : CalendarEvent
    data object PatternApplyError : CalendarEvent
    data object PatternUndoError : CalendarEvent
    data object PatternNoChanges : CalendarEvent
    data class EntryDeleted(val entry: WorkDay) : CalendarEvent
    data class PatternApplied(val insertedDates: List<LocalDate>) : CalendarEvent
}

private data class CalendarPeriodData(
    val month: YearMonth,
    val monthDays: List<WorkDay>,
    val yearDays: List<WorkDay>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val workDayRepository: WorkDayRepository,
    private val preferencesRepository: WorkPreferencesRepository,
) : ViewModel() {
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val mutableEvents = MutableSharedFlow<CalendarEvent>(extraBufferCapacity = 1)

    val events: Flow<CalendarEvent> = mutableEvents

    private val backupRestoreCoordinator = BackupRestoreCoordinator(
        snapshotDays = workDayRepository::snapshotAll,
        replaceDays = workDayRepository::replaceAll,
        readPayment = {
            preferencesRepository.preferences.first().let { preferences ->
                BackupPayment(
                    hourlyRateMinor = preferences.hourlyRateMinor,
                    currencyCode = preferences.currencyCode,
                )
            }
        },
        replacePayment = { payment ->
            preferencesRepository.updatePayment(
                hourlyRateMinor = payment.hourlyRateMinor,
                currencyCode = payment.currencyCode,
            )
        },
    )

    private val monthEntries = visibleMonth.flatMapLatest { month ->
        workDayRepository.observeMonth(month)
            .catch {
                mutableEvents.emit(CalendarEvent.DataError)
                emit(emptyList())
            }
    }

    private val yearEntries = visibleMonth
        .map { month -> Year.of(month.year) }
        .distinctUntilChanged()
        .flatMapLatest { year ->
            workDayRepository.observeYear(year)
                .catch {
                    mutableEvents.emit(CalendarEvent.DataError)
                    emit(emptyList())
                }
        }

    private val periodData = combine(
        visibleMonth,
        monthEntries,
        yearEntries,
    ) { month, monthDays, yearDays ->
        CalendarPeriodData(month, monthDays, yearDays)
    }

    val uiState = combine(
        periodData,
        preferencesRepository.preferences,
        selectedDate,
    ) { data, preferences, selected ->
        CalendarUiState(
            visibleMonth = data.month,
            entries = data.monthDays.associateBy(WorkDay::date),
            selectedDate = selected,
            summary = MonthSummaryCalculator.calculate(data.monthDays, preferences.hourlyRateMinor),
            detailedStatistics = DetailedMonthStatisticsCalculator.calculate(
                data.monthDays,
                preferences.hourlyRateMinor,
            ),
            detailedYearStatistics = DetailedYearStatisticsCalculator.calculate(
                Year.of(data.month.year),
                data.yearDays,
                preferences.hourlyRateMinor,
            ),
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

    fun saveDay(
        date: LocalDate,
        type: WorkDayType,
        workedMinutes: Int,
        overtimeMinutes: Int,
        note: String,
        hourlyRateOverrideMinor: Long?,
    ) {
        if (WorkDayValidator.validate(type, workedMinutes, overtimeMinutes) != null) return
        if (hourlyRateOverrideMinor != null && hourlyRateOverrideMinor < 0) return
        if (type != WorkDayType.WORK && hourlyRateOverrideMinor != null) return

        viewModelScope.launch {
            runCatching {
                workDayRepository.upsert(
                    WorkDay(
                        date = date,
                        workedMinutes = workedMinutes,
                        overtimeMinutes = overtimeMinutes,
                        note = note.trim(),
                        updatedAtEpochMillis = System.currentTimeMillis(),
                        hourlyRateOverrideMinor = hourlyRateOverrideMinor,
                        type = type,
                    ),
                )
            }.onSuccess {
                selectedDate.value = null
            }.onFailure {
                mutableEvents.emit(CalendarEvent.SaveError)
            }
        }
    }

    fun deleteDay(date: LocalDate) {
        val entry = uiState.value.entries[date] ?: return
        viewModelScope.launch {
            runCatching { workDayRepository.delete(date) }
                .onSuccess {
                    selectedDate.value = null
                    mutableEvents.emit(CalendarEvent.EntryDeleted(entry))
                }
                .onFailure { mutableEvents.emit(CalendarEvent.DeleteError) }
        }
    }

    fun restoreDay(entry: WorkDay) {
        viewModelScope.launch {
            runCatching { workDayRepository.upsert(entry) }
                .onFailure { mutableEvents.emit(CalendarEvent.RestoreError) }
        }
    }

    fun applyPattern(days: List<ShiftPatternDay>) {
        if (days.isEmpty()) return
        if (days.any { day -> WorkDayValidator.validate(day.type, day.workedMinutes, 0) != null }) return

        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            runCatching {
                workDayRepository.insertMissing(
                    days.map { day ->
                        WorkDay(
                            date = day.date,
                            workedMinutes = day.workedMinutes,
                            overtimeMinutes = 0,
                            note = "",
                            updatedAtEpochMillis = timestamp,
                            hourlyRateOverrideMinor = null,
                            type = day.type,
                        )
                    },
                )
            }.onSuccess { insertedDates ->
                if (insertedDates.isEmpty()) {
                    mutableEvents.emit(CalendarEvent.PatternNoChanges)
                } else {
                    mutableEvents.emit(CalendarEvent.PatternApplied(insertedDates))
                }
            }.onFailure {
                mutableEvents.emit(CalendarEvent.PatternApplyError)
            }
        }
    }

    fun undoPattern(insertedDates: List<LocalDate>) {
        if (insertedDates.isEmpty()) return
        viewModelScope.launch {
            runCatching { workDayRepository.deleteAll(insertedDates) }
                .onFailure { mutableEvents.emit(CalendarEvent.PatternUndoError) }
        }
    }

    fun updatePayment(hourlyRateMinor: Long?, currencyCode: String) {
        viewModelScope.launch {
            runCatching {
                preferencesRepository.updatePayment(hourlyRateMinor, currencyCode)
            }.onFailure {
                mutableEvents.emit(CalendarEvent.SettingsError)
            }
        }
    }

    suspend fun writeBackup(output: OutputStream): Result<Int> =
        backupRestoreCoordinator.writeBackup(output)

    suspend fun restoreBackup(input: InputStream): Result<Int> =
        backupRestoreCoordinator.restoreBackup(input).onSuccess {
            selectedDate.value = null
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
