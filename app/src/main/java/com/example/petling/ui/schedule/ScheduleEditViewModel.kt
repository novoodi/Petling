package com.example.petling.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.datastore.SettingsDataStore
import com.example.petling.data.repository.ScheduleRepository
import com.example.petling.domain.AppClock
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.model.Schedule
import com.example.petling.domain.model.ScheduleCategory
import com.example.petling.domain.model.ScheduleSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ScheduleEditUiState(
    val id: Long? = null,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val startMinuteOfDay: Int? = null,
    val isAllDay: Boolean = true,
    val location: String = "",
    val memo: String = "",
    val category: ScheduleCategory? = null,
    val isImportant: Boolean = false,
    val reminderOffsetMin: Int? = 10,
    val source: ScheduleSource = ScheduleSource.MANUAL,
    /** 파싱 draft로 진입 시, 확신도가 낮아 검토가 필요함을 알리는 배너 표시. */
    val needsReview: Boolean = false,
    val loaded: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank()
}

class ScheduleEditViewModel(
    private val repository: ScheduleRepository,
    private val settings: SettingsDataStore,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleEditUiState())
    val state: StateFlow<ScheduleEditUiState> = _state.asStateFlow()

    /** 편집 모드 로드 또는 신규(선택적으로 날짜/파싱 draft 프리필). */
    fun load(scheduleId: Long?, presetEpochDay: Long?, seed: ParsedDraftSeed? = null) {
        if (_state.value.loaded) return
        viewModelScope.launch {
            when {
                scheduleId != null -> {
                    val s = repository.getById(scheduleId) ?: return@launch
                    _state.value = ScheduleEditUiState(
                        id = s.id,
                        title = s.title,
                        date = s.date,
                        startMinuteOfDay = s.startMinuteOfDay,
                        isAllDay = s.isAllDay,
                        location = s.location.orEmpty(),
                        memo = s.memo.orEmpty(),
                        category = s.category,
                        isImportant = s.isImportant,
                        reminderOffsetMin = s.reminderOffsetMin,
                        loaded = true,
                    )
                }
                seed != null -> {
                    val defaultOffset = settings.settings.first().defaultReminderOffsetMin
                    _state.value = ScheduleEditUiState(
                        title = seed.title.orEmpty(),
                        date = seed.date ?: presetEpochDay?.let { LocalDate.ofEpochDay(it) } ?: clock.today(),
                        startMinuteOfDay = seed.startMinuteOfDay,
                        isAllDay = seed.startMinuteOfDay == null,
                        location = seed.location.orEmpty(),
                        category = seed.category,
                        isImportant = seed.isImportant,
                        source = seed.source,
                        needsReview = seed.confidence < 0.6f,
                        reminderOffsetMin = defaultOffset,
                        loaded = true,
                    )
                }
                else -> {
                    val defaultOffset = settings.settings.first().defaultReminderOffsetMin
                    _state.value = ScheduleEditUiState(
                        date = presetEpochDay?.let { LocalDate.ofEpochDay(it) } ?: clock.today(),
                        reminderOffsetMin = defaultOffset,
                        loaded = true,
                    )
                }
            }
        }
    }

    fun updateTitle(v: String) { _state.value = _state.value.copy(title = v) }
    fun updateDate(v: LocalDate) { _state.value = _state.value.copy(date = v) }
    fun updateLocation(v: String) { _state.value = _state.value.copy(location = v) }
    fun updateMemo(v: String) { _state.value = _state.value.copy(memo = v) }
    fun updateCategory(v: ScheduleCategory?) { _state.value = _state.value.copy(category = v) }
    fun updateImportant(v: Boolean) { _state.value = _state.value.copy(isImportant = v) }
    fun updateReminderOffset(v: Int?) { _state.value = _state.value.copy(reminderOffsetMin = v) }

    fun updateAllDay(allDay: Boolean) {
        _state.value = if (allDay) {
            _state.value.copy(isAllDay = true, startMinuteOfDay = null)
        } else {
            _state.value.copy(isAllDay = false, startMinuteOfDay = _state.value.startMinuteOfDay ?: 9 * 60)
        }
    }

    fun updateTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(isAllDay = false, startMinuteOfDay = hour * 60 + minute)
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        val schedule = Schedule(
            id = s.id ?: 0L,
            title = s.title.trim(),
            date = s.date,
            startMinuteOfDay = if (s.isAllDay) null else s.startMinuteOfDay,
            location = s.location.trim().ifBlank { null },
            memo = s.memo.trim().ifBlank { null },
            category = s.category,
            isImportant = s.isImportant,
            source = s.source,
            reminderOffsetMin = s.reminderOffsetMin,
        )
        viewModelScope.launch {
            if (s.id == null) repository.create(schedule) else repository.update(schedule)
            onDone()
        }
    }
}
