package com.example.petling.golden

import com.example.petling.domain.model.ParsedDraftSeed
import java.time.LocalDate
import kotlin.math.max

/**
 * 골든 케이스 채점기 (순수 함수).
 * 필드 단위 부분 점수: date 0.4 / time 0.3 / title 0.2 / location 0.1.
 * 과잉 추출은 감점 — 기대 0건 케이스는 추출당 -0.5, 매칭 후 남은 초과분은 -0.25.
 */
object GoldenScorer {

    /**
     * 이 임계 미만 confidence이거나 date/time이 모두 없는 seed는 "추출"로 치지 않는다.
     * 파서의 weak 신호 캡(<= 0.3)과 맞물려 저신뢰 오탐을 자동 흡수하는 장치.
     */
    const val EXTRACTION_MIN_CONFIDENCE = 0.35f

    data class CaseResult(val id: String, val score: Double, val detail: String)

    fun scoreCase(case: GoldenCase, actual: List<ParsedDraftSeed>): CaseResult {
        val extractions = actual.filter {
            (it.date != null || it.startMinuteOfDay != null) && it.confidence >= EXTRACTION_MIN_CONFIDENCE
        }
        if (case.expected.isEmpty()) {
            val score = if (extractions.isEmpty()) 1.0 else max(0.0, 1.0 - 0.5 * extractions.size)
            return CaseResult(case.id, score, "기대 0건 / 추출 ${extractions.size}건")
        }

        // 기대 항목별로 아직 안 쓴 추출과 그리디 최적 매칭
        val used = BooleanArray(extractions.size)
        var sum = 0.0
        for (exp in case.expected) {
            var bestIdx = -1
            var best = 0.0
            for ((i, seed) in extractions.withIndex()) {
                if (used[i]) continue
                val s = pairScore(exp, seed)
                if (s > best) {
                    best = s
                    bestIdx = i
                }
            }
            if (bestIdx >= 0) {
                used[bestIdx] = true
                sum += best
            }
        }
        val extra = used.count { !it }
        val score = (sum / case.expected.size - 0.25 * extra).coerceIn(0.0, 1.0)
        return CaseResult(case.id, score, "기대 ${case.expected.size}건 / 추출 ${extractions.size}건 / 초과 $extra")
    }

    private fun pairScore(exp: GoldenSchedule, seed: ParsedDraftSeed): Double {
        var s = 0.0
        if (exp.date?.let(LocalDate::parse) == seed.date) s += 0.4
        if (exp.time?.let(::toMinuteOfDay) == seed.startMinuteOfDay) s += 0.3
        if (textMatches(exp.title, seed.title)) s += 0.2
        if (textMatches(exp.location, seed.location)) s += 0.1
        return s
    }

    private fun toMinuteOfDay(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map(String::toInt)
        return h * 60 + m
    }

    /** null==null 정답, 둘 다 있으면 정규화 후 포함 관계(양방향). */
    private fun textMatches(expected: String?, actual: String?): Boolean {
        if (expected == null && actual == null) return true
        if (expected == null || actual == null) return false
        val e = normalize(expected)
        val a = normalize(actual)
        return e.isNotEmpty() && a.isNotEmpty() && (a.contains(e) || e.contains(a))
    }

    private fun normalize(s: String): String = s.lowercase().replace(Regex("\\s+"), "")
}
