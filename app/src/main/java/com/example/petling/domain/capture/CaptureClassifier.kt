package com.example.petling.domain.capture

import com.example.petling.domain.model.Category

/** OCR 텍스트를 캡처 카테고리로 분류한 결과. [categoryKey]는 활성 카테고리의 key. */
data class Classification(
    val categoryKey: String,
    val title: String,
    val confidence: Float,
)

/**
 * 캡처 분류기. 사용자의 활성 카테고리 집합([categories]) 안에서만 분류한다.
 *
 * @param parsedHasSchedule 규칙 파서가 날짜+시간을 뽑아냈는지(=일정성 캡처 힌트).
 * @param categories 활성 카테고리(비어 있지 않다고 가정; 항상 catch-all 포함).
 */
interface CaptureClassifier {
    suspend fun classify(
        ocrText: String,
        parsedHasSchedule: Boolean,
        scheduleTitle: String?,
        categories: List<Category>,
    ): Classification
}

/** LLM(Nano) → 규칙 폴백. primary가 null이거나 실패하면 규칙으로 폴백. */
class CompositeCaptureClassifier(
    private val primary: CaptureClassifier?,
    private val fallback: CaptureClassifier,
) : CaptureClassifier {
    override suspend fun classify(
        ocrText: String,
        parsedHasSchedule: Boolean,
        scheduleTitle: String?,
        categories: List<Category>,
    ): Classification {
        primary?.let { p ->
            runCatching { p.classify(ocrText, parsedHasSchedule, scheduleTitle, categories) }
                .getOrNull()?.let { return it }
        }
        return fallback.classify(ocrText, parsedHasSchedule, scheduleTitle, categories)
    }
}
