package com.example.petling.data.parsing

import com.example.petling.data.capture.NanoLog
import com.example.petling.domain.AppClock
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.model.ScheduleSource
import com.example.petling.domain.parsing.KoreanScheduleParser
import com.example.petling.domain.parsing.ParsedScheduleDraft
import com.example.petling.domain.parsing.RelativeDateGuard
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

    private companion object {
        const val RETRY_DELAY_MS = 400L
    }

    override suspend fun parse(rawText: String): List<ParsedDraftSeed> {
        if (rawText.isBlank()) return emptyList()
        // Nano가 준비된 기기에서만 시도. 그 외에는 규칙 파서로 폴백.
        val status = runCatching { model.checkStatus() }.getOrNull()
        if (status != FeatureStatus.AVAILABLE) {
            NanoLog.d("parser", "status_fail", "status=$status")
            return emptyList()
        }

        // 생성 실패는 일시적인 경우가 많아(모델 콜드/바쁨) 1회 재시도 후에만 규칙 폴백.
        val prompt = buildPrompt(rawText, clock.today())
        var response = runCatching { model.generateContent(prompt) }.getOrNull()
        if (response == null) {
            NanoLog.d("parser", "gen_retry", "textLen=${rawText.length}")
            // 즉시 실패(같은 ms 재실패 관찰) 대비: 짧게 쉬고 재시도해야 실효가 있다.
            kotlinx.coroutines.delay(RETRY_DELAY_MS)
            response = runCatching { model.generateContent(prompt) }.getOrNull()
        }
        if (response == null) {
            NanoLog.d("parser", "gen_fail", "textLen=${rawText.length}")
            return emptyList()
        }
        val answer = response.candidates.firstOrNull()?.text.orEmpty()
        // Nano의 상대 날짜 산술("토요일"→오늘 반환)은 신뢰하지 않는다 — 코드가 검증·보정.
        val today = clock.today()
        var dateFixes = 0
        var timeFills = 0
        val parsedSeeds = parseDrafts(answer).mapNotNull { it.toSeed(source) }
        val seeds = parsedSeeds.map { seed ->
            var s = seed
            val verified = RelativeDateGuard.verify(rawText, s.date, today)
            if (verified != s.date) {
                dateFixes++
                s = s.copy(date = verified)
            }
            // Nano가 "11시"를 HH:mm로 못 바꿔 시간이 비는 케이스 — 단일 일정일 때만
            // 규칙 추출로 보강(다중 일정은 어느 시드의 시간인지 모호). Nano 값은 덮지 않는다.
            if (parsedSeeds.size == 1 && s.startMinuteOfDay == null) {
                KoreanScheduleParser.extractTimeMinute(rawText)?.let {
                    timeFills++
                    s = s.copy(startMinuteOfDay = it)
                }
            }
            s
        }
        if (dateFixes > 0) NanoLog.d("parser", "date_fix", "count=$dateFixes")
        if (timeFills > 0) NanoLog.d("parser", "time_fill", "count=$timeFills")
        if (seeds.isEmpty()) {
            NanoLog.d("parser", "empty", "answerLen=${answer.length}")
        } else {
            NanoLog.d("parser", "ok", "seeds=${seeds.size} textLen=${rawText.length}")
        }
        return seeds
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
        if (start < 0 || end <= start) {
            NanoLog.d("parser", "json_fail", "no_array answerLen=${answer.length}")
            return emptyList()
        }
        val jsonStr = answer.substring(start, end + 1)
        return runCatching {
            json.decodeFromString(ListSerializer(ParsedScheduleDraft.serializer()), jsonStr)
        }.getOrElse {
            NanoLog.d("parser", "json_fail", "decode answerLen=${answer.length}")
            emptyList()
        }
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
