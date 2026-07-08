package com.example.petling.domain

import com.example.petling.domain.engine.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 7, 7)

    @Test
    fun first_ever_completion_starts_at_one() {
        assertEquals(1, StreakCalculator.onCompletion(null, today, 0))
    }

    @Test
    fun same_day_additional_completion_keeps_streak() {
        assertEquals(4, StreakCalculator.onCompletion(today, today, 4))
    }

    @Test
    fun same_day_but_zero_streak_becomes_one() {
        // 방어적: 완료 이력은 오늘인데 스트릭이 0인 비정상 상태 -> 최소 1
        assertEquals(1, StreakCalculator.onCompletion(today, today, 0))
    }

    @Test
    fun yesterday_completion_extends_streak() {
        assertEquals(5, StreakCalculator.onCompletion(today.minusDays(1), today, 4))
    }

    @Test
    fun gap_resets_streak_to_one() {
        assertEquals(1, StreakCalculator.onCompletion(today.minusDays(2), today, 9))
    }

    @Test
    fun boundary_from_two_to_three_days() {
        // 이틀 연속 뒤 오늘 완료 -> 3일째, 이 값으로 배율이 계산되어야 함
        val newStreak = StreakCalculator.onCompletion(today.minusDays(1), today, 2)
        assertEquals(3, newStreak)
    }

    @Test
    fun month_boundary_is_handled_by_localdate() {
        val firstOfMonth = LocalDate.of(2026, 8, 1)
        val lastOfPrevMonth = LocalDate.of(2026, 7, 31)
        assertEquals(3, StreakCalculator.onCompletion(lastOfPrevMonth, firstOfMonth, 2))
    }

    @Test
    fun year_boundary_is_handled() {
        val newYear = LocalDate.of(2027, 1, 1)
        val lastDay = LocalDate.of(2026, 12, 31)
        assertEquals(6, StreakCalculator.onCompletion(lastDay, newYear, 5))
    }

    // --- streakEndingAt: 완료 취소 시 이력 기반 재계산 ---

    @Test
    fun streakEndingAt_counts_consecutive_run_from_anchor() {
        val d = today.toEpochDay()
        // 오늘, 어제, 그제 연속 완료 -> 3
        val days = setOf(d, d - 1, d - 2)
        assertEquals(3, StreakCalculator.streakEndingAt(d, days))
    }

    @Test
    fun streakEndingAt_stops_at_gap() {
        val d = today.toEpochDay()
        // 오늘, 어제 완료했지만 그제는 공백 -> 2
        val days = setOf(d, d - 1, d - 3)
        assertEquals(2, StreakCalculator.streakEndingAt(d, days))
    }

    @Test
    fun streakEndingAt_null_or_empty_is_zero() {
        assertEquals(0, StreakCalculator.streakEndingAt(null, emptySet()))
        assertEquals(0, StreakCalculator.streakEndingAt(today.toEpochDay(), emptySet()))
    }

    @Test
    fun streakEndingAt_matches_incremental_after_revert() {
        // 어제까지 3일 스트릭 상태에서 오늘 완료 -> 4 (incremental)
        val d = today.toEpochDay()
        val incremental = StreakCalculator.onCompletion(today.minusDays(1), today, 3)
        assertEquals(4, incremental)
        // 오늘 완료를 취소하면 남는 이력은 어제까지 연속 3일 -> streakEndingAt(어제)=3
        val remaining = setOf(d - 1, d - 2, d - 3)
        assertEquals(3, StreakCalculator.streakEndingAt(d - 1, remaining))
    }
}
