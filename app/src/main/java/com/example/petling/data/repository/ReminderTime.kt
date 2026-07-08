package com.example.petling.data.repository

import com.example.petling.domain.model.Schedule
import java.time.LocalTime
import java.time.ZoneId

/**
 * 비서형 일정 알림 계획.
 * - reminderOffsetMin이 null이면 알림을 만들지 않는다(사용자가 "알림 없음" 선택).
 * - 시각이 있는 일정: "시작 offset분 전"에 알리고, 하루 이상 남았으면 전날 저녁 예고도 함께 준다.
 * - 종일 일정: 시각이 없어 전날 저녁 + 당일 아침으로 알린다.
 * 지난 시각은 제외한다.
 */
object ReminderTime {

    const val DAY_BEFORE_HOUR = 18 // 전날 저녁 6시(예고)
    const val DAY_OF_HOUR = 8      // 종일 일정 당일 아침 8시

    /** 미래의 알림 트리거 시각 목록(오름차순). */
    fun plan(schedule: Schedule, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): List<Long> {
        val offsetMin = schedule.reminderOffsetMin ?: return emptyList() // 알림 없음
        val date = schedule.date
        fun millisAt(days: Long, hour: Int): Long =
            date.minusDays(days).atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()

        val start = schedule.startTime
        val triggers = if (start != null) {
            // 시각이 있는 일정: 시작 offset분 전 + 전날 저녁 예고
            val eventMillis = date.atTime(start).atZone(zone).toInstant().toEpochMilli()
            listOf(millisAt(1, DAY_BEFORE_HOUR), eventMillis - offsetMin * 60_000L)
        } else {
            // 종일 일정: 전날 저녁 + 당일 아침
            listOf(millisAt(1, DAY_BEFORE_HOUR), millisAt(0, DAY_OF_HOUR))
        }

        return triggers.filter { it > nowMillis }.sorted().distinct()
    }

    /** 부팅 재등록 쿼리용 비정규화 값: 가장 이른 미래 알림. */
    fun compute(schedule: Schedule, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long? =
        plan(schedule, nowMillis, zone).firstOrNull()
}
