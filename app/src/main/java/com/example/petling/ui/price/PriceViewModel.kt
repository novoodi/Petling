package com.example.petling.ui.price

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.repository.PriceAnalysis
import com.example.petling.data.repository.PriceRepository
import com.example.petling.data.repository.TrackedProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 가격표 분석 오버레이 상태. null이면 목록 모드. */
sealed interface AnalysisUi {
    object Loading : AnalysisUi
    data class Ready(
        val analysis: PriceAnalysis,
        val name: String,
        val priceText: String,
        /** 촬영 매장(선택). 빈 문자열이면 매장 없이 기록. */
        val storeName: String = "",
        /** 최근 사용 매장 칩 후보. */
        val recentStores: List<String> = emptyList(),
        /** 매장 직접 입력 중 참가격 판매점 자동완성. */
        val storeSuggestions: List<String> = emptyList(),
        val requerying: Boolean = false,
    ) : AnalysisUi
}

class PriceViewModel(private val repository: PriceRepository) : ViewModel() {

    val tracked: StateFlow<List<TrackedProduct>> =
        repository.observeTracked()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _analysis = MutableStateFlow<AnalysisUi?>(null)
    val analysis: StateFlow<AnalysisUi?> = _analysis.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** 저장 연타 방지. */
    private var saving = false

    /** 매장 변경 시 비교 대상 재계산 — 연속 입력이면 이전 계산을 취소해 순서 꼬임을 막는다. */
    private var storeJob: Job? = null

    fun analyze(uri: Uri) {
        _analysis.value = AnalysisUi.Loading
        viewModelScope.launch {
            val recentStores = repository.recentStores()
            val store = repository.defaultStoreForToday().orEmpty()
            val result = repository.analyze(uri, store.ifBlank { null })
            _analysis.value = AnalysisUi.Ready(
                analysis = result,
                name = result.tag.name,
                priceText = result.tag.priceWon?.toString().orEmpty(),
                storeName = store,
                recentStores = recentStores,
            )
        }
    }

    fun updateStore(storeName: String) {
        val current = _analysis.value as? AnalysisUi.Ready ?: return
        val trimmed = storeName.take(40)
        _analysis.value = current.copy(storeName = trimmed)
        storeJob?.cancel()
        storeJob = viewModelScope.launch {
            val suggestions = if (trimmed.length >= 2 && trimmed !in current.recentStores) {
                repository.storeSuggestions(trimmed).filter { it != trimmed }
            } else emptyList()
            val updated = repository.withStore(current.analysis, trimmed.ifBlank { null })
            val latest = _analysis.value as? AnalysisUi.Ready ?: return@launch
            _analysis.value = latest.copy(analysis = updated, storeSuggestions = suggestions)
        }
    }

    /** 시장 카드: 후보 선택(goodId) 또는 "해당 없음"(null). */
    fun pickMarketProduct(goodId: Long?) {
        val current = _analysis.value as? AnalysisUi.Ready ?: return
        viewModelScope.launch {
            val updated = repository.withMarketPick(current.analysis, goodId, current.storeName.ifBlank { null })
            val latest = _analysis.value as? AnalysisUi.Ready ?: return@launch
            _analysis.value = latest.copy(analysis = updated)
        }
    }

    fun updateName(name: String) {
        val current = _analysis.value as? AnalysisUi.Ready ?: return
        _analysis.value = current.copy(name = name)
    }

    fun updatePriceText(text: String) {
        val current = _analysis.value as? AnalysisUi.Ready ?: return
        _analysis.value = current.copy(priceText = text.filter { it.isDigit() }.take(8))
    }

    fun pickCandidate(priceWon: Int) = updatePriceText(priceWon.toString())

    /** 이름을 고친 뒤 네이버 재검색 + 재방문 재매칭. */
    fun requery() {
        val current = _analysis.value as? AnalysisUi.Ready ?: return
        if (current.requerying || current.name.isBlank()) return
        _analysis.value = current.copy(requerying = true)
        viewModelScope.launch {
            val updated = repository.requery(
                current.analysis,
                current.name.trim(),
                current.storeName.ifBlank { null },
            )
            val latest = _analysis.value as? AnalysisUi.Ready ?: return@launch
            _analysis.value = latest.copy(analysis = updated, requerying = false)
        }
    }

    fun save() {
        val current = _analysis.value as? AnalysisUi.Ready ?: return
        val price = current.priceText.toIntOrNull()
        if (current.name.isBlank() || price == null || price <= 0) {
            _message.value = "상품명과 가격을 확인해주세요"
            return
        }
        if (saving) return
        saving = true
        viewModelScope.launch {
            val id = repository.save(
                current.analysis,
                current.name.trim(),
                price,
                current.storeName.ifBlank { null },
            )
            saving = false
            if (id != null) {
                _analysis.value = null
                _message.value = "가격을 기록했어요"
            } else {
                _message.value = "저장에 실패했어요 — 다시 시도해주세요"
            }
        }
    }

    fun dismissAnalysis() {
        val current = _analysis.value
        if (current is AnalysisUi.Ready) {
            viewModelScope.launch { repository.discard(current.analysis) }
        }
        _analysis.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }
}
