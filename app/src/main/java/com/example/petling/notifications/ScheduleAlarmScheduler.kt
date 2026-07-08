package com.example.petling.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.petling.data.local.PetlingDatabase
import com.example.petling.data.local.entity.toDomain
import com.example.petling.data.repository.ReminderTime
import com.example.petling.domain.AppClock
import com.example.petling.domain.model.Schedule

/**
 * AlarmManager 기반 정확한 알람 스케줄러.
 * exact alarm 권한이 없으면 setWindow 폴백을 사용한다.
 */
class ScheduleAlarmScheduler(
    private val context: Context,
    private val db: PetlingDatabase,
    private val clock: AppClock,
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(schedule: Schedule) {
        cancel(schedule.id) // 재등록 시 기존 알람 정리
        val times = ReminderTime.plan(schedule, clock.nowMillis())
        times.forEachIndexed { idx, triggerAt ->
            val pending = pendingIntent(schedule.id, idx)
            if (canExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 10 * 60 * 1000L, pending)
            }
        }
    }

    override fun cancel(scheduleId: Long) {
        for (idx in 0 until MAX_REMINDERS) alarmManager.cancel(pendingIntent(scheduleId, idx))
    }

    override suspend fun rescheduleAll() {
        val pending = db.scheduleDao().getPendingWithReminderAfter(clock.nowMillis())
        pending.forEach { schedule(it.toDomain()) }
    }

    private fun canExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    private fun pendingIntent(scheduleId: Long, index: Int): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_SCHEDULE_ID, scheduleId)
        }
        return PendingIntent.getBroadcast(
            context,
            (scheduleId * 10 + index).toInt(), // 알림별 고유 request code(전날/당일)
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val MAX_REMINDERS = 2
    }
}
