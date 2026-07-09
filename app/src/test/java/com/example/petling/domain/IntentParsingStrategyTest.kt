package com.example.petling.domain

import com.example.petling.domain.model.CaptureType
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.parsing.IntentParsingStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IntentParsingStrategyTest {

    private fun seed(confidence: Float, timed: Boolean = true) = ParsedDraftSeed(
        title = "제목",
        date = LocalDate.of(2026, 7, 8),
        startMinuteOfDay = if (timed) 15 * 60 else null,
        confidence = confidence,
    )

    @Test
    fun low_classification_confidence_uses_default_policy() {
        // 분류가 애매하면(0.6 미만) 의도를 신뢰하지 않는다
        val policy = IntentParsingStrategy.policyFor(CaptureType.SHOPPING, 0.55f)
        assertEquals(IntentParsingStrategy.DEFAULT, policy)
    }

    @Test
    fun chat_strips_line_edge_timestamps_but_keeps_inline_time() {
        val policy = IntentParsingStrategy.policyFor(CaptureType.CHAT, 0.7f)
        val text = "민지\n오후 2:31\n내일 오후 2:30에 보자 오후 2:32\n좋아"
        val stripped = IntentParsingStrategy.applyStrip(policy, text)
        // 줄 머리/꼬리 타임스탬프는 제거
        assertTrue(!stripped.contains("2:31"))
        assertTrue(!stripped.contains("2:32"))
        // 본문 중간의 진짜 약속 시각은 보존
        assertTrue(stripped.contains("오후 2:30에 보자"))
    }

    @Test
    fun study_enables_date_carry() {
        val policy = IntentParsingStrategy.policyFor(CaptureType.STUDY, 0.65f)
        assertTrue(policy.dateCarryAcrossLines)
        assertTrue(policy.toHint().dateCarryAcrossLines)
    }

    @Test
    fun shopping_below_gate_halves_confidence() {
        val policy = IntentParsingStrategy.policyFor(CaptureType.SHOPPING, 0.7f)
        val out = IntentParsingStrategy.postProcess(policy, listOf(seed(0.9f)))
        assertEquals(0.45f, out.single().confidence, 1e-4f)
    }

    @Test
    fun shopping_at_gate_suppresses_weak_and_untimed_seeds() {
        val policy = IntentParsingStrategy.policyFor(CaptureType.SHOPPING, 0.85f)
        assertTrue(policy.ruleOnly)
        val out = IntentParsingStrategy.postProcess(
            policy,
            listOf(
                seed(0.9f), // 강한 timed — 통과 (파싱을 차단하지 않는다)
                seed(0.6f), // 저신뢰 — 억제
                seed(0.9f, timed = false), // 시간 없음 — 억제
            ),
        )
        assertEquals(1, out.size)
        assertEquals(0.9f, out.single().confidence, 1e-4f)
    }

    @Test
    fun memory_at_gate_suppresses_but_below_gate_is_default() {
        assertTrue(IntentParsingStrategy.policyFor(CaptureType.MEMORY, 0.85f).suppressSeedsBelow != null)
        assertEquals(IntentParsingStrategy.DEFAULT, IntentParsingStrategy.policyFor(CaptureType.MEMORY, 0.7f))
    }

    @Test
    fun schedule_link_place_have_no_special_policy() {
        for (type in listOf(CaptureType.SCHEDULE, CaptureType.LINK, CaptureType.PLACE)) {
            assertEquals(IntentParsingStrategy.DEFAULT, IntentParsingStrategy.policyFor(type, 0.9f))
        }
    }

    @Test
    fun standalone_timestamp_line_is_removed_entirely() {
        val policy = IntentParsingStrategy.policyFor(CaptureType.CHAT, 0.7f)
        val stripped = IntentParsingStrategy.applyStrip(policy, "오후 9:02\n콜")
        assertEquals("콜", stripped.trim())
    }
}
