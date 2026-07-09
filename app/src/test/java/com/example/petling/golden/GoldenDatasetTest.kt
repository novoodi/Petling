package com.example.petling.golden

import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.parsing.KoreanScheduleParser
import com.example.petling.domain.parsing.toSeed
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 골든 데이터셋 회귀 게이트.
 * 규칙 파서만 채점한다(Nano 경로는 기기 의존이라 CI 대상 아님).
 *
 * 매 실행마다 stdout에 케이스별 표와 새 baseline JSON 전문을 출력한다.
 * 정확도를 개선한 커밋에서는 그 출력을 golden/baseline.json에 수동 반영하고
 * 전/후 수치를 커밋 메시지에 기록한다. 테스트가 baseline을 스스로 덮어쓰지
 * 않는 것은 의도된 설계다(자동 갱신은 회귀 게이트를 무력화한다).
 */
class GoldenDatasetTest {

    private val prettyJson = Json { prettyPrint = true }

    private fun parserSeeds(case: GoldenCase): List<ParsedDraftSeed> =
        KoreanScheduleParser.parse(case.text, LocalDate.parse(case.today)).map { it.toSeed() }

    private fun scoreAll(): List<GoldenScorer.CaseResult> =
        GoldenDataset.load().map { GoldenScorer.scoreCase(it, parserSeeds(it)) }

    @Test
    fun dataset_is_well_formed() {
        val cases = GoldenDataset.load()
        assertEquals("케이스 수", 30, cases.size)
        assertEquals("id 중복", cases.size, cases.map { it.id }.toSet().size)
        val intents = setOf("chat", "timetable", "shopping", "place", "link", "memory", "plain")
        assertTrue(cases.all { it.intent in intents })
    }

    @Test
    fun parser_total_meets_baseline() {
        val results = scoreAll()
        val total = results.sumOf { it.score } / results.size
        printReport(results, total)
        val baseline = GoldenDataset.baseline()
        assertTrue(
            "골든셋 총점 회귀: %.4f < baseline %.4f".format(total, baseline.parserTotal),
            total + 1e-3 >= baseline.parserTotal,
        )
    }

    @Test
    fun parser_no_case_regressed() {
        val baseline = GoldenDataset.baseline()
        val regressed = scoreAll().mapNotNull { r ->
            val base = baseline.perCase[r.id] ?: return@mapNotNull null
            if (r.score + 1e-3 < base) "${r.id}: %.2f < %.2f".format(r.score, base) else null
        }
        assertTrue("케이스별 회귀: $regressed", regressed.isEmpty())
    }

    private fun printReport(results: List<GoldenScorer.CaseResult>, total: Double) {
        println("═══ 골든 데이터셋 리포트 (규칙 파서) ═══")
        results.forEach { println("  %-10s %.2f  %s".format(it.id, it.score, it.detail)) }
        println("  총점: %.4f (케이스 %d개)".format(total, results.size))
        println("─── 새 baseline.json (개선 커밋에서만 수동 반영) ───")
        val baseline = GoldenBaseline(
            version = 1,
            parserTotal = round4(total),
            perCase = results.associate { it.id to round4(it.score) },
        )
        println(prettyJson.encodeToString(baseline))
    }

    private fun round4(v: Double): Double = Math.round(v * 10000.0) / 10000.0
}
