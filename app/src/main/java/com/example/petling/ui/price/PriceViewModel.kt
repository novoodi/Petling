package com.example.petling.ui.price

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.Analytics
import com.example.petling.data.market.MarketOverview
import com.example.petling.data.market.MarketRepository
import com.example.petling.data.repository.PriceAnalysis
import com.example.petling.data.repository.PriceRepository
import com.example.petling.data.repository.TrackedProduct
import com.example.petling.domain.receipt.ReceiptDraft
import com.example.petling.domain.receipt.ReceiptItem
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

    /** 영수증 확인 화면: 파싱된 상품을 체크·수정해 한 번에 기록한다. */
    data class ReceiptReady(
        val draft: ReceiptDraft,
        val rows: List<ReceiptRow>,
        val storeName: String,
        val recentStores: List<String> = emptyList(),
        val dateEpochDay: Long?,
        val saving: Boolean = false,
    ) : AnalysisUi {
        val checkedCount: Int get() = rows.count { it.checked && it.priceText.toIntOrNull() != null && it.name.isNotBlank() }
    }
}

/** 영수증 상품 한 줄의 편집 상태. */
data class ReceiptRow(
    val name: String,
    val priceText: String,
    val quantity: Int,
    val checked: Boolean = true,
)

class PriceViewModel(
    private val repository: PriceRepository,
    private val market: MarketRepository,
    private val analytics: Analytics,
) : ViewModel() {

    val tracked: StateFlow<List<TrackedProduct>> =
        repository.observeTracked()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 홈 상단 "이번 주 시장 물가" — 시장 데이터 동기화가 끝나면 다시 계산한다. */
    private val _overview = MutableStateFlow<MarketOverview?>(null)
    val overview: StateFlow<MarketOverview?> = _overview.asStateFlow()

    init {
        viewModelScope.launch {
            market.state.collect { s -> if (s.hasData && !s.syncing) _overview.value = runCatching { market.overview() }.getOrNull() }
        }
    }

    private val _analysis = MutableStateFlow<AnalysisUi?>(null)
    val analysis: StateFlow<AnalysisUi?> = _analysis.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** 저장 연타 방지. */
    private var saving = false

    /** 매장 변경 시 비교 대상 재계산 — 연속 입력이면 이전 계산을 취소해 순서 꼬임을 막는다. */
    private var storeJob: Job? = null

    fun analyze(uri: Uri, source: String = "camera") {
        analytics.analyzeStarted(source)
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

    // ── 영수증

    fun analyzeReceipt(uri: Uri, source: String) {
        analytics.receiptStarted(source)
        _analysis.value = AnalysisUi.Loading
        viewModelScope.launch {
            val draft = repository.analyzeReceipt(uri)
            if (draft.isEmpty) {
                _analysis.value = null
                _message.value = "영수증에서 상품을 찾지 못했어요 — 영수증 전체가 밝고 반듯하게 나오게 다시 찍어주세요"
                return@launch
            }
            val recent = repository.recentStores()
            _analysis.value = AnalysisUi.ReceiptReady(
                draft = draft,
                rows = draft.items.map { ReceiptRow(it.name, it.unitPriceWon.toString(), it.quantity) },
                storeName = draft.storeName ?: repository.defaultStoreForToday().orEmpty(),
                recentStores = recent,
                dateEpochDay = draft.dateEpochDay,
            )
        }
    }

    fun updateReceiptRow(index: Int, name: String? = null, priceText: String? = null, checked: Boolean? = null) {
        val current = _analysis.value as? AnalysisUi.ReceiptReady ?: return
        val row = current.rows.getOrNull(index) ?: return
        val updated = row.copy(
            name = name ?: row.name,
            priceText = priceText?.filter { it.isDigit() }?.take(8) ?: row.priceText,
            checked = checked ?: row.checked,
        )
        _analysis.value = current.copy(rows = current.rows.toMutableList().also { it[index] = updated })
    }

    fun updateReceiptStore(storeName: String) {
        val current = _analysis.value as? AnalysisUi.ReceiptReady ?: return
        _analysis.value = current.copy(storeName = storeName.take(40))
    }

    fun saveReceipt() {
        val current = _analysis.value as? AnalysisUi.ReceiptReady ?: return
        if (current.saving) return
        val items = current.rows.filter { it.checked && it.name.isNotBlank() }.mapNotNull { r ->
            val price = r.priceText.toIntOrNull() ?: return@mapNotNull null
            if (price <= 0) null else ReceiptItem(r.name.trim(), price, r.quantity, price * r.quantity)
        }
        if (items.isEmpty()) {
            _message.value = "기록할 상품을 하나 이상 체크해주세요"
            return
        }
        _analysis.value = current.copy(saving = true)
        viewModelScope.launch {
            val saved = repository.saveReceipt(items, current.storeName.ifBlank { null }, current.dateEpochDay)
            analytics.receiptSaved(saved, current.draft.items.size)
            _analysis.value = null
            _message.value = "영수증에서 ${saved}개 상품을 기록했어요"
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
                analytics.recordSaved(
                    hasMarket = current.analysis.marketGoodId != null,
                    hasStore = current.storeName.isNotBlank(),
                )
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
        // ReceiptReady는 이미지를 저장하지 않으므로 정리할 것이 없다
        _analysis.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }
}
