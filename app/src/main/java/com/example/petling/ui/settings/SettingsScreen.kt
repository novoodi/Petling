package com.example.petling.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.PetlingButtonStyle
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.theme.Dimens
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onOpenCategories: () -> Unit = {}) {
    val container = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(
        initialValue = com.example.petling.data.datastore.AppSettings(),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingFocused)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space4),
    ) {
        Spacer(Modifier.height(Dimens.Space4))
        Text("설정", style = MaterialTheme.typography.headlineSmall)

        AiClassifierCard()

        // 내 분류함(맞춤 카테고리)
        PetlingCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCategories)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("내 분류함", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "자주 모으는 분류를 고르고 직접 추가해요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        PetlingCard(modifier = Modifier.fillMaxWidth()) {
            SettingToggle(
                title = "알림 사용",
                checked = settings.notificationsEnabled,
                onCheckedChange = { scope.launch { container.settings.setNotificationsEnabled(it) } },
            )
        }

        // 스크린샷 자동 감지 (옵트인)
        ScreenshotWatchCard(
            enabled = settings.screenshotWatchEnabled,
            onEnable = {
                scope.launch {
                    // 활성화 시점의 최신 이미지를 기준점으로 → 이후 찍는 스크린샷만 감지
                    container.settings.setLastSeenScreenshotId(
                        com.example.petling.notifications.ScreenshotWatcher.currentMaxImageId(context),
                    )
                    container.settings.setScreenshotWatchEnabled(true)
                    com.example.petling.notifications.ScreenshotWatcher.schedule(context)
                }
            },
            onDisable = {
                scope.launch { container.settings.setScreenshotWatchEnabled(false) }
                com.example.petling.notifications.ScreenshotWatcher.cancel(context)
            },
        )

        // 정확한 알람 권한 상태 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canExact = alarmManager.canScheduleExactAlarms()
            PetlingCard(modifier = Modifier.fillMaxWidth()) {
                Text("정확한 시간 알림", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Dimens.Space1))
                Text(
                    if (canExact) "제때 알림을 보낼 수 있어요." else "정확한 알림 권한이 꺼져 있어 알림이 지연될 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!canExact) {
                    Spacer(Modifier.height(Dimens.Space3))
                    PetlingButton("권한 설정 열기", onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
                        )
                    }, style = PetlingButtonStyle.Secondary)
                }
            }
        }

        // 개발용 디버그 (디버그 빌드에서만 노출 — 릴리스/베타 사용자에게는 숨김)
        if (com.example.petling.BuildConfig.DEBUG) {
            PetlingCard(modifier = Modifier.fillMaxWidth()) {
                Text("개발용 (캡처 카운트 주입)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(Dimens.Space2))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                    listOf(0, 10, 30, 60).forEach { count ->
                        PetlingButton(
                            text = "$count",
                            onClick = { scope.launch { container.characterRepository.debugSetCaptureCount(count) } },
                            style = PetlingButtonStyle.Secondary,
                            fillWidth = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(Dimens.Space3))
                PetlingButton(
                    "데이터 초기화",
                    onClick = {
                        scope.launch {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                container.database.clearAllTables()
                            }
                            container.settings.clear()
                            // 온보딩 플래그가 초기화됐으므로 액티비티를 재생성해 온보딩부터 다시 시작
                            (context as? android.app.Activity)?.recreate()
                        }
                    },
                    style = PetlingButtonStyle.Secondary,
                )
            }
        }

        Text(
            "이 앱은 캡처·음성 데이터를 서버로 보내지 않아요. 모든 처리는 기기 안에서 이뤄집니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Space6))
    }
}

@Composable
private fun AiClassifierCard() {
    val container = appContainer()
    // FeatureStatus: 0=미지원, 1=다운로드가능, 2=다운로드중, 3=사용가능
    val status by androidx.compose.runtime.produceState(initialValue = -1) {
        value = container.captureNano.availability()
    }
    val (line, isOn) = when (status) {
        3 -> "AI 분류를 사용 중이에요 (Gemini Nano)." to true
        2 -> "AI 모델을 준비하는 중이에요. 곧 사용돼요." to false
        1 -> "AI 모델 준비가 필요해요. 캡처를 넣으면 자동으로 받아요." to false
        0 -> "이 기기는 온디바이스 AI 분류를 지원하지 않아, 기본 규칙으로 정리해요." to false
        else -> "AI 분류 상태 확인 중…" to false
    }
    PetlingCard(modifier = Modifier.fillMaxWidth()) {
        Text("AI 자동 분류", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Dimens.Space1))
        Text(
            line,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun mediaImagesPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasFullImageAccess(context: Context): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, mediaImagesPermission()) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

@Composable
private fun ScreenshotWatchCard(
    enabled: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    val context = LocalContext.current
    var granted by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(hasFullImageAccess(context))
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) {
        granted = hasFullImageAccess(context)
        if (granted) onEnable()
    }

    PetlingCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("스크린샷 자동 감지", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "스크린샷을 찍으면 \"정리할까요?\"를 알려줘요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { on ->
                    if (!on) {
                        onDisable()
                    } else if (hasFullImageAccess(context)) {
                        granted = true
                        onEnable()
                    } else {
                        permissionLauncher.launch(mediaImagesPermission())
                    }
                },
            )
        }
        // 켰는데 전체 접근이 아니면 안내(부분 "선택한 사진만"으로는 새 스크린샷 감지 불가)
        if (enabled && !granted) {
            Spacer(Modifier.height(Dimens.Space2))
            Text(
                "새 스크린샷을 감지하려면 사진 '전체 접근' 권한이 필요해요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(Dimens.Space2))
            PetlingButton("앱 권한 설정 열기", onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", context.packageName, null),
                    ),
                )
            }, style = PetlingButtonStyle.Secondary)
        }
    }
}
