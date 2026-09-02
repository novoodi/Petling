package com.example.petling.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.petling.data.price.NanoState

/**
 * 온디바이스 AI 안내의 단일 출처 — 첫 실행 대화상자와 설정 카드가 같은 문구를 쓴다.
 * 지원 기종은 ML Kit GenAI Prompt API 문서 기준(2026-09 확인). 갤럭시 S25·Z Fold6 이하는 미지원.
 */
object OnDeviceAiTexts {
    const val SUPPORTED_DEVICES =
        "지원 기종 — 갤럭시: Z Fold7, Z Fold8·Z Flip8, S26 시리즈 · 픽셀: 9, 10, 11 시리즈 " +
            "(갤럭시 S25·Z Fold6 이하는 미지원)"

    const val FALLBACK =
        "AI가 없어도 가격표는 읽어요. 사진의 글자를 인식해 상품명과 가격을 규칙으로 뽑고, " +
            "헷갈리면 후보를 보여드려요. AI는 상품명을 더 정확히 고르는 데만 쓰이고, " +
            "가격 숫자는 AI 유무와 상관없이 항상 글자 인식 결과를 써요."

    const val PRIVACY = "AI 처리도 사진도 휴대폰 안에서만 이루어져요. 서버로 전송되지 않아요."

    const val DOWNLOAD_NOTE =
        "모델은 수 GB라 시스템이 Wi-Fi·충전 중에 받아요. 버튼을 눌러도 조건이 안 맞으면 기다렸다가 이어받아요."

    fun statusLine(state: NanoState): String = when (state) {
        NanoState.Checking -> "AI 지원 여부 확인 중…"
        NanoState.Available -> "사용 가능 — 가격표를 AI가 함께 읽어요"
        NanoState.Downloadable -> "지원 기기예요 — 모델을 받으면 AI가 함께 읽어요"
        is NanoState.Downloading -> {
            val pct = state.percent
            if (pct != null) "모델 받는 중 $pct% (${gb(state.downloadedBytes)} / ${gb(state.totalBytes!!)}) — 그동안 기본 인식으로 동작해요"
            else "모델 받는 중 — Wi-Fi·충전 중에 이어서 받아요. 그동안 기본 인식으로 동작해요"
        }
        is NanoState.Failed -> "다운로드에 실패했어요 — 다시 시도해주세요"
        NanoState.Unavailable -> "이 기기는 AI 미지원 — 기본 인식으로 동작해요"
    }

    private fun gb(bytes: Long): String = "%.1fGB".format(bytes / 1_073_741_824.0)
}

/** 0~100, 총량을 모르면 null. */
val NanoState.Downloading.percent: Int?
    get() = totalBytes?.takeIf { it > 0 }?.let { (downloadedBytes * 100 / it).toInt().coerceIn(0, 100) }

private val NanoState.canRequestDownload: Boolean
    get() = this is NanoState.Downloadable || this is NanoState.Failed

/** 설정 화면 카드: 상태 + 진행률 + 다운로드 버튼 + 지원 기종 + AI 없을 때 동작. */
@Composable
fun OnDeviceAiCard(state: NanoState, onDownload: () -> Unit, modifier: Modifier = Modifier) {
    PetlingCard(modifier = modifier) {
        Text("온디바이스 AI", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            OnDeviceAiTexts.statusLine(state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state is NanoState.Downloading) {
            Spacer(Modifier.height(8.dp))
            val pct = state.percent
            if (pct != null) {
                LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        if (state.canRequestDownload) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                Text(if (state is NanoState.Failed) "다시 받기" else "AI 모델 지금 받기")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                OnDeviceAiTexts.DOWNLOAD_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            OnDeviceAiTexts.SUPPORTED_DEVICES,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            OnDeviceAiTexts.FALLBACK,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 첫 실행 안내: 이 휴대폰이 AI 지원인지, 아니면 어떻게 동작하는지 먼저 알려준다.
 * 지원 기기면 바로 받기 버튼을 준다. 설정 화면에서 같은 내용을 다시 볼 수 있다.
 */
@Composable
fun OnDeviceAiIntroDialog(state: NanoState, onDownload: () -> Unit, onDismiss: () -> Unit) {
    val supported = state.isSupported
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    state is NanoState.Checking -> "가격표는 이렇게 읽어요"
                    supported -> "이 휴대폰은 AI 지원 기기예요"
                    else -> "이 휴대폰은 기본 인식으로 동작해요"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        state is NanoState.Available ->
                            "온디바이스 AI(Gemini Nano)가 준비돼 있어요. 가격표 사진을 AI가 함께 읽어 상품명을 더 정확히 골라요."
                        supported ->
                            "온디바이스 AI(Gemini Nano)를 받으면 상품명을 더 정확히 골라요. 받기 전에도 기본 인식으로 바로 쓸 수 있어요."
                        state is NanoState.Checking ->
                            "AI 지원 여부를 아직 확인하는 중이에요. 확인되면 설정 화면에 표시돼요."
                        else ->
                            "이 기종은 온디바이스 AI(Gemini Nano)를 지원하지 않아요. 그래도 가격표 기록·비교는 전부 똑같이 돼요."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(OnDeviceAiTexts.FALLBACK, style = MaterialTheme.typography.bodySmall)
                Text(
                    OnDeviceAiTexts.SUPPORTED_DEVICES,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    OnDeviceAiTexts.PRIVACY,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "설정 탭에서 언제든 다시 볼 수 있어요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            if (state.canRequestDownload) {
                TextButton(onClick = { onDownload(); onDismiss() }) { Text("AI 모델 지금 받기") }
            } else {
                TextButton(onClick = onDismiss) { Text("확인") }
            }
        },
        dismissButton = {
            if (state.canRequestDownload) {
                TextButton(onClick = onDismiss) { Text("나중에") }
            }
        },
    )
}
