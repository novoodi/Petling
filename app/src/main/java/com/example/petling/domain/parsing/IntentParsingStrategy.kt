package com.example.petling.domain.parsing

import com.example.petling.domain.model.CaptureType
import com.example.petling.domain.model.ParsedDraftSeed

/**
 * 의도(분류 결과)가 파싱을 조정하기 위한 정책.
 * 원칙: 의도는 파싱을 "조정"하되 "차단"하지 않는다 — 순수 skip 대신
 * 강한 seed만 통과시키는 억제(suppression)를 쓴다. 분류 confidence가
 * 미교정(Nano는 고정 0.9)이어도 최악의 결과가 "약한 오탐 제거"에 그친다.
 */
data class ParsingPolicy(
    /** 파싱 전 텍스트에서 제거할 패턴(예: 카톡 말풍선 타임스탬프). */
    val stripPatterns: List<Regex> = emptyList(),
    /** LLM 파서 생략(지연 절약) — 규칙 파서만 사용. */
    val ruleOnly: Boolean = false,
    /** 파싱 후 이 값 미만 confidence seed 제거. */
    val suppressSeedsBelow: Float? = null,
    /** 억제 시 날짜+시간이 모두 있는 seed만 유지. */
    val requireTimedSeed: Boolean = false,
    /** 결과 confidence 보정 배수. */
    val confidenceMul: Float = 1f,
    /** 시간표: 요일 헤더 날짜를 이후 시간 줄에 전파. */
    val dateCarryAcrossLines: Boolean = false,
) {
    fun toHint(): ParseHint = ParseHint(ruleOnly = ruleOnly, dateCarryAcrossLines = dateCarryAcrossLines)
}

/** 의도(CaptureType) → 파싱 정책 순수 테이블. */
object IntentParsingStrategy {

    /** 분류 confidence가 이 미만이면 의도를 신뢰하지 않고 기본 정책으로 파싱한다. */
    const val MIN_CLASSIFICATION_CONFIDENCE = 0.6f

    /** 이 이상 확신할 때만 억제 정책을 켠다. */
    const val SUPPRESS_GATE_CONFIDENCE = 0.8f

    val DEFAULT = ParsingPolicy()

    /**
     * 카톡 말풍선 타임스탬프 "(오전|오후) h:mm"은 줄 머리/꼬리에 위치한다.
     * 줄 경계에만 앵커링해 본문 중간의 진짜 약속 시각("오후 2:30에 보자")은 살린다.
     */
    private val CHAT_TIMESTAMPS = listOf(
        Regex("^\\s*(오전|오후)\\s*\\d{1,2}:\\d{2}\\s*", RegexOption.MULTILINE),
        Regex("\\s*(오전|오후)\\s*\\d{1,2}:\\d{2}\\s*$", RegexOption.MULTILINE),
    )

    /** 강한(date+time, conf 0.75+) seed만 통과. 규칙 파서만 사용해 지연도 절약. */
    private val SUPPRESS = ParsingPolicy(ruleOnly = true, suppressSeedsBelow = 0.75f, requireTimedSeed = true)

    fun policyFor(baseType: CaptureType, classificationConfidence: Float): ParsingPolicy {
        if (classificationConfidence < MIN_CLASSIFICATION_CONFIDENCE) return DEFAULT
        return when (baseType) {
            CaptureType.CHAT -> ParsingPolicy(stripPatterns = CHAT_TIMESTAMPS)
            CaptureType.STUDY -> ParsingPolicy(dateCarryAcrossLines = true)
            CaptureType.SHOPPING ->
                if (classificationConfidence >= SUPPRESS_GATE_CONFIDENCE) SUPPRESS
                else ParsingPolicy(confidenceMul = 0.5f)
            CaptureType.MEMORY ->
                if (classificationConfidence >= SUPPRESS_GATE_CONFIDENCE) SUPPRESS
                else DEFAULT
            else -> DEFAULT // SCHEDULE, LINK, PLACE
        }
    }

    fun applyStrip(policy: ParsingPolicy, text: String): String =
        policy.stripPatterns.fold(text) { acc, regex -> regex.replace(acc, "") }

    fun postProcess(policy: ParsingPolicy, seeds: List<ParsedDraftSeed>): List<ParsedDraftSeed> {
        var result = seeds
        if (policy.confidenceMul != 1f) {
            result = result.map { it.copy(confidence = (it.confidence * policy.confidenceMul).coerceIn(0f, 1f)) }
        }
        policy.suppressSeedsBelow?.let { min ->
            result = result.filter { seed ->
                seed.confidence >= min &&
                    (!policy.requireTimedSeed || (seed.date != null && seed.startMinuteOfDay != null))
            }
        }
        return result
    }
}
