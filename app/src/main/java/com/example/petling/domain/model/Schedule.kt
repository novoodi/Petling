package com.example.petling.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Schedule(
    val id: Long = 0L,
    val title: String,
    val date: LocalDate,
    /** 하루 중 시작 시각(분). null이면 종일 일정. */
    val startMinuteOfDay: Int? = null,
    val location: String? = null,
    val memo: String? = null,
    val category: ScheduleCategory? = null,
    val isImportant: Boolean = false,
    val status: ScheduleStatus = ScheduleStatus.PENDING,
    val source: ScheduleSource = ScheduleSource.MANUAL,
    /** 알림 오프셋(분). null=알림 없음, 0=정시, 10/30/60=N분 전. */
    val reminderOffsetMin: Int? = null,
    /** 알림 트리거 시각(epoch millis). 부팅 후 재등록 쿼리용 비정규화 값. */
    val reminderAtMillis: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val isAllDay: Boolean get() = startMinuteOfDay == null

    val startTime: LocalTime?
        get() = startMinuteOfDay?.let { LocalTime.of(it / 60, it % 60) }
}
