package com.example.petling.ui.paste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.repository.ScheduleRepository
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.model.Schedule
import com.example.petling.domain.parsing.ScheduleParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PasteStage { INPUT, PARSED }

data class PasteUiState(
    val stage: PasteStage = PasteStage.INPUT,
    val text: String = "",
    val drafts: List<ParsedDraftSeed> = emptyList(),
    val savedCount: Int = 0,
)

/**
 * 텍스트 붙여넣기 → 파싱 → 다중 draft 확인.
 * 음성(VoiceViewModel) 플로우와 동일하되 텍스트를 붙여넣기/공유로 얻는다.
 */
class PasteViewModel(
    private val parser: ScheduleParser,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PasteUiState())
    val state: StateFlow<PasteUiState> = _state.asStateFlow()

    /** 초기 텍스트(공유/클립보드) 프리필. 이미 입력된 게 있으면 덮어쓰지 않는다. */
    fun prefill(text: String) {
        if (_state.value.text.isBlank() && text.isNotBlank()) {
            _state.value = _state.value.copy(text = text)
        }
    }

    fun updateText(v: String) {
        _state.value = _state.value.copy(text = v)
    }

    fun parse() {
        val text = _state.value.text
        if (text.isBlank()) return
        viewModelScope.launch {
            val drafts = parser.parse(text)
            _state.value = _state.value.copy(stage = PasteStage.PARSED, drafts = drafts, savedCount = 0)
        }
    }

    fun backToInput() {
        _state.value = _state.value.copy(stage = PasteStage.INPUT)
    }

    fun quickSave(seed: ParsedDraftSeed) {
        val date = seed.date ?: return
        viewModelScope.launch {
            scheduleRepository.create(
                Schedule(
                    title = seed.title?.ifBlank { "일정" } ?: "일정",
                    date = date,
                    startMinuteOfDay = seed.startMinuteOfDay,
                    location = seed.location,
                    category = seed.category,
                    isImportant = seed.isImportant,
                    source = seed.source,
                    reminderOffsetMin = 10,
                ),
            )
            _state.value = _state.value.copy(
                drafts = _state.value.drafts - seed,
                savedCount = _state.value.savedCount + 1,
            )
        }
    }
}
