package com.example.petling.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.petling.PetlingApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 알림의 "완료했어요" 액션 → 일정 완료 처리 + XP 지급. */
class CompleteActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId < 0) return
        val app = context.applicationContext as PetlingApplication
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = app.container
                val schedule = container.scheduleRepository.getById(scheduleId) ?: return@launch
                container.characterRepository.completeSchedule(schedule)
                container.alarmScheduler.cancel(scheduleId)
                NotificationManagerCompat.from(context).cancel(scheduleId.toInt())
            } finally {
                pending.finish()
            }
        }
    }
}
