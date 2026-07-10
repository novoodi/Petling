package com.example.petling.domain

import com.example.petling.domain.parsing.RelativeDateGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RelativeDateGuardTest {

    // 2026-07-10 은 금요일
    private val today = LocalDate.of(2026, 7, 10)

    @Test
    fun weekday_mismatch_corrected() {
        // Nano가 "토요일"을 오늘(금)로 잘못 계산한 실기기 관찰 케이스
        val fixed = RelativeDateGuard.verify("토요일 11시 할머니네", today, today)
        assertEquals(LocalDate.of(2026, 7, 11), fixed)
    }

    @Test
    fun weekday_match_kept() {
        val ok = RelativeDateGuard.verify("토요일 11시 할머니네", LocalDate.of(2026, 7, 11), today)
        assertEquals(LocalDate.of(2026, 7, 11), ok)
    }

    @Test
    fun weekday_today_is_that_day() {
        // 오늘이 금요일일 때 "금요일" = 오늘(nextOrSame, 규칙 파서와 동일)
        val fixed = RelativeDateGuard.verify("금요일 6시 저녁 약속", LocalDate.of(2026, 7, 3), today)
        assertEquals(today, fixed)
    }

    @Test
    fun next_week_weekday() {
        // 차주는 월요일 기준: 7/13(월)~ → 토요일 = 7/18
        val fixed = RelativeDateGuard.verify("다음 주 토요일 11시", today, today)
        assertEquals(LocalDate.of(2026, 7, 18), fixed)
    }

    @Test
    fun this_week_weekday() {
        val fixed = RelativeDateGuard.verify("이번 주 일요일 점심", today, today)
        assertEquals(LocalDate.of(2026, 7, 12), fixed)
    }

    @Test
    fun tomorrow_corrected() {
        val fixed = RelativeDateGuard.verify("내일 오후 3시 치과", today, today)
        assertEquals(today.plusDays(1), fixed)
    }

    @Test
    fun day_after_tomorrow_not_confused_with_tomorrow() {
        val fixed = RelativeDateGuard.verify("내일모레 2시 알바", today, today)
        assertEquals(today.plusDays(2), fixed)
    }

    @Test
    fun null_date_filled_from_token() {
        val fixed = RelativeDateGuard.verify("토요일 11시 할머니네", null, today)
        assertEquals(LocalDate.of(2026, 7, 11), fixed)
    }

    @Test
    fun absolute_date_present_untouched() {
        // 절대 날짜가 있으면 Nano 값을 신뢰(요일은 장식일 수 있음)
        val kept = RelativeDateGuard.verify("7월 15일 수요일 저녁 7시", LocalDate.of(2026, 7, 15), today)
        assertEquals(LocalDate.of(2026, 7, 15), kept)
    }

    @Test
    fun mixed_tomorrow_and_weekday_untouched() {
        // 상대 표현 2종 혼합: 어느 시드의 날짜인지 모호 → Nano 값 유지
        val nano = LocalDate.of(2026, 7, 17)
        val kept = RelativeDateGuard.verify("내일 3시 회의, 금요일 2시 시험", nano, today)
        assertEquals(nano, kept)
    }

    @Test
    fun multiple_weekdays_untouched() {
        // 시간표류: 어느 시드의 날짜인지 모호 → 보정 안 함
        val kept = RelativeDateGuard.verify("월요일 수학, 수요일 영어", today, today)
        assertEquals(today, kept)
    }

    @Test
    fun no_token_untouched() {
        assertNull(RelativeDateGuard.verify("할머니네 놀러가기", null, today))
        assertEquals(today, RelativeDateGuard.verify("할머니네 놀러가기", today, today))
    }

    @Test
    fun weekday_shorthand_yol() {
        // 학생 구어체 "토욜"
        val fixed = RelativeDateGuard.verify("토욜 11시 할머니네", today, today)
        assertEquals(LocalDate.of(2026, 7, 11), fixed)
    }
}
