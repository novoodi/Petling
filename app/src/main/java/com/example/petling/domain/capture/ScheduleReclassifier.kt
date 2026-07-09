package com.example.petling.domain.capture

import com.example.petling.domain.model.CaptureType
import com.example.petling.domain.model.Category
import com.example.petling.domain.model.ParsedDraftSeed

/**
 * 2-pass 일정 재조정 (순수 함수).
 *
 * 의도 우선 파이프라인에서 분류기는 파서 결과를 모른다. 대신 정책 적용 후
 * 강한 날짜+시간 seed가 나오면, catch-all(추억)이거나 저신뢰인 1-pass 분류를
 * 일정으로 승격한다. CHAT/SHOPPING 등 뚜렷한 의도는 유지 — 캘린더 자동 등록은
 * 어차피 분류와 무관하게 timedSeed로 동작한다.
 */
object ScheduleReclassifier {

    /** 이 이상 확신하는 timed seed만 승격 근거로 인정. */
    const val PROMOTE_MIN_SEED_CONFIDENCE = 0.75f

    /** 짧은 이벤트 메모만 일정으로 — 긴 게시물/공지는 승격하지 않음. */
    const val MAX_LINES = 6

    /** 1-pass 분류가 이 이상 확신하면(그리고 catch-all이 아니면) 유지. */
    const val KEEP_CLASSIFICATION_CONFIDENCE = 0.6f

    fun maybePromote(
        classification: Classification,
        timedSeed: ParsedDraftSeed?,
        lineCount: Int,
        categories: List<Category>,
    ): Classification {
        if (timedSeed == null || timedSeed.confidence < PROMOTE_MIN_SEED_CONFIDENCE) return classification
        if (lineCount > MAX_LINES) return classification

        val currentBase = categories.firstOrNull { it.key == classification.categoryKey }?.baseType
        val weakOrCatchAll = currentBase == CaptureType.MEMORY ||
            classification.confidence < KEEP_CLASSIFICATION_CONFIDENCE
        if (!weakOrCatchAll) return classification

        val scheduleMatches = categories.filter { it.baseType == CaptureType.SCHEDULE }
        val scheduleKey = (scheduleMatches.firstOrNull { it.isBuiltIn } ?: scheduleMatches.firstOrNull())?.key
            ?: return classification

        val title = timedSeed.title?.takeIf { it.isNotBlank() } ?: classification.title
        return Classification(categoryKey = scheduleKey, title = title, confidence = 0.8f)
    }
}
