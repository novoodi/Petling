package com.example.petling.data.parsing

import com.example.petling.domain.AppClock
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.model.ScheduleSource
import com.example.petling.domain.parsing.ParsedScheduleDraft
import com.example.petling.domain.parsing.ScheduleParser
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

/**
 * Gemini Nano(ML Kit GenAI Prompt API)로 원시 텍스트(OCR/STT/붙여넣기)에서 일정을 구조화 추출한다.
 *
 * 설계 원칙:
 * - 지원 기기(AICore+Nano)에서만 동작. 미지원·실패·빈/저품질 결과 시 빈 리스트를 반환해
 *   CompositeScheduleParser가 규칙 파서로 폴백한다(품질 저하·다중 일정 유실 방지).
 * - 한 텍스트에 여러 일정이 섞이면 JSON 배열로 분리 추출한다(기획 P0: 시간표 등 다중 일정).
 * - 상대 날짜("내일","다음 주 화요일")는 오늘 기준으로 프롬프트에 명시해 절대 날짜로 계산시킨다.
 * - 모든 결과는 확인/수정 화면을 거치므로, 오파싱은 사용자가 저장 전 교정한다.
 * - 앱이 절대 죽지 않도록 상태 확인·생성·파싱을 전부 runCatching으로 감싼다.
 */
class GeminiNanoScheduleParser(
    private val clock: AppClock,
    private val source: ScheduleSource,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ScheduleParser {

    private val model: GenerativeModel by lazy { Generation.getClient() }

    override suspend fun parse(rawText: String): List<ParsedDraftSeed> {
        if (rawText.isBlank()) return emptyList()
        // Nano가 준비된 기기에서만 시도. 그 외에는 규칙 파서로 폴백.
        if (runCatching { model.checkStatus() }.getOrNull() != FeatureStatus.AVAILABLE) return emptyList()

        val response = runCatching { model.generateContent(buildPrompt(rawText, clock.today())) }.getOrNull()
            ?: return emptyList()
        val answer = response.candidates.firstOrNull()?.text.orEmpty()
        return parseDrafts(answer).mapNotNull { it.toSeed(source) }
    }

    private fun buildPrompt(rawText: String, today: LocalDate): String {
        val text = rawText.take(2000)
        return """
            오늘은 $today (yyyy-MM-dd)야. 아래 텍스트에서 일정(약속·할 일·행사)을 모두 찾아 JSON 배열로만 답해.
            각 원소 형식: {"title":"제목","dateIso":"yyyy-MM-dd","time":"HH:mm","location":"장소"}
            규칙:
            - "내일","이번 주말","다음 주 화요일" 같은 표현은 오늘 기준 실제 날짜로 계산해 dateIso에 넣어.
            - 값이 없으면 해당 필드를 null로. 일정이 하나도 없으면 빈 배열 [] 로 답해.
            - JSON 배열 외의 다른 말은 절대 하지 마.
            텍스트: ```$text```
            JSON:
        """.trimIndent()
    }

    /** 응답에서 첫 JSON 배열만 추출해 파싱. 실패 시 빈 리스트. */
    private fun parseDrafts(answer: String): List<ParsedScheduleDraft> {
        val start = answer.indexOf('[')
        val end = answer.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        val jsonStr = answer.substring(start, end + 1)
        return runCatching {
            json.decodeFromString(ListSerializer(ParsedScheduleDraft.serializer()), jsonStr)
        }.getOrDefault(emptyList())
    }
}

/** LLM이 낸 draft를 편집 화면 seed로 변환. 제목이 없으면(일정 아님) null. */
private fun ParsedScheduleDraft.toSeed(source: ScheduleSource): ParsedDraftSeed? {
    fun String?.clean(): String? =
        this?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

    val cleanTitle = title.clean() ?: return null
    val date = dateIso.clean()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val minute = time.clean()?.let {
        runCatching { LocalTime.parse(it).let { t -> t.hour * 60 + t.minute } }.getOrNull()
    }
    return ParsedDraftSeed(
        title = cleanTitle,
        date = date,
        startMinuteOfDay = minute,
        location = location.clean(),
        confidence = 0.9f,
        source = source,
    )
}
