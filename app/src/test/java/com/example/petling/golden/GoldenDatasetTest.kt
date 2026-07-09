package com.example.petling.golden

import com.example.petling.domain.parsing.KoreanScheduleParser
import com.example.petling.domain.parsing.toSeed
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 골든 데이터셋 회귀 게이트. 두 경로를 채점한다:
 * - parser: 규칙 파서 단독 (KoreanScheduleParser)
 * - pipeline: 분류 → 의도 정책 → 파싱 → 2-pass 재조정 (GoldenPipeline)
 * Nano 경로는 기기 의존이라 CI 대상이 아님.
 *
 * 매 실행마다 stdout에 케이스별 표와 새 baseline JSON 전문을 출력한다.
 * 정확도를 개선한 커밋에서는 그 출력을 golden/baseline.json에 수동 반영하고
 * 전/후 수치를 커밋 메시지에 기록한다. 테스트가 baseline을 스스로 덮어쓰지
 * 않는 것은 의도된 설계다(자동 갱신은 회귀 게이트를 무력화한다).
 */
class GoldenDatasetTest {

    private val prettyJson = Json { prettyPrint = true }

    private data class Scored(
        val id: String,
        val intent: String,
        val parser: GoldenScorer.CaseResult,
        val pipeline: GoldenScorer.CaseResult,
        val categoryKey: String,
    )

    private fun scoreAll(): List<Scored> = GoldenDataset.load().map { case ->
        val today = LocalDate.parse(case.today)
        val parserSeeds = KoreanScheduleParser.parse(case.text, today).map { it.toSeed() }
        val pipe = GoldenPipeline.run(case.text, today)
        Scored(
            id = case.id,
            intent = case.intent,
            parser = GoldenScorer.scoreCase(case, parserSeeds),
            pipeline = GoldenScorer.scoreCase(case, pipe.seeds),
            categoryKey = pipe.categoryKey,
        )
    }

    @Test
    fun dataset_is_well_formed() {
        val cases = GoldenDataset.load()
        assertEquals("케이스 수", 30, cases.size)
        assertEquals("id 중복", cases.size, cases.map { it.id }.toSet().size)
        val intents = setOf("chat", "timetable", "shopping", "place", "link", "memory", "plain")
        assertTrue(cases.all { it.intent in intents })
    }

    @Test
    fun totals_meet_baseline() {
        val results = scoreAll()
        val parserTotal = results.sumOf { it.parser.score } / results.size
        val pipelineTotal = results.sumOf { it.pipeline.score } / results.size
        printReport(results, parserTotal, pipelineTotal)
        val baseline = GoldenDataset.baseline()
        assertTrue(
            "파서 총점 회귀: %.4f < baseline %.4f".format(parserTotal, baseline.parserTotal),
            parserTotal + 1e-3 >= baseline.parserTotal,
        )
        assertTrue(
            "파이프라인 총점 회귀: %.4f < baseline %.4f".format(pipelineTotal, baseline.pipelineTotal),
            pipelineTotal + 1e-3 >= baseline.pipelineTotal,
        )
    }

    @Test
    fun no_case_regressed() {
        val baseline = GoldenDataset.baseline()
        val regressed = scoreAll().flatMap { r ->
            val base = baseline.perCase[r.id] ?: return@flatMap emptyList()
            buildList {
                if (r.parser.score + 1e-3 < base.parser) add("${r.id}(parser): %.2f < %.2f".format(r.parser.score, base.parser))
                if (r.pipeline.score + 1e-3 < base.pipeline) add("${r.id}(pipeline): %.2f < %.2f".format(r.pipeline.score, base.pipeline))
            }
        }
        assertTrue("케이스별 회귀: $regressed", regressed.isEmpty())
    }

    private fun printReport(results: List<Scored>, parserTotal: Double, pipelineTotal: Double) {
        println("═══ 골든 데이터셋 리포트 ═══")
        println("  %-10s %-9s %-9s %s".format("id", "parser", "pipeline", "pipeline 분류"))
        results.forEach {
            println("  %-10s %.2f      %.2f      %s".format(it.id, it.parser.score, it.pipeline.score, it.categoryKey))
        }
        println("  총점: parser=%.4f pipeline=%.4f (케이스 %d개)".format(parserTotal, pipelineTotal, results.size))
        printIntentAccuracy(results)
        println("─── 새 baseline.json (개선 커밋에서만 수동 반영) ───")
        val baseline = GoldenBaseline(
            version = 2,
            parserTotal = round4(parserTotal),
            pipelineTotal = round4(pipelineTotal),
            perCase = results.associate { it.id to CaseBaseline(round4(it.parser.score), round4(it.pipeline.score)) },
        )
        println(prettyJson.encodeToString(baseline))
    }

    /** 의도 분류 정확도는 리포트만 — 게이트 아님(파싱 개선과 분리 추적). */
    private fun printIntentAccuracy(results: List<Scored>) {
        val expectedKey = mapOf(
            "chat" to "CHAT", "timetable" to "STUDY", "shopping" to "SHOPPING",
            "place" to "PLACE", "link" to "LINK", "memory" to "MEMORY",
        )
        val judged = results.mapNotNull { r -> expectedKey[r.intent]?.let { it == r.categoryKey } }
        if (judged.isNotEmpty()) {
            println("  참고: 규칙 분류 일치 %d/%d (plain 제외, SCHEDULE 승격은 불일치로 집계)".format(judged.count { it }, judged.size))
        }
    }

    private fun round4(v: Double): Double = Math.round(v * 10000.0) / 10000.0
}
