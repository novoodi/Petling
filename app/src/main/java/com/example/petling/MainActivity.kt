package com.example.petling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.petling.ui.navigation.PetlingNavHost
import com.example.petling.ui.theme.PetlingTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : ComponentActivity() {

    /**
     * Play 인앱 업데이트(유연 방식). 앱을 열 때 Play에 새 버전이 있는지 묻고, 있으면 구글 표준 안내 창을 띄운다.
     * 사용자가 수락하면 백그라운드로 받고, 다 받으면 하단에 "다시 시작" 스낵바를 보여준다.
     * Play 스토어로 설치한 빌드에서만 동작한다(디버그·APK 직접 설치는 조용히 무시).
     */
    private val updateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val updateDownloaded = mutableStateOf(false)
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) updateDownloaded.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateManager.registerListener(installListener)
        checkForUpdate()
        setContent {
            PetlingTheme {
                val snackbar = remember { SnackbarHostState() }
                val downloaded by updateDownloaded
                LaunchedEffect(downloaded) {
                    if (downloaded) {
                        val result = snackbar.showSnackbar(
                            message = "새 버전을 받았어요. 다시 시작하면 적용돼요.",
                            actionLabel = "다시 시작",
                            duration = SnackbarDuration.Indefinite,
                        )
                        if (result == SnackbarResult.ActionPerformed) updateManager.completeUpdate()
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    PetlingNavHost()
                    SnackbarHost(
                        hostState = snackbar,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 받아둔 업데이트가 있는데 아직 설치 안 했으면(앱을 껐다 켠 경우) 다시 안내
        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) updateDownloaded.value = true
        }
    }

    override fun onDestroy() {
        updateManager.unregisterListener(installListener)
        super.onDestroy()
    }

    private fun checkForUpdate() {
        runCatching {
            updateManager.appUpdateInfo.addOnSuccessListener { info ->
                val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                if (available && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    runCatching {
                        updateManager.startUpdateFlowForResult(
                            info,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    }
                }
            }
        }
    }
}
