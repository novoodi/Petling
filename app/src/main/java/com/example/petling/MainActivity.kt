package com.example.petling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.example.petling.notifications.NotificationHelper
import com.example.petling.ui.navigation.PetlingNavHost
import com.example.petling.ui.theme.PetlingTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_LIBRARY = "extra_open_library"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PetlingApplication).container

        val deepLinkScheduleId = intent
            ?.getLongExtra(NotificationHelper.EXTRA_SCHEDULE_ID, -1L)
            ?.takeIf { it >= 0 }

        // 공유된 이미지 또는 스크린샷 제안 알림의 uri → 인제스트로 라우팅
        val sharedImageUri: android.net.Uri? = when {
            intent?.action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
                }
            }
            intent?.hasExtra(NotificationHelper.EXTRA_SCREENSHOT_URI) == true ->
                intent.getStringExtra(NotificationHelper.EXTRA_SCREENSHOT_URI)?.let { android.net.Uri.parse(it) }
            else -> null
        }

        val openLibrary = intent?.getBooleanExtra(EXTRA_OPEN_LIBRARY, false) == true

        val sharedText: String? =
            if (intent?.action == android.content.Intent.ACTION_SEND &&
                intent.type == "text/plain"
            ) {
                intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            } else {
                null
            }

        setContent {
            PetlingTheme {
                // 온보딩 완료 여부를 로드할 때까지 null, 로드되면 시작 목적지 결정
                val onboardingDone by produceState<Boolean?>(initialValue = null) {
                    value = container.settings.settings.first().onboardingCompleted
                }
                onboardingDone?.let { done ->
                    PetlingNavHost(
                        startOnboarding = !done,
                        initialScheduleId = if (done) deepLinkScheduleId else null,
                        sharedImageUri = if (done) sharedImageUri else null,
                        sharedText = if (done) sharedText else null,
                        openLibrary = done && openLibrary,
                    )
                }
            }
        }
    }
}
