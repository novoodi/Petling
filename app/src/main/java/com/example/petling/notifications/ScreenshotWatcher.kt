package com.example.petling.notifications

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.provider.MediaStore

/**
 * 스크린샷 감지 잡 등록/해제. 콘텐츠 URI 트리거라 상시 서비스 없이
 * MediaStore(이미지)에 변화가 생길 때만 잠깐 깨어난다(배터리 절약).
 */
object ScreenshotWatcher {

    const val JOB_ID = 7001

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        val component = ComponentName(context, ScreenshotDetectJobService::class.java)
        val trigger = JobInfo.TriggerContentUri(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
        )
        // 주의: 콘텐츠 트리거 잡은 setPersisted 불가(상호배타). 재부팅 재등록은
        // BootCompletedReceiver / PetlingApplication.onCreate 에서 처리한다.
        val job = JobInfo.Builder(JOB_ID, component)
            .addTriggerContentUri(trigger)
            .setTriggerContentUpdateDelay(1_000L)   // 연속 변화 배치(1초)
            .setTriggerContentMaxDelay(10_000L)      // 최대 10초 내 실행
            .build()
        scheduler.schedule(job)
    }

    fun cancel(context: Context) {
        context.getSystemService(JobScheduler::class.java)?.cancel(JOB_ID)
    }

    /** 현재 이미지 중 가장 큰 _id — 활성화 시 기준점으로 잡아 이후 스크린샷만 감지한다. */
    fun currentMaxImageId(context: Context): Long {
        return runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null, null,
                "${MediaStore.Images.Media._ID} DESC",
            )?.use { c ->
                if (c.moveToFirst()) c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)) else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    fun isScheduled(context: Context): Boolean =
        context.getSystemService(JobScheduler::class.java)
            ?.allPendingJobs?.any { it.id == JOB_ID } ?: false
}
