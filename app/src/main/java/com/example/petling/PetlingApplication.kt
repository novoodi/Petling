package com.example.petling

import android.app.Application
import com.example.petling.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PetlingApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 지원 기기면 온디바이스 모델을 미리 받아 첫 가격표부터 AI 인식이 되게 함(비차단)
        CoroutineScope(Dispatchers.IO).launch {
            container.priceTagExtractor.prewarm()
        }
    }
}
