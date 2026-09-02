package com.example.petling.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.petling.BuildConfig
import com.example.petling.ui.ActionIntents
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.PetlingCard
import com.google.mlkit.genai.common.FeatureStatus

private const val PRIVACY_POLICY_URL =
    "https://spotless-mahogany-316.notion.site/Petling-397f6d06643c80469ff5eaba97086359"

@Composable
fun SettingsScreen() {
    val container = appContainer()
    val context = LocalContext.current

    // AI(Gemini Nano) 지원 상태 — 미지원이어도 규칙 인식으로 동작함을 안내
    val aiStatus by produceState(initialValue = "확인 중…") {
        value = when (container.priceTagExtractor.availability()) {
            FeatureStatus.AVAILABLE -> "사용 가능 — 가격표를 AI가 함께 읽어요"
            FeatureStatus.DOWNLOADING, FeatureStatus.DOWNLOADABLE -> "모델 준비 중 — 그동안 기본 인식으로 동작해요"
            else -> "이 기기는 미지원 — 기본 인식으로 동작해요"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("설정", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        PetlingCard(modifier = Modifier.fillMaxWidth()) {
            Text("온디바이스 AI", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                aiStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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
}
