package com.example.petling.golden

import com.example.petling.domain.capture.RuleBasedCaptureClassifier
import com.example.petling.domain.capture.ScheduleReclassifier
import com.example.petling.domain.model.BuiltInCatalog
import com.example.petling.domain.model.CaptureType
import com.example.petling.domain.model.Category
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.parsing.IntentParsingStrategy
import com.example.petling.domain.parsing.KoreanScheduleParser
import com.example.petling.domain.parsing.toSeed
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * "분류 → 의도 정책 → 파싱 → 2-pass 재조정" 전체 경로의 순수 실행기.
 * CaptureRepository.ingest()의 파싱 파이프라인과 동일한 조립 —
 * 전부 순수/무의존 구성요소라 JVM 테스트에서 결정적으로 실행된다. Nano 제외.
 */
object GoldenPipeline {

    data class Result(
        val categoryKey: String,
        val seeds: List<ParsedDraftSeed>,
        val promoted: Boolean,
    )

    private val classifier = RuleBasedCaptureClassifier()

    fun run(
        text: String,
        today: LocalDate,
        categories: List<Category> = BuiltInCatalog.defaults,
    ): Result {
        val firstPass = runBlocking { classifier.classify(text, categories) }
        val baseType = categories.firstOrNull { it.key == firstPass.categoryKey }?.baseType
            ?: CaptureType.MEMORY

        val policy = IntentParsingStrategy.policyFor(baseType, firstPass.confidence)
        val parseText = IntentParsingStrategy.applyStrip(policy, text)
        val seeds = IntentParsingStrategy.postProcess(
            policy,
            KoreanScheduleParser.parse(
                parseText,
                today,
                options = KoreanScheduleParser.ParseOptions(dateCarryAcrossLines = policy.dateCarryAcrossLines),
            ).map { it.toSeed() },
        )

        val timedSeed = seeds.firstOrNull { it.date != null && it.startMinuteOfDay != null }
        val lineCount = parseText.lineSequence().count { it.isNotBlank() }
        val final = ScheduleReclassifier.maybePromote(firstPass, timedSeed, lineCount, categories)
        return Result(
            categoryKey = final.categoryKey,
            seeds = seeds,
            promoted = final.categoryKey != firstPass.categoryKey,
        )
    }
}
