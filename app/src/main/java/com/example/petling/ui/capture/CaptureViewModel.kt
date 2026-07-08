package com.example.petling.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.repository.CaptureRepository
import com.example.petling.data.repository.CaptureResult
import com.example.petling.data.repository.CategoryRepository
import com.example.petling.domain.model.Category
import com.example.petling.domain.model.CharacterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class IngestStage { PROCESSING, DONE, FAILED }

data class CaptureUiState(
    val stage: IngestStage = IngestStage.PROCESSING,
    val result: CaptureResult? = null,
    val currentKey: String? = null,
    val scheduleRegistered: Boolean = false,
    val scheduleUndone: Boolean = false,
)

/**
 * 이미지 → 자동 정리(OCR·분류·보관·캐릭터 성장) 결과 표시.
 */
class CaptureViewModel(
    private val captureRepository: CaptureRepository,
    categoryRepository: CategoryRepository,
    characterFlow: kotlinx.coroutines.flow.Flow<CharacterState?>,
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    /** 분류 변경 칩용 활성 카테고리. */
    val categories: StateFlow<List<Category>> =
        categoryRepository.enabledCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoriesByKey: StateFlow<Map<String, Category>> =
        categoryRepository.allCategories
            .map { list -> list.associateBy { it.key } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val character: StateFlow<CharacterState?> =
        characterFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun processImage(uri: Uri) {
        _state.value = CaptureUiState(stage = IngestStage.PROCESSING)
        viewModelScope.launch {
            val result = captureRepository.ingest(uri)
            _state.value = if (result == null) {
                CaptureUiState(stage = IngestStage.FAILED)
            } else {
                CaptureUiState(
                    stage = IngestStage.DONE,
                    result = result,
                    currentKey = result.item.categoryKey,
                )
            }
        }
    }

    /** 자동 분류가 틀렸을 때 사용자가 카테고리 변경. */
    fun changeCategory(key: String) {
        val item = _state.value.result?.item ?: return
        viewModelScope.launch {
            captureRepository.updateCategory(item, key)
            _state.value = _state.value.copy(currentKey = key)
        }
    }

    /** 자동 등록된 일정을 취소(되돌리기). */
    fun undoSchedule() {
        val item = _state.value.result?.item ?: return
        viewModelScope.launch {
            captureRepository.undoAutoSchedule(item)
            _state.value = _state.value.copy(scheduleUndone = true)
        }
    }

    /** SCHEDULE 캡처를 실제 일정으로도 등록. */
    fun registerAsSchedule() {
        val result = _state.value.result ?: return
        val seed = result.scheduleSeed ?: return
        viewModelScope.launch {
            captureRepository.registerAsSchedule(result.item, seed)
            _state.value = _state.value.copy(scheduleRegistered = true)
        }
    }
}
