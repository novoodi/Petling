package com.example.petling.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.petling.MainActivity
import com.example.petling.PetlingApplication
import com.example.petling.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 제안 알림의 [정리하기] → 백그라운드에서 분류·보관하고 알림을 결과로 갱신. */
class OrganizeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uriStr = intent.getStringExtra(NotificationHelper.EXTRA_SCREENSHOT_URI) ?: return
        val uri = Uri.parse(uriStr)
        val app = context.applicationContext as PetlingApplication
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = app.container.captureRepository.ingest(uri)
                val typeName = result?.item?.categoryKey?.let { key ->
                    app.container.categoryRepository.mapByKey()[key]?.label
                }
                updateNotification(app, typeName)
            } finally {
                pending.finish()
            }
        }
    }

    private fun updateNotification(app: PetlingApplication, typeName: String?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openLibrary = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_LIBRARY, true)
        }
        val pending = PendingIntent.getActivity(
            app, 2, openLibrary,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (typeName != null) "${typeName}(으)로 정리했어요 · 보관함에서 보기" else "정리했어요 · 보관함에서 보기"
        val builder = NotificationCompat.Builder(app, NotificationHelper.CHANNEL_SUGGESTIONS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("정리 완료")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
        NotificationManagerCompat.from(app).notify(NotificationHelper.SUGGESTION_NOTIFICATION_ID, builder.build())
    }
}
