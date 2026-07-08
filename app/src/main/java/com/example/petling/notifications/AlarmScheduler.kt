package com.example.petling.notifications

import com.example.petling.domain.model.Schedule

/**
 * 일정 알림 스케줄링 추상화. Repository는 이 인터페이스에만 의존하고
 * 실제 AlarmManager 구현은 notifications 패키지에서 제공한다(M6).
 */
interface AlarmScheduler {
    fun schedule(schedule: Schedule)
    fun cancel(scheduleId: Long)
    suspend fun rescheduleAll()
}

/** 알림을 전혀 걸지 않는 기본 구현(테스트/초기 마일스톤용). */
object NoOpAlarmScheduler : AlarmScheduler {
    override fun schedule(schedule: Schedule) {}
    override fun cancel(scheduleId: Long) {}
    override suspend fun rescheduleAll() {}
}
