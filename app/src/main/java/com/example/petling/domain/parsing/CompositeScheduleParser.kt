package com.example.petling.domain.parsing

import com.example.petling.domain.model.ParsedDraftSeed

/**
 * LLM 파서(지원 기기) → 규칙 파서(전 기기) 순으로 시도하는 조합 파서.
 * primary가 null이거나 빈 결과를 내면 fallback으로 넘어간다.
 *
 * 사용자에게는 기술적 차이를 노출하지 않고 "AI 인식"으로 안내한다(기술 문서 3.1).
 */
class CompositeScheduleParser(
    private val primary: ScheduleParser?,
    private val fallback: ScheduleParser,
) : ScheduleParser {

    override suspend fun parse(rawText: String): List<ParsedDraftSeed> {
        primary?.let { p ->
            val result = runCatching { p.parse(rawText) }.getOrNull()
            if (!result.isNullOrEmpty()) return result
        }
        return fallback.parse(rawText)
    }
}
