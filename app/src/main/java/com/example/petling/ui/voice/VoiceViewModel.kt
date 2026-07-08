package com.example.petling.ui.voice

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

enum class VoiceStage { LISTENING, PROCESSING, REVIEW, EMPTY, ERROR }

data class VoiceUiState(
    val stage: VoiceStage = VoiceStage.LISTENING,
    val recognizedText: String = "",
    val drafts: List<ParsedDraftSeed> = emptyList(),
    val errorMessage: String? = null,
)

class VoiceViewModel(
    private val parser: ScheduleParser,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    fun onListening() {
        _state.value = VoiceUiState(stage = VoiceStage.LISTENING)
    }

    fun onRecognized(text: String) {
        if (text.isBlank()) {
            _state.value = VoiceUiState(stage = VoiceStage.EMPTY)
            return
        }
        _state.value = VoiceUiState(stage = VoiceStage.PROCESSING, recognizedText = text)
        viewModelScope.launch {
            val drafts = parser.parse(text)
            _state.value = VoiceUiState(
                stage = if (drafts.isEmpty()) VoiceStage.EMPTY else VoiceStage.REVIEW,
                recognizedText = text,
                drafts = drafts,
            )
        }
    }

    fun onError(message: String) {
        _state.value = VoiceUiState(stage = VoiceStage.ERROR, errorMessage = message)
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
            _state.value = _state.value.copy(drafts = _state.value.drafts - seed)
        }
    }
}
