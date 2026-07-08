package com.example.petling.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** 알림 채널 생성 및 상수. */
object NotificationHelper {
    const val CHANNEL_REMINDERS = "schedule_reminders"
    const val CHANNEL_SUGGESTIONS = "screenshot_suggestions"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_SCREENSHOT_URI = "extra_screenshot_uri"
    const val SUGGESTION_NOTIFICATION_ID = 90001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_REMINDERS) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_REMINDERS, "일정 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "다가오는 일정을 캐릭터가 알려줘요."
                    },
                )
            }
            if (manager.getNotificationChannel(CHANNEL_SUGGESTIONS) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_SUGGESTIONS, "스크린샷 정리", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "새 스크린샷을 정리할지 조용히 제안해요."
                    },
                )
            }
        }
    }
}
