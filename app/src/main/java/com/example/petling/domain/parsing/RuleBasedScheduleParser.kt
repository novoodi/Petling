package com.example.petling.domain.parsing

import com.example.petling.domain.AppClock
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.model.ScheduleSource
import java.time.LocalDate
import java.time.LocalTime

/**
 * 규칙 기반 파서 구현. 모든 기기에서 동작하는 폴백 엔진.
 * 실제 파싱 로직은 순수 함수 KoreanScheduleParser에 있다.
 */
class RuleBasedScheduleParser(
    private val clock: AppClock,
    private val source: ScheduleSource = ScheduleSource.CAPTURE,
) : ScheduleParser {

    override suspend fun parse(rawText: String): List<ParsedDraftSeed> =
        parseAt(rawText, clock.today())

    override suspend fun parse(rawText: String, hint: ParseHint): List<ParsedDraftSeed> =
        parseAt(rawText, clock.today(), hint)

    /** 테스트용: 기준일을 직접 넘겨 결정적으로 파싱. */
    fun parseAt(rawText: String, today: LocalDate, hint: ParseHint = ParseHint()): List<ParsedDraftSeed> =
        KoreanScheduleParser.parse(
            rawText,
            today,
            source,
            KoreanScheduleParser.ParseOptions(dateCarryAcrossLines = hint.dateCarryAcrossLines),
        ).map { it.toSeed() }
}

fun KoreanScheduleParser.ParsedLine.toSeed(): ParsedDraftSeed {
    val minuteOfDay = draft.time?.let {
        runCatching { LocalTime.parse(it).let { t -> t.hour * 60 + t.minute } }.getOrNull()
    }
    val date = draft.dateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return ParsedDraftSeed(
        title = draft.title,
        date = date,
        startMinuteOfDay = minuteOfDay,
        location = draft.location,
        category = category,
        isImportant = isImportant,
        confidence = draft.confidence,
        source = draft.source,
    )
}
