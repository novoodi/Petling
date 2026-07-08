package com.example.petling.data.capture

import com.example.petling.domain.capture.CaptureClassifier
import com.example.petling.domain.capture.CaptureSummarizer
import com.example.petling.domain.capture.CaptureTitle
import com.example.petling.domain.capture.Classification
import com.example.petling.domain.model.BuiltInCatalog
import com.example.petling.domain.model.Category
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Gemini Nano(ML Kit GenAI Prompt API)로 캡처 내용을 이해해 분류한다.
 * 프롬프트를 사용자의 활성 카테고리로 동적 생성해, 그 집합 안에서만 답하게 한다.
 * 지원 기기(AICore+Nano)에서만 동작하며, 실패·미지원·다운로드 필요 시 예외를 던져
 * CompositeCaptureClassifier가 규칙 분류기로 폴백한다. 앱이 절대 죽지 않도록 전부 감싼다.
 */
class GeminiNanoCaptureClassifier : CaptureClassifier, CaptureSummarizer {

    private val model: GenerativeModel by lazy { Generation.getClient() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var downloadStarted = false

    /** 설정 화면 등에서 지원 상태를 확인. FeatureStatus 상수(0~3), 실패 시 UNAVAILABLE. */
    suspend fun availability(): Int =
        runCatching { model.checkStatus() }.getOrDefault(FeatureStatus.UNAVAILABLE)

    override suspend fun classify(
        ocrText: String,
        parsedHasSchedule: Boolean,
        scheduleTitle: String?,
        categories: List<Category>,
    ): Classification {
        if (ocrText.isBlank()) throw IllegalStateException("empty text")
        if (categories.isEmpty()) throw IllegalStateException("no categories")

        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE -> {
                ensureDownload()
                throw IllegalStateException("nano downloadable — fall back this time")
            }
            else -> throw IllegalStateException("nano unavailable")
        }

        val response = model.generateContent(buildPrompt(ocrText, categories))
        val answer = response.candidates.firstOrNull()?.text.orEmpty()
        val category = matchCategory(answer, categories)
            ?: throw IllegalStateException("unparsed nano answer: $answer")

        val title = if (category.key == BuiltInCatalog.SCHEDULE && !scheduleTitle.isNullOrBlank()) {
            scheduleTitle
        } else {
            CaptureTitle.autoTitle(ocrText)
        }
        return Classification(category.key, title, 0.9f)
    }

