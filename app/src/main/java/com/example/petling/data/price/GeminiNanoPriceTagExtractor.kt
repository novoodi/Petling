package com.example.petling.data.price

import android.graphics.BitmapFactory
import com.example.petling.data.capture.NanoLog
import com.example.petling.data.capture.generateOrNull
import com.example.petling.domain.price.PriceTagExtractor
import com.example.petling.domain.price.PriceTagInfo
import com.example.petling.domain.price.RuleBasedPriceTagExtractor
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 온디바이스 AI(Gemini Nano) 준비 상태. 설정 화면·첫 실행 안내가 그대로 표시한다.
 * 모델 파일은 AICore(시스템 서비스)가 관리하며 앱은 다운로드를 "요청"만 할 수 있다.
 */
sealed interface NanoState {
    /** AICore에 아직 물어보는 중(앱 시작 직후). */
    data object Checking : NanoState
    /** 이 기기는 미지원 — 규칙 인식만으로 동작. */
    data object Unavailable : NanoState
    /** 지원 기기이나 모델을 아직 받지 않음. */
    data object Downloadable : NanoState
    /** 받는 중. [totalBytes]는 AICore가 알려주기 전엔 null. */
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long?) : NanoState
    /** 다운로드 실패 — 사용자가 다시 시도할 수 있다. */
    data class Failed(val reason: String?) : NanoState
    /** 모델 준비 완료. */
    data object Available : NanoState

    val isReady: Boolean get() = this is Available
    val isSupported: Boolean get() = this !is Unavailable && this !is Checking
}

/**
 * 규칙 추출 + Nano 멀티모달 보정.
 *
 * 숫자는 규칙(OCR 텍스트)이 진실원이다 — Nano 산술·숫자 생성은 신뢰하지 않는다(릴리스 QA 교훈).
 * Nano의 역할은 두 가지 "선택" 뿐:
 *  1) 사진에 이웃 가격표가 섞였을 때 중앙 가격표의 상품명 고르기
 *  2) 규칙이 최종가를 확정 못 했을 때(후보 여러 개) 후보 중에서 고르기 — 후보 밖 답은 버린다
 * Nano 미지원/실패 시 규칙 결과 그대로 반환하므로 전 기기에서 동작한다.
 */
