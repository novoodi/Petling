package com.example.petling

import android.app.Application
import com.example.petling.di.AppContainer
import com.example.petling.notifications.NotificationHelper
import com.example.petling.notifications.ScreenshotWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PetlingApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        // 앱 시작 시 예약 알람·스크린샷 감시 재동기화(프로세스 재생성 대비)
        CoroutineScope(Dispatchers.IO).launch {
            container.categoryRepository.seedIfEmpty()
            container.alarmScheduler.rescheduleAll()
            if (container.settings.isScreenshotWatchEnabled()) {
                ScreenshotWatcher.schedule(this@PetlingApplication)
            }
            // 지원 기기면 온디바이스 모델을 미리 받아 첫 캡처부터 AI 분류가 되게 함(비차단)
            container.captureNano.prewarm()
        }
    }
}
