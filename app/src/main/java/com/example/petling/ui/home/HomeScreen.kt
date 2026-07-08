package com.example.petling.ui.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.domain.model.Schedule
import com.example.petling.domain.model.ScheduleStatus
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.CategoryBadge
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.components.timeLabel
import com.example.petling.ui.theme.Brand500
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.TextTertiary

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddSchedule: () -> Unit,
    onCaptureSchedule: () -> Unit,
    onVoiceSchedule: () -> Unit,
    onPasteSchedule: () -> Unit,
    onOpenSchedule: (Long) -> Unit,
    onOpenCharacter: () -> Unit,
) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(container.scheduleRepository, container.characterRepository, container.clock)
            }
        },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }

    // 전역 마당 캐릭터가 홈에서 띄울 말풍선 문구를 발행(오버레이가 구독).
    LaunchedEffect(state.greeting) { container.petSpeech.value = state.greeting }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is HomeEvent.XpGained -> {
                    container.petCelebrate.tryEmit(false)
                    snackbar.showMessage("+${event.amount} XP · ${event.message}")
                }
                is HomeEvent.Evolved -> {
                    container.petCelebrate.tryEmit(true)
                    snackbar.showMessage("🎉 ${event.stage.displayName} · ${event.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }, containerColor = Brand500) {
                Icon(Icons.Filled.Add, contentDescription = "일정 추가", tint = Color.White)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.ScreenPadding),
        ) {
            val character = state.character
            Spacer(Modifier.height(Dimens.Space4))
            if (character != null) {
                StageProgress(
                    character.name,
                    state.progressInStage,
                    state.nextStageTarget,
                    modifier = Modifier.clickable(onClick = onOpenCharacter),
                )
            }
            Spacer(Modifier.height(Dimens.Space5))
            Text("오늘의 일정", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Dimens.Space2))
            if (state.todaySchedules.isEmpty()) {
                Text(
                    "오늘은 등록된 일정이 없어요. 아래 + 로 추가해보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary,
                    modifier = Modifier.padding(vertical = Dimens.Space6),
                )
            } else {
                // 하단 여백: 마당을 배회하는 캐릭터 뒤로 마지막 일정이 가리지 않게
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space2),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp),
                ) {
                    items(state.todaySchedules, key = { it.id }) { schedule ->
                        ScheduleRow(
                            schedule = schedule,
                            onToggle = { vm.complete(schedule) },
                            onOpen = { onOpenSchedule(schedule.id) },
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddScheduleSheet(
            onDismiss = { showAddSheet = false },
            onManual = { showAddSheet = false; onAddSchedule() },
            onCapture = { showAddSheet = false; onCaptureSchedule() },
            onVoice = { showAddSheet = false; onVoiceSchedule() },
            onPaste = { showAddSheet = false; onPasteSchedule() },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleSheet(
    onDismiss: () -> Unit,
    onManual: () -> Unit,
    onCapture: () -> Unit,
    onVoice: () -> Unit,
    onPaste: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(Dimens.ScreenPaddingFocused)) {
            Text("일정 추가", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Dimens.Space4))
            SheetOption(Icons.Filled.Add, "직접 입력", onManual)
            SheetOption(Icons.Filled.PhotoCamera, "캡처로 등록", onCapture)
            SheetOption(Icons.Filled.Mic, "말로 등록", onVoice)
            SheetOption(Icons.Filled.ContentPaste, "텍스트 붙여넣기", onPaste)
            Spacer(Modifier.height(Dimens.Space6))
        }
    }
}

@Composable
private fun SheetOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.Space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Brand500)
        Spacer(Modifier.size(Dimens.Space3))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StageProgress(name: String, progress: Int, target: Int?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (target != null) {
            Text(
                "다음 단계까지 캡처 $progress / $target",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Dimens.Space1))
            LinearProgressIndicator(
                progress = { (progress.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Brand500,
            )
        } else {
            Text("$name(은)는 최종 단계에 도달했어요!", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ScheduleRow(schedule: Schedule, onToggle: () -> Unit, onOpen: () -> Unit) {
    PetlingCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val done = schedule.status == ScheduleStatus.COMPLETED
            Icon(
                imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (done) "완료됨" else "완료 체크",
                tint = if (done) Brand500 else TextTertiary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(enabled = !done, onClick = onToggle),
            )
            Spacer(Modifier.size(Dimens.Space3))
            Column(modifier = Modifier.weight(1f)) {
                Text(schedule.title, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        schedule.timeLabel() + (schedule.location?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            schedule.category?.let {
                CategoryBadge(it)
            }
        }
    }
}

private suspend fun SnackbarHostState.showMessage(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}
