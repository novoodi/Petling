package com.example.petling.ui.paste

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.ui.appContainer
import com.example.petling.ui.capture.DraftCard
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.PetlingButtonStyle
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasteScreen(
    initialText: String?,
    onBack: () -> Unit,
    onReviewDraft: (ParsedDraftSeed) -> Unit,
) {
    val container = appContainer()
    val vm: PasteViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PasteViewModel(container.textParser, container.scheduleRepository) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    // 공유 텍스트 우선, 없으면 클립보드 텍스트로 자동 프리필(1회)
    LaunchedEffect(Unit) {
        val prefill = initialText?.takeIf { it.isNotBlank() } ?: clipboard.getText()?.text
        if (!prefill.isNullOrBlank()) vm.prefill(prefill)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("텍스트로 등록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPaddingFocused),
        ) {
            Spacer(Modifier.height(Dimens.Space3))
            OutlinedTextField(
                value = state.text,
                onValueChange = vm::updateText,
                label = { Text("카톡 공지나 문자를 붙여넣어 보세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
            )
            Spacer(Modifier.height(Dimens.Space3))
            PetlingButton(
                "일정 찾기",
                onClick = { vm.parse() },
                enabled = state.text.isNotBlank(),
            )
            Spacer(Modifier.height(Dimens.Space4))

            if (state.stage == PasteStage.PARSED) {
                if (state.drafts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space8),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.savedCount > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("모두 등록했어요! (${state.savedCount}건)", color = TextTertiary)
                                Spacer(Modifier.height(Dimens.Space4))
                                PetlingButton("완료", onClick = onBack, fillWidth = false)
                            }
                        } else {
                            Text("일정을 찾지 못했어요. 문구를 다듬어 다시 시도해보세요.", color = TextTertiary)
                        }
                    }
                } else {
                    Text(
                        "${state.drafts.size}개의 일정을 찾았어요.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(Dimens.Space2))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                        items(state.drafts) { draft ->
                            DraftCard(
                                draft = draft,
                                onQuickSave = { vm.quickSave(draft) },
                                onReview = { onReviewDraft(draft) },
                            )
                        }
                    }
                    TextButton(onClick = onBack) { Text("완료") }
                }
            }
        }
    }
}
