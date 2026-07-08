package com.example.petling.domain.parsing

import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.model.ScheduleSource
import kotlinx.serialization.Serializable

/**
 * LLM(Gemini Nano) 파이프라인이 채우게 될 파싱 결과 JSON 스키마.
 * 기술스택_아키텍처.md 3.3 스키마 준거. 규칙 파서는 이 DTO를 거쳐 seed로 변환된다.
 */
@Serializable
data class ParsedScheduleDraft(
    val title: String? = null,
    /** ISO-8601 날짜 문자열 (yyyy-MM-dd). */
    val dateIso: String? = null,
    /** HH:mm 형식 시각. null이면 종일. */
    val time: String? = null,
    val location: String? = null,
    val confidence: Float = 0f,
    val source: ScheduleSource = ScheduleSource.MANUAL,
)

/**
 * 원시 텍스트(OCR/STT 결과)에서 일정 seed를 추출하는 파서.
 * 구현: RuleBasedScheduleParser(전 기기), (미래) GeminiNanoParser(지원 기기).
 * CompositeScheduleParser가 LLM → 규칙 폴백 순으로 조합한다.
 */
interface ScheduleParser {
    suspend fun parse(rawText: String): List<ParsedDraftSeed>
}
