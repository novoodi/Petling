package com.example.petling.domain

import com.example.petling.domain.model.ScheduleCategory
import com.example.petling.domain.parsing.KoreanScheduleParser
import com.example.petling.domain.parsing.toSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class KoreanScheduleParserTest {

    // 2026-07-07 은 화요일
    private val today = LocalDate.of(2026, 7, 7)

    private fun parseOne(text: String) =
        KoreanScheduleParser.parse(text, today).firstOrNull()?.toSeed()

    @Test
    fun absolute_month_day_and_time() {
        val s = parseOne("3월 15일 오후 3시 학원 상담")!!
        assertEquals(LocalDate.of(2027, 3, 15), s.date) // 이미 지난 달 → 내년
        assertEquals(15 * 60, s.startMinuteOfDay)
        assertTrue(s.title!!.contains("상담"))
        assertEquals(ScheduleCategory.STUDY, s.category)
    }

    @Test
    fun iso_date() {
        val s = parseOne("2026-07-14 15:00 상담")!!
        assertEquals(LocalDate.of(2026, 7, 14), s.date)
        assertEquals(15 * 60, s.startMinuteOfDay)
    }

    @Test
    fun tomorrow_relative() {
        val s = parseOne("내일 오전 9시 등교")!!
        assertEquals(today.plusDays(1), s.date)
        assertEquals(9 * 60, s.startMinuteOfDay)
    }

    @Test
    fun day_after_tomorrow() {
        assertEquals(today.plusDays(2), parseOne("모레 시험")!!.date)
    }

    @Test
    fun next_week_weekday() {
        val s = parseOne("다음 주 화요일 3시 미팅")!!
        // 다음 주(2026-07-13 월요일 시작)의 화요일 = 2026-07-14
        assertEquals(LocalDate.of(2026, 7, 14), s.date)
        assertEquals(DayOfWeek.TUESDAY, s.date!!.dayOfWeek)
    }

    @Test
    fun this_week_weekday() {
        val s = parseOne("이번 주 금요일 오후 2시 병원")!!
        assertEquals(LocalDate.of(2026, 7, 10), s.date) // 같은 주 금요일
        assertEquals(14 * 60, s.startMinuteOfDay)
    }

    @Test
    fun bare_weekday_is_next_or_same() {
        val s = parseOne("목요일 스터디")!!
        assertEquals(DayOfWeek.THURSDAY, s.date!!.dayOfWeek)
        assertTrue(!s.date!!.isBefore(today))
    }

    @Test
    fun weekend_maps_to_saturday() {
        assertEquals(DayOfWeek.SATURDAY, parseOne("이번 주말 영화")!!.date!!.dayOfWeek)
    }

    @Test
    fun half_hour_notation() {
        val s = parseOne("내일 3시 반 약속")!!
        assertEquals(15 * 60 + 30, s.startMinuteOfDay)
    }

    @Test
    fun explicit_morning_afternoon() {
        assertEquals(9 * 60, parseOne("내일 오전 9시 수업")!!.startMinuteOfDay)
        assertEquals(21 * 60, parseOne("내일 밤 9시 게임")!!.startMinuteOfDay)
        assertEquals(0, parseOne("내일 자정 마감")!!.startMinuteOfDay)
        assertEquals(12 * 60, parseOne("내일 정오 약속")!!.startMinuteOfDay)
    }

    @Test
    fun bare_hour_heuristic_afternoon() {
        // 오전/오후 없는 3시 → 오후로 간주
        assertEquals(15 * 60, parseOne("내일 3시 학원")!!.startMinuteOfDay)
    }

    @Test
    fun location_with_eseo() {
        val s = parseOne("내일 2시 스타벅스에서 만남")!!
        assertEquals("스타벅스", s.location)
        assertEquals(ScheduleCategory.APPOINTMENT, s.category)
    }

    @Test
    fun location_keyword() {
        val s = parseOne("금요일 3시 강남학원")!!
        assertNotNull(s.location)
        assertTrue(s.location!!.contains("학원"))
    }

    @Test
    fun important_keyword_flags_importance() {
        assertTrue(parseOne("내일 오전 10시 중간고사 시험")!!.isImportant)
        assertTrue(!parseOne("내일 3시 카페")!!.isImportant)
    }

    @Test
    fun multiple_schedules_from_timetable() {
        val text = """
            월요일 9시 국어
            화요일 10시 수학
            수요일 오후 1시 영어
        """.trimIndent()
        val results = KoreanScheduleParser.parse(text, today)
        assertEquals(3, results.size)
        assertTrue(results.all { it.draft.dateIso != null })
    }

    @Test
    fun bullet_markers_are_stripped() {
        val text = "• 내일 3시 학원\n- 모레 병원 예약"
        val results = KoreanScheduleParser.parse(text, today).map { it.toSeed() }
        assertEquals(2, results.size)
        assertTrue(results[0].title!!.none { it == '•' })
    }

    @Test
    fun text_without_datetime_is_low_confidence_title_only() {
        val results = KoreanScheduleParser.parse("친구랑 놀기", today).map { it.toSeed() }
        assertEquals(1, results.size)
        assertNull(results[0].date)
        assertNull(results[0].startMinuteOfDay)
        assertTrue(results[0].confidence < 0.3f)
    }

    @Test
    fun confidence_higher_with_more_signals() {
        val full = parseOne("3월 15일 오후 3시 강남학원 상담")!!
        val partial = parseOne("친구랑 놀기")!!
        assertTrue(full.confidence > partial.confidence)
        assertTrue(full.confidence >= 0.9f)
    }

    @Test
    fun title_excludes_datetime_and_location_tokens() {
        val s = parseOne("내일 오후 3시 학원에서 수학 과외")!!
        val title = s.title!!
        assertTrue(!title.contains("내일"))
        assertTrue(!title.contains("3시"))
        assertTrue(title.contains("수학") || title.contains("과외"))
    }
}
