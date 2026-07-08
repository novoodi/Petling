package com.example.petling.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.repository.ScheduleRepository
import com.example.petling.domain.AppClock
import com.example.petling.domain.model.Schedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repository: ScheduleRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(clock.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.from(clock.today()))
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    /** 보이는 달의 모든 일정 (dot 표시용). */
    val monthSchedules: StateFlow<List<Schedule>> = _visibleMonth
        .flatMapLatest { month ->
            repository.observeBetween(month.atDay(1), month.atEndOfMonth())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 선택한 날의 일정 목록. */
    val daySchedules: StateFlow<List<Schedule>> = _selectedDate
        .flatMapLatest { date -> repository.observeByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _visibleMonth.value = YearMonth.from(date)
    }

    fun changeMonth(month: YearMonth) {
        _visibleMonth.value = month
    }

    fun goToToday() {
        selectDate(clock.today())
    }
}
