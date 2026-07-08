package com.example.petling.notifications

import android.Manifest
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.petling.MainActivity
import com.example.petling.PetlingApplication
import com.example.petling.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * MediaStore 이미지 변화 시 깨어나 새 스크린샷을 찾고 "정리할까요?" 제안 알림을 띄운다.
 * 콘텐츠 트리거 잡은 1회성이라 매번 재예약한다.
 */
class ScreenshotDetectJobService : JobService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        val app = applicationContext as PetlingApplication
        android.util.Log.d("PetlingShot", "job fired")
        scope.launch {
            try {
                if (app.container.settings.isScreenshotWatchEnabled() &&
                    app.container.settings.isNotificationsEnabled()
                ) {
                    detectAndNotify(app)
                } else {
                    android.util.Log.d("PetlingShot", "watch/notify disabled, skip")
                }
            } finally {
                // 콘텐츠 트리거 잡은 1회성 → 다시 감시하도록 재예약
                ScreenshotWatcher.schedule(applicationContext)
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true // 재예약 요청

    private suspend fun detectAndNotify(app: PetlingApplication) {
        val settings = app.container.settings
        val lastSeen = settings.getLastSeenScreenshotId()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
        )
        val newest = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media._ID} > ?",
            arrayOf(lastSeen.toString()),
            "${MediaStore.Images.Media._ID} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
            var maxId = lastSeen
            var found: Pair<Long, Uri>? = null
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                if (id > maxId) maxId = id
                if (found == null &&
                    ScreenshotDetector.isScreenshot(cursor.getString(pathCol), cursor.getString(nameCol))
                ) {
                    found = id to ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }
            }
            // 다음 감지 기준점 갱신(전체 최대 id로 마킹)
            if (maxId > lastSeen) settings.setLastSeenScreenshotId(maxId)
            found
        }

        android.util.Log.d("PetlingShot", "lastSeen=$lastSeen, foundScreenshot=${newest?.first}")
        if (newest == null) return
        // baseline이 잡혀 있으면(활성화 시 세팅) 이후 새 스크린샷만 알림
        postSuggestion(app, newest.second)
    }

    private fun postSuggestion(app: PetlingApplication, uri: Uri) {
        NotificationHelper.ensureChannel(app)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openIntent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationHelper.EXTRA_SCREENSHOT_URI, uri.toString())
        }
        val openPending = PendingIntent.getActivity(
            app, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val organizeIntent = Intent(app, OrganizeActionReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_SCREENSHOT_URI, uri.toString())
        }
        val organizePending = PendingIntent.getBroadcast(
            app, 1, organizeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val thumb = runCatching {
            app.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()

        val builder = NotificationCompat.Builder(app, NotificationHelper.CHANNEL_SUGGESTIONS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("새 스크린샷이 있어요")
            .setContentText("정리할까요?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, "정리하기", organizePending)
        if (thumb != null) builder.setLargeIcon(thumb)

        NotificationManagerCompat.from(app).notify(NotificationHelper.SUGGESTION_NOTIFICATION_ID, builder.build())
    }
}
