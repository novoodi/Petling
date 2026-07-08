package com.example.petling.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.petling.PetlingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 재부팅/시간 변경 후 예약 알람 재등록 (AlarmManager 알람은 재부팅 시 사라진다). */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            -> {
                val app = context.applicationContext as PetlingApplication
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        app.container.alarmScheduler.rescheduleAll()
                        if (app.container.settings.isScreenshotWatchEnabled()) {
                            ScreenshotWatcher.schedule(context)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
