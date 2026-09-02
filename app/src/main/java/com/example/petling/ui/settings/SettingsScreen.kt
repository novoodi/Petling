package com.example.petling.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petling.BuildConfig
import com.example.petling.data.backup.BackupFormatException
import com.example.petling.ui.ActionIntents
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.OnDeviceAiCard
import com.example.petling.ui.components.PetlingCard
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL =
    "https://spotless-mahogany-316.notion.site/Petling-397f6d06643c80469ff5eaba97086359"

@Composable
fun SettingsScreen() {
    val container = appContainer()
    val context = LocalContext.current

    // AI(Gemini Nano) 상태 — 화면에 들어올 때마다 AICore에 다시 묻는다(시스템 업데이트로 바뀔 수 있음)
    val nano = container.priceTagExtractor
    val nanoState by nano.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { nano.refresh() }

    // 백업: 시스템 파일 선택기(CreateDocument/OpenDocument) → 저장소 권한 불필요
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }
    val backup = container.backupRepository

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val message = runCatching { backup.exportTo(uri) }
                .fold(
                    onSuccess = { (products, entries) -> "내보냈어요 — 상품 ${products}개, 기록 ${entries}건" },
                    onFailure = { "내보내기에 실패했어요: ${it.message ?: "알 수 없는 오류"}" },
                )
            busy = false
            snackbar.showSnackbar(message)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val message = runCatching { backup.importFrom(uri) }
                .fold(
                    onSuccess = {
                        "가져왔어요 — 새 상품 ${it.productsAdded}개, 새 기록 ${it.entriesAdded}건" +
                            if (it.entriesSkipped > 0) " (중복 ${it.entriesSkipped}건 건너뜀)" else ""
                    },
                    onFailure = {
                        when (it) {
                            is BackupFormatException -> it.message ?: "백업 파일 형식이 아니에요"
                            else -> "가져오기에 실패했어요: ${it.message ?: "알 수 없는 오류"}"
                        }
                    },
                )
            busy = false
            snackbar.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("설정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        PetlingCard(modifier = Modifier.fillMaxWidth()) {
            Text("백업", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "기록은 휴대폰 안에만 있어서 폰을 바꾸면 사라져요. 파일로 내보내 두고 새 폰에서 가져오세요. (사진은 제외)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch(backup.suggestedFileName()) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text("내보내기") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text("가져오기") }
            }
        }

        OnDeviceAiCard(
            state = nanoState,
            onDownload = { nano.download() },
            modifier = Modifier.fillMaxWidth(),
        )

        PetlingCard(modifier = Modifier.fillMaxWidth()) {
            Text("개인정보", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "사진과 가격 기록은 모두 휴대폰 안에만 저장돼요. 서버로 전송되지 않아요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "개인정보 처리방침 보기 ›",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { ActionIntents.openUrl(context, PRIVACY_POLICY_URL) },
            )
        }

        PetlingCard(modifier = Modifier.fillMaxWidth()) {
            Text("앱 정보", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "버전 ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    SnackbarHost(
        hostState = snackbar,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
    )
    }
}