class GeminiNanoPriceTagExtractor(
    private val rules: RuleBasedPriceTagExtractor = RuleBasedPriceTagExtractor(),
) : PriceTagExtractor {

    private val model: GenerativeModel by lazy { Generation.getClient() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<NanoState>(NanoState.Checking)
    /** 설정·첫 실행 안내용 상태. 다운로드 진행률까지 흘러온다. */
    val state: StateFlow<NanoState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    /** AICore에 현재 상태를 다시 묻는다. 다운로드 중이면 진행률 스트림을 덮어쓰지 않는다. */
    suspend fun refresh(): NanoState {
        val status = runCatching { model.checkStatus() }.getOrNull()
        val current = _state.value
        val next = when (status) {
            FeatureStatus.AVAILABLE -> NanoState.Available
            FeatureStatus.DOWNLOADABLE -> NanoState.Downloadable
            FeatureStatus.DOWNLOADING ->
                if (current is NanoState.Downloading) current else NanoState.Downloading(0L, null)
            else -> NanoState.Unavailable
        }
        // 활성 다운로드 잡이 진행률을 쓰고 있으면 폴링 결과로 덮어쓰지 않는다
        if (downloadJob?.isActive == true && next !is NanoState.Available) return current
        _state.value = next
        return next
    }

    /**
     * 모델 다운로드를 요청한다(지원 기기에서만 의미 있음). AICore가 Wi-Fi·저장공간 조건을 판단하므로
     * 요청 즉시 받지 않을 수도 있다 — 그 경우 상태는 Downloading(0)에 머문다.
     * @return 요청을 걸었으면 true, 이미 진행 중이거나 지원 상태가 아니면 false.
     */
    fun download(): Boolean {
        if (downloadJob?.isActive == true) return false
        val current = _state.value
        if (current !is NanoState.Downloadable && current !is NanoState.Downloading && current !is NanoState.Failed) return false
        _state.value = NanoState.Downloading(0L, null)
        downloadJob = scope.launch {
            var total: Long? = null
            runCatching {
                model.download().collect { status ->
                    when (status) {
                        is DownloadStatus.DownloadStarted -> {
                            total = status.bytesToDownload.takeIf { it > 0 }
                            _state.value = NanoState.Downloading(0L, total)
                        }
                        is DownloadStatus.DownloadProgress ->
                            _state.value = NanoState.Downloading(status.totalBytesDownloaded, total)
                        is DownloadStatus.DownloadCompleted -> _state.value = NanoState.Available
                        is DownloadStatus.DownloadFailed -> {
                            NanoLog.d("price", "dl_fail", status.e.message ?: "")
                            _state.value = NanoState.Failed(status.e.message)
                        }
                    }
                }
            }.onFailure {
                NanoLog.d("price", "dl_fail", it.message ?: "")
                _state.value = NanoState.Failed(it.message)
            }.onSuccess {
                NanoLog.d("price", "dl_ok")
                // 완료 이벤트 없이 스트림이 끝난 경우를 대비해 실제 상태를 한 번 더 확인
                if (_state.value !is NanoState.Available && _state.value !is NanoState.Failed) refresh()
            }
        }
        return true
    }

    /** 앱 시작 시: 상태를 확인하고 받을 수 있으면 미리 받아 첫 가격표부터 Nano 보정이 되게 한다. */
    suspend fun prewarm() {
        when (refresh()) {
            NanoState.Downloadable, is NanoState.Downloading -> download()
            else -> Unit
        }
    }

    override suspend fun extract(ocrText: String, imagePath: String?): PriceTagInfo {
        val ruleInfo = rules.extract(ocrText, imagePath)
        if (imagePath == null || ocrText.isBlank()) return ruleInfo

        val status = refresh()
        if (status !is NanoState.Available) {
            NanoLog.d("price", "status_skip", "status=$status")
            // 앱 시작 시 prewarm이 놓친 경우(AICore 초기화 지연 등) 여기서 다시 다운로드를 건다.
            if (status is NanoState.Downloadable) download()
            return ruleInfo
        }

        val bitmap = loadDownscaled(imagePath) ?: return ruleInfo
        val request = GenerateContentRequest.Builder(
            ImagePart(bitmap),
            TextPart(buildPrompt(ocrText, ruleInfo)),
        ).build()
        val answer = model.generateOrNull(request)
            ?.candidates?.firstOrNull()?.text
        if (answer.isNullOrBlank()) {
            NanoLog.d("price", "gen_fail")
            return ruleInfo
        }

        val nanoName = parseField(answer, "상품명")
        val nanoPrice = parseField(answer, "가격")
            ?.filter { it.isDigit() }
            ?.toIntOrNull()

        // 상품명: OCR 텍스트와 한 토큰이라도 겹칠 때만 채택(환각 차단).
        // 규칙이 이름을 못 뽑았을 땐 Nano 답을 그대로 쓴다(이미지에서 읽었을 수 있음).
        val name = when {
            nanoName.isNullOrBlank() -> ruleInfo.name
            ruleInfo.name.isBlank() -> nanoName
            overlapsOcr(nanoName, ocrText) -> nanoName
            else -> ruleInfo.name
        }

        // 가격: 규칙이 확정했다면 그대로. 미확정일 때만 Nano가 고른 값이
        // 규칙의 후보 목록에 있을 경우에 한해 채택한다.
        val price = when {
            ruleInfo.priceWon != null -> ruleInfo.priceWon
            nanoPrice != null && nanoPrice in ruleInfo.priceCandidatesWon -> {
                NanoLog.d("price", "pick_ok")
                nanoPrice
            }
            else -> null
        }

        if (name != ruleInfo.name || price != ruleInfo.priceWon) {
            NanoLog.d("price", "refined")
        }
        return ruleInfo.copy(name = name.trim(), priceWon = price)
    }

    private fun buildPrompt(ocrText: String, ruleInfo: PriceTagInfo): String {
        val candidates = ruleInfo.priceCandidatesWon.joinToString(", ")
        val candidateLine = if (ruleInfo.priceWon == null && candidates.isNotEmpty()) {
            "가격은 반드시 다음 후보 중 하나의 숫자여야 해: $candidates"
        } else ""
        return """
            이 사진은 마트 매대의 가격표야. 사진 중앙의 가장 큰 가격표 한 장에 대해서만, 아래 두 줄 형식으로 답해.
            상품명: (가격표에 적힌 상품 이름 그대로, 용량 표기는 빼고)
            가격: (할인이 반영된 최종 판매 가격 숫자만)
            $candidateLine
            참고 — 화면에서 인식된 텍스트: ```${ocrText.take(2000)}```
        """.trimIndent()
    }

    private fun parseField(answer: String, label: String): String? =
        answer.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("$label:") || it.startsWith("$label :") }
            ?.substringAfter(":")
            ?.trim(' ', '"', '\'')
            ?.takeIf { it.isNotBlank() }

    private fun overlapsOcr(name: String, ocrText: String): Boolean =
        name.split(' ').any { it.length >= 2 && ocrText.contains(it) }

    private fun loadDownscaled(path: String): android.graphics.Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIM_PX) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    private companion object {
        const val MAX_DIM_PX = 1024
    }
}