    /** 캡처 내용을 한 문장으로 요약(온디바이스). 지원·성공 시에만 값, 그 외 null. */
    override suspend fun summarize(text: String): String? {
        if (text.isBlank()) return null
        if (runCatching { model.checkStatus() }.getOrNull() != FeatureStatus.AVAILABLE) return null
        val response = runCatching { model.generateContent(buildSummaryPrompt(text)) }.getOrNull() ?: return null
        val answer = response.candidates.firstOrNull()?.text
            ?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotBlank() }
            ?.removePrefix("요약:")?.trim()
            ?.trim('"', '\'', '「', '」', '“', '”')
        return answer?.takeIf { it.isNotBlank() }?.take(60)
    }

    private fun buildSummaryPrompt(ocrText: String): String {
        val text = ocrText.take(3000)
        return """
            다음은 스마트폰 스크린샷에서 읽은 텍스트야. 이게 무슨 내용인지 한국어 한 문장으로 짧고 자연스럽게 요약해줘.
            군더더기 없이 핵심만, 40자 이내로.
            텍스트: ```$text```
            요약:
        """.trimIndent()
    }

    /** 앱 시작 시 호출: 다운로드 가능 상태면 모델을 미리 받아 첫 분류부터 Nano가 되게 한다. */
    suspend fun prewarm() {
        if (runCatching { model.checkStatus() }.getOrNull() == FeatureStatus.DOWNLOADABLE) {
            ensureDownload()
        }
    }

    /** 최초 1회 모델 다운로드를 백그라운드로 시작한다(비차단). */
    private fun ensureDownload() {
        if (downloadStarted) return
        downloadStarted = true
        scope.launch { runCatching { model.download().collect {} } }
    }

    private fun buildPrompt(ocrText: String, categories: List<Category>): String {
        val text = ocrText.take(3000)
        val options = categories.joinToString(", ") { it.label }
        val descriptions = categories.joinToString("\n") { "- ${it.label}: ${it.description}" }
        return """
            다음은 스마트폰 스크린샷에서 읽은 텍스트야. 이 스크린샷을 아래 보기 중 하나로만 분류해서 한국어로 보기 이름 그대로만 답해.
            보기: $options
            $descriptions
            어디에도 안 맞으면 가장 가까운 보기를 골라.
            텍스트: ```$text```
            분류(보기 이름 하나):
        """.trimIndent()
    }

    /** Nano 답을 활성 카테고리로 매핑. 정확 일치 → 라벨 포함 → baseType 동의어. */
    private fun matchCategory(answer: String, categories: List<Category>): Category? {
        val a = answer.trim().removeSuffix(".").trim()
        if (a.isEmpty()) return null
        // 라벨 정확/포함 일치
        categories.firstOrNull { it.label == a }?.let { return it }
        categories.firstOrNull { a.contains(it.label) || it.label.contains(a) }?.let { return it }
        // 보기 밖 자연어("영화"·"팔로우" 등) → baseType 동의어로 흡수 후, 그 baseType의 활성 카테고리로
        val base = synonyms.entries.firstOrNull { (kw, _) -> a.contains(kw) }?.value ?: return null
        val matches = categories.filter { it.baseType == base }
        return matches.firstOrNull { it.isBuiltIn } ?: matches.firstOrNull()
    }

    private companion object {
        // Nano가 보기 밖 단어로 답할 때 baseType으로 흡수. 더 구체적 키워드를 앞에.
        val synonyms = linkedMapOf(
            "쇼핑" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "예매" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "예약" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "주문" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "결제" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "상품" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "영화" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "티켓" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "항공" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "숙소" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "배송" to com.example.petling.domain.model.CaptureType.SHOPPING,
            "공부" to com.example.petling.domain.model.CaptureType.STUDY,
            "학습" to com.example.petling.domain.model.CaptureType.STUDY,
            "시험" to com.example.petling.domain.model.CaptureType.STUDY,
            "과제" to com.example.petling.domain.model.CaptureType.STUDY,
            "강의" to com.example.petling.domain.model.CaptureType.STUDY,
            "수업" to com.example.petling.domain.model.CaptureType.STUDY,
            "등록금" to com.example.petling.domain.model.CaptureType.STUDY,
            "장소" to com.example.petling.domain.model.CaptureType.PLACE,
            "지도" to com.example.petling.domain.model.CaptureType.PLACE,
            "길찾기" to com.example.petling.domain.model.CaptureType.PLACE,
            "맛집" to com.example.petling.domain.model.CaptureType.PLACE,
            "위치" to com.example.petling.domain.model.CaptureType.PLACE,
            "식당" to com.example.petling.domain.model.CaptureType.PLACE,
            "카페" to com.example.petling.domain.model.CaptureType.PLACE,
            "대화" to com.example.petling.domain.model.CaptureType.CHAT,
            "채팅" to com.example.petling.domain.model.CaptureType.CHAT,
            "메신저" to com.example.petling.domain.model.CaptureType.CHAT,
            "카톡" to com.example.petling.domain.model.CaptureType.CHAT,
            "메시지" to com.example.petling.domain.model.CaptureType.CHAT,
            "문자" to com.example.petling.domain.model.CaptureType.CHAT,
            "링크" to com.example.petling.domain.model.CaptureType.LINK,
            "영상" to com.example.petling.domain.model.CaptureType.LINK,
            "유튜브" to com.example.petling.domain.model.CaptureType.LINK,
            "인스타" to com.example.petling.domain.model.CaptureType.LINK,
            "게시물" to com.example.petling.domain.model.CaptureType.LINK,
            "피드" to com.example.petling.domain.model.CaptureType.LINK,
            "팔로우" to com.example.petling.domain.model.CaptureType.LINK,
            "좋아요" to com.example.petling.domain.model.CaptureType.LINK,
            "기사" to com.example.petling.domain.model.CaptureType.LINK,
            "웹" to com.example.petling.domain.model.CaptureType.LINK,
            "댓글" to com.example.petling.domain.model.CaptureType.LINK,
            "일정" to com.example.petling.domain.model.CaptureType.SCHEDULE,
            "약속" to com.example.petling.domain.model.CaptureType.SCHEDULE,
            "예정" to com.example.petling.domain.model.CaptureType.SCHEDULE,
            "행사" to com.example.petling.domain.model.CaptureType.SCHEDULE,
            "추억" to com.example.petling.domain.model.CaptureType.MEMORY,
            "사진" to com.example.petling.domain.model.CaptureType.MEMORY,
            "기타" to com.example.petling.domain.model.CaptureType.MEMORY,
        )
    }
}
