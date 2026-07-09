package com.example.petling.domain

import com.example.petling.domain.capture.Classification
import com.example.petling.domain.capture.ScheduleReclassifier
import com.example.petling.domain.model.BuiltInCatalog
import com.example.petling.domain.model.CaptureType
import com.example.petling.domain.model.ParsedDraftSeed
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 2-pass 일정 승격 테스트. 구 RuleBasedCaptureClassifier의 SCHEDULE 하드 규칙
 * (schedule_wins / chat_with_datetime / long_text) 의미를 이 계층에서 보존한다.
 */
class ScheduleReclassifierTest {

    private val cats = BuiltInCatalog.defaults

    private fun timedSeed(confidence: Float = 0.95f) = ParsedDraftSeed(
        title = "상담",
        date = LocalDate.of(2026, 7, 8),
        startMinuteOfDay = 15 * 60,
        confidence = confidence,
    )

    @Test
    fun promotes_memory_fallback_with_strong_timed_seed() {
        // 구 schedule_wins_when_parser_found_datetime: 순수 일정 메모는 SCHEDULE로
        val memory = Classification(BuiltInCatalog.MEMORY, "자동제목", 0.3f)
        val promoted = ScheduleReclassifier.maybePromote(memory, timedSeed(), lineCount = 1, categories = cats)
        assertEquals(CaptureType.SCHEDULE.name, promoted.categoryKey)
        assertEquals("상담", promoted.title) // 제목은 seed 우선
    }

    @Test
    fun promotes_low_confidence_classification() {
        val weakPlace = Classification(CaptureType.PLACE.name, "제목", 0.45f)
        val promoted = ScheduleReclassifier.maybePromote(weakPlace, timedSeed(), lineCount = 2, categories = cats)
        assertEquals(CaptureType.SCHEDULE.name, promoted.categoryKey)
    }

    @Test
    fun keeps_confident_chat_classification() {
        // 구 chat_with_datetime_is_chat_not_schedule: 카톡은 시각이 있어도 대화 유지
        val chat = Classification(CaptureType.CHAT.name, "제목", 0.7f)
        val result = ScheduleReclassifier.maybePromote(chat, timedSeed(), lineCount = 5, categories = cats)
        assertEquals(CaptureType.CHAT.name, result.categoryKey)
    }

    @Test
    fun does_not_promote_long_text() {
        // 구 long_text_with_datetime_is_not_schedule: 긴 게시물은 일정으로 단정하지 않음
        val memory = Classification(BuiltInCatalog.MEMORY, "제목", 0.3f)
        val result = ScheduleReclassifier.maybePromote(memory, timedSeed(), lineCount = 11, categories = cats)
        assertEquals(BuiltInCatalog.MEMORY, result.categoryKey)
    }

    @Test
    fun does_not_promote_weak_seed() {
        val memory = Classification(BuiltInCatalog.MEMORY, "제목", 0.3f)
        val result = ScheduleReclassifier.maybePromote(memory, timedSeed(confidence = 0.5f), lineCount = 1, categories = cats)
        assertEquals(BuiltInCatalog.MEMORY, result.categoryKey)
    }

    @Test
    fun does_not_promote_without_timed_seed() {
        val memory = Classification(BuiltInCatalog.MEMORY, "제목", 0.3f)
        val result = ScheduleReclassifier.maybePromote(memory, timedSeed = null, lineCount = 1, categories = cats)
        assertEquals(BuiltInCatalog.MEMORY, result.categoryKey)
    }

    @Test
    fun no_promotion_when_schedule_category_disabled() {
        val noSchedule = cats.filter { it.key != CaptureType.SCHEDULE.name }
        val memory = Classification(BuiltInCatalog.MEMORY, "제목", 0.3f)
        val result = ScheduleReclassifier.maybePromote(memory, timedSeed(), lineCount = 1, categories = noSchedule)
        assertEquals(BuiltInCatalog.MEMORY, result.categoryKey)
    }
}
