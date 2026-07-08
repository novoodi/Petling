package com.example.petling.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.domain.model.BuiltInCatalog
import com.example.petling.domain.model.Category
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.SurfaceSubtle
import com.example.petling.ui.theme.TextTertiary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureDetailScreen(captureId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val vm: CaptureDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CaptureDetailViewModel(
                    captureId,
                    container.captureRepository,
                    container.categoryRepository,
                    container.captureParser,
                )
            }
        },
    )
    val item by vm.item.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val summarizing by vm.summarizing.collectAsStateWithLifecycle()
    val scheduleRegistered by vm.scheduleRegistered.collectAsStateWithLifecycle()
    val canRegisterSchedule by vm.canRegisterSchedule.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("캡처") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "삭제")
                    }
                },
            )
        },
    ) { padding ->
        val c = item ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPaddingFocused)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(Dimens.Space3))
            CaptureThumbnail(
                c.imagePath,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(Dimens.RadiusLg)),
            )
            Spacer(Modifier.height(Dimens.Space3))
            Text(c.title, style = MaterialTheme.typography.titleLarge)
            val date = Instant.ofEpochMilli(c.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            Text(date.format(formatter), style = MaterialTheme.typography.bodySmall, color = TextTertiary)

            // 한줄 정리(온디바이스 AI 요약)
            if (c.summary != null || summarizing) {
                Spacer(Modifier.height(Dimens.Space3))
                PetlingCard(modifier = Modifier.fillMaxWidth()) {
                    Text("한줄 정리", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(Dimens.Space1))
                    Text(
                        c.summary ?: "내용을 요약하는 중이에요…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (c.summary != null) MaterialTheme.colorScheme.onSurface else TextTertiary,
                    )
                }
            }

            Spacer(Modifier.height(Dimens.Space4))
            Text("분류", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(Dimens.Space2))
            CategoryChips(categories = categories, selectedKey = c.categoryKey, onSelect = vm::changeCategory)

            if (canRegisterSchedule || c.categoryKey == BuiltInCatalog.SCHEDULE) {
                Spacer(Modifier.height(Dimens.Space4))
                if (scheduleRegistered || c.sourceScheduleId != null) {
                    Text("캘린더에 등록됨 · 하루 전·당일 알림 ✓", color = MaterialTheme.colorScheme.tertiary)
                } else if (canRegisterSchedule) {
                    PetlingButton("일정으로 등록", onClick = { vm.registerAsSchedule() })
                }
            }

            // 비서 행동: 링크 열기 / 지도 열기
            val ctx = androidx.compose.ui.platform.LocalContext.current
            c.linkUrl?.let { url ->
                Spacer(Modifier.height(Dimens.Space3))
                PetlingButton("🔗 링크 열기", onClick = { com.example.petling.ui.ActionIntents.openUrl(ctx, url) })
            }
            val baseType = categories.firstOrNull { it.key == c.categoryKey }?.baseType
            if (baseType == com.example.petling.domain.model.CaptureType.PLACE) {
                Spacer(Modifier.height(Dimens.Space3))
                PetlingButton(
                    "📍 지도에서 보기",
                    onClick = { com.example.petling.ui.ActionIntents.openMap(ctx, c.title) },
                    style = com.example.petling.ui.components.PetlingButtonStyle.Secondary,
                )
            }

            Spacer(Modifier.height(Dimens.Space4))
            Text("메모", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(Dimens.Space2))
            var note by remember(c.id) { mutableStateOf(c.note.orEmpty()) }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it; vm.updateNote(it) },
                placeholder = { Text("메모 추가") },
                modifier = Modifier.fillMaxWidth(),
            )

            // 인식된 원문은 기본으로 숨긴다(화면 정돈). 복사가 필요할 때만 펼쳐 본다.
            if (c.ocrText.isNotBlank()) {
                Spacer(Modifier.height(Dimens.Space4))
                var showOcr by remember(c.id) { mutableStateOf(false) }
                Text(
                    if (showOcr) "인식된 원문 접기 ▲" else "인식된 원문 보기 ▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.RadiusSm))
                        .clickable { showOcr = !showOcr }
                        .padding(vertical = Dimens.Space1),
                )
                if (showOcr) {
                    Spacer(Modifier.height(Dimens.Space2))
                    PetlingCard(modifier = Modifier.fillMaxWidth()) {
                        SelectionContainer {
                            Text(c.ocrText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Dimens.Space8))
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("이 캡처를 삭제할까요?") },
            text = { Text("이미지와 인식된 내용이 함께 삭제돼요.") },
            confirmButton = {
                TextButton(onClick = { showDelete = false; vm.delete(onBack) }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("취소") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(categories: List<Category>, selectedKey: String, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space2),
    ) {
        categories.forEach { category ->
            val (fg, bg) = categoryColors(category.baseType)
            val isSel = category.key == selectedKey
            Text(
                text = category.display,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSel) fg else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(if (isSel) bg else SurfaceSubtle)
                    .clickable { onSelect(category.key) }
                    .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
            )
        }
    }
}
