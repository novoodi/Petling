package com.example.petling.ui.voice

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.ui.appContainer
import com.example.petling.ui.capture.DraftCard
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.PetlingButtonStyle
import com.example.petling.ui.theme.Brand500
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.TextTertiary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    onBack: () -> Unit,
    onReviewDraft: (ParsedDraftSeed) -> Unit,
) {
    val container = appContainer()
    val context = LocalContext.current
    val vm: VoiceViewModel = viewModel(
        factory = viewModelFactory {
            initializer { VoiceViewModel(container.voiceParser, container.scheduleRepository) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    // 마이크 권한이 영구 거부(다시 묻지 않음)된 경우 → 재요청 대신 앱 설정으로 안내(무한 루프 방지)
    var micPermanentlyDenied by remember { mutableStateOf(false) }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // 온디바이스 우선
        }
    }

    fun startListening() {
        vm.onListening()
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                vm.onError("음성을 인식하지 못했어요. 다시 시도해주세요.")
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                vm.onRecognized(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer?.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            micPermanentlyDenied = false
            startListening()
        } else {
            // 거부 후에도 rationale을 보여줄 수 없으면(=다시 묻지 않음/영구 거부) 설정으로 안내
            val activity = context as? android.app.Activity
            val canAskAgain = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
            } ?: false
            micPermanentlyDenied = !canAskAgain
            vm.onError(
                if (micPermanentlyDenied) "마이크 권한이 꺼져 있어요. 설정에서 켜주세요."
                else "마이크 권한이 필요해요.",
            )
        }
    }

    LaunchedEffect(Unit) {
        if (recognizer == null) {
            vm.onError("이 기기에서는 음성 인식을 사용할 수 없어요.")
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose { recognizer?.destroy() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("말로 등록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (state.stage) {
                VoiceStage.LISTENING -> CenterColumn {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = Brand500, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(Dimens.Space4))
                    Text("듣고 있어요…", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.Space2))
                    Text("\"다음 주 화요일 3시 학원 상담\" 처럼 말해보세요.", color = TextTertiary)
                }
                VoiceStage.PROCESSING -> CenterColumn {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(Dimens.Space4))
                    Text("\"${state.recognizedText}\"", style = MaterialTheme.typography.bodyLarge)
                }
                VoiceStage.EMPTY -> CenterColumn {
                    Text("일정을 찾지 못했어요.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.Space4))
                    PetlingButton("다시 말하기", onClick = { startListening() })
                }
                VoiceStage.ERROR -> CenterColumn {
                    Text(state.errorMessage ?: "오류가 발생했어요.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.Space4))
                    if (micPermanentlyDenied) {
                        PetlingButton("앱 설정 열기", onClick = {
                            context.startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", context.packageName, null),
                                ),
                            )
                        })
                    } else {
                        PetlingButton("다시 시도", onClick = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        })
                    }
                    Spacer(Modifier.height(Dimens.Space3))
                    PetlingButton("돌아가기", onClick = onBack, style = PetlingButtonStyle.Secondary)
                }
                VoiceStage.REVIEW -> Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(Dimens.Space3))
                    Text("\"${state.recognizedText}\"", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                    Spacer(Modifier.height(Dimens.Space3))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                        items(state.drafts) { draft ->
                            DraftCard(
                                draft = draft,
                                onQuickSave = { vm.quickSave(draft) },
                                onReview = { onReviewDraft(draft) },
                            )
                        }
                    }
                    if (state.drafts.isEmpty()) {
                        Spacer(Modifier.height(Dimens.Space6))
                        PetlingButton("완료", onClick = onBack)
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}
