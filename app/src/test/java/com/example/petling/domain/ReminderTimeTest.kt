package com.example.petling.domain

import com.example.petling.data.repository.ReminderTime
import com.example.petling.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ReminderTimeTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private fun millis(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    @Test fun null_offset_means_no_reminder() {
        val date = LocalDate.of(2026, 3, 15)
        val schedule = Schedule(title = "상담", date = date, startMinuteOfDay = 15 * 60, reminderOffsetMin = null)
        val now = millis(date.minusDays(5), 0)
        assertTrue(ReminderTime.plan(schedule, now, zone).isEmpty())
    }

    @Test fun timed_event_reminds_offset_minutes_before_start() {
        val date = LocalDate.of(2026, 3, 15)
        // 오후 3시, 10분 전 알림
        val schedule = Schedule(title = "상담", date = date, startMinuteOfDay = 15 * 60, reminderOffsetMin = 10)
        val now = millis(date.minusDays(5), 0)
        val plan = ReminderTime.plan(schedule, now, zone)
        assertEquals(2, plan.size)
        assertEquals(millis(date.minusDays(1), ReminderTime.DAY_BEFORE_HOUR), plan[0]) // 전날 저녁 예고
        assertEquals(millis(date, 14, 50), plan[1])                                     // 시작 10분 전
    }

    @Test fun offset_zero_reminds_exactly_at_start() {
        val date = LocalDate.of(2026, 3, 15)
        val schedule = Schedule(title = "상담", date = date, startMinuteOfDay = 15 * 60, reminderOffsetMin = 0)
        val now = millis(date, 12) // 같은 날 정오 → 전날 예고는 이미 지남
        val plan = ReminderTime.plan(schedule, now, zone)
        assertEquals(1, plan.size)
        assertEquals(millis(date, 15), plan[0]) // 정시
    }

    @Test fun past_reminders_are_filtered() {
        val date = LocalDate.of(2026, 3, 15)
        val schedule = Schedule(title = "상담", date = date, startMinuteOfDay = 15 * 60, reminderOffsetMin = 10)
        val now = millis(date, 15) // 시작 시각 이후 → 남는 알림 없음
        assertTrue(ReminderTime.plan(schedule, now, zone).isEmpty())
    }

    @Test fun all_day_event_has_day_before_and_day_of() {
        val date = LocalDate.of(2026, 3, 15)
        val schedule = Schedule(title = "종일", date = date, startMinuteOfDay = null, reminderOffsetMin = 10)
        val now = millis(date.minusDays(2), 0)
        val plan = ReminderTime.plan(schedule, now, zone)
        assertEquals(2, plan.size)
        assertEquals(millis(date.minusDays(1), ReminderTime.DAY_BEFORE_HOUR), plan[0])
        assertEquals(millis(date, ReminderTime.DAY_OF_HOUR), plan[1])
    }

    @Test fun all_day_event_with_null_offset_has_no_reminder() {
        val date = LocalDate.of(2026, 3, 15)
        val schedule = Schedule(title = "종일", date = date, startMinuteOfDay = null, reminderOffsetMin = null)
        val now = millis(date.minusDays(2), 0)
        assertTrue(ReminderTime.plan(schedule, now, zone).isEmpty())
    }
}
