package com.example.petling.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.petling.MainActivity
import com.example.petling.PetlingApplication
import com.example.petling.R
import com.example.petling.domain.model.ScheduleStatus
import com.example.petling.domain.personality.PhraseArgs
import com.example.petling.domain.personality.PhraseContext
import com.example.petling.domain.personality.PhraseSelector
import com.example.petling.ui.components.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 예약된 알람 수신 → 캐릭터 성격 문구로 알림 표시. */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId < 0) return
        val app = context.applicationContext as PetlingApplication
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = app.container
                // "알림 사용"이 꺼져 있으면 알림을 띄우지 않는다.
                if (!container.settings.isNotificationsEnabled()) return@launch
                val schedule = container.scheduleRepository.getById(scheduleId)
                    ?: return@launch
                if (schedule.status != ScheduleStatus.PENDING) return@launch
                val character = container.characterRepository.get() ?: return@launch

                val selector = PhraseSelector()
                val phrase = selector.pick(
                    character.personality,
                    PhraseContext.REMINDER,
                    PhraseArgs(
                        name = character.name,
                        title = schedule.title,
                        time = formatTime(schedule.startMinuteOfDay),
                        location = schedule.location,
                    ),
                )
                val days = schedule.date.toEpochDay() - container.clock.today().toEpochDay()
                val dayLabel = when {
                    days <= 0L -> "오늘"
                    days == 1L -> "내일"
                    else -> "${days}일 뒤"
                }
                val body = listOf(dayLabel, schedule.timeLabelForBody()).filter { it.isNotBlank() }.joinToString(" · ")
                showNotification(context, scheduleId, phrase, schedule.title, body)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        scheduleId: Long,
        title: String,
        scheduleTitle: String,
        body: String,
    ) {
        NotificationHelper.ensureChannel(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationHelper.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val openPending = PendingIntent.getActivity(
            context,
            scheduleId.toInt(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val completeIntent = Intent(context, CompleteActionReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_SCHEDULE_ID, scheduleId)
        }
        val completePending = PendingIntent.getBroadcast(
            context,
            (scheduleId + 1_000_000).toInt(),
            completeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("$scheduleTitle · $body")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, "완료했어요", completePending)
            .build()

        NotificationManagerCompat.from(context).notify(scheduleId.toInt(), notification)
    }
}

private fun com.example.petling.domain.model.Schedule.timeLabelForBody(): String =
    formatTime(startMinuteOfDay) + (location?.let { " · $it" } ?: "")
