package com.example.petling.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.repository.CharacterRepository
import com.example.petling.data.repository.ScheduleRepository
import com.example.petling.domain.model.Schedule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 일정 상세 화면 상태: 로딩 → 로드됨 / 없음(삭제되었거나 잘못된 링크). */
sealed interface ScheduleDetailUiState {
    data object Loading : ScheduleDetailUiState
    data object NotFound : ScheduleDetailUiState
    data class Loaded(val schedule: Schedule) : ScheduleDetailUiState
}

class ScheduleDetailViewModel(
    scheduleId: Long,
    private val scheduleRepository: ScheduleRepository,
    private val characterRepository: CharacterRepository,
) : ViewModel() {

    val uiState: StateFlow<ScheduleDetailUiState> = scheduleRepository.observeById(scheduleId)
        .map { if (it == null) ScheduleDetailUiState.NotFound else ScheduleDetailUiState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleDetailUiState.Loading)

    private fun current(): Schedule? = (uiState.value as? ScheduleDetailUiState.Loaded)?.schedule

    fun complete() {
        viewModelScope.launch {
            current()?.let { characterRepository.completeSchedule(it) }
        }
    }

    fun revert() {
        viewModelScope.launch {
            current()?.let { characterRepository.revertCompletion(it) }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            current()?.let { scheduleRepository.delete(it) }
            onDone()
        }
    }
}
