package com.example.petling.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.repository.CaptureRepository
import com.example.petling.data.repository.CategoryRepository
import com.example.petling.domain.model.CaptureItem
import com.example.petling.domain.model.Category
import com.example.petling.domain.parsing.ScheduleParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CaptureDetailViewModel(
    private val captureId: Long,
    private val repository: CaptureRepository,
    categoryRepository: CategoryRepository,
    private val parser: ScheduleParser,
) : ViewModel() {

    val item: StateFlow<CaptureItem?> = repository.observeById(captureId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categories: StateFlow<List<Category>> = categoryRepository.enabledCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _summarizing = MutableStateFlow(false)
    val summarizing: StateFlow<Boolean> = _summarizing.asStateFlow()
    private var summaryTriggered = false

    init {
        // 상세를 열면 요약이 없을 때 온디바이스 AI로 한 번 생성(캐시됨 → Flow가 갱신 반영)
        viewModelScope.launch {
            item.collect { current ->
                if (current != null && current.summary.isNullOrBlank() && !summaryTriggered) {
                    summaryTriggered = true
                    _summarizing.value = true
                    repository.ensureSummary(current)
                    _summarizing.value = false
                }
            }
        }
    }

    /** OCR 텍스트에서 날짜가 파싱되면 종류와 무관하게 일정 등록을 제안한다. */
    val canRegisterSchedule: StateFlow<Boolean> = item
        .map { it != null && parser.parse(it.ocrText).any { seed -> seed.date != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _scheduleRegistered = MutableStateFlow(false)
    val scheduleRegistered: StateFlow<Boolean> = _scheduleRegistered.asStateFlow()

    fun changeCategory(key: String) {
        val current = item.value ?: return
        viewModelScope.launch { repository.updateCategory(current, key) }
    }

    fun updateNote(note: String) {
        val current = item.value ?: return
        viewModelScope.launch { repository.updateNote(current, note.ifBlank { null }) }
    }

    fun registerAsSchedule() {
        val current = item.value ?: return
        viewModelScope.launch {
            val seed = parser.parse(current.ocrText).firstOrNull { it.date != null } ?: return@launch
            repository.registerAsSchedule(current, seed)
            _scheduleRegistered.value = true
        }
    }

    fun delete(onDone: () -> Unit) {
        val current = item.value ?: return
        viewModelScope.launch {
            repository.delete(current)
            onDone()
        }
    }
}
