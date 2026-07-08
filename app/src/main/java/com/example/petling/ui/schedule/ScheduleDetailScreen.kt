package com.example.petling.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.domain.model.ScheduleStatus
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.CategoryBadge
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.PetlingButtonStyle
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.components.timeLabel
import com.example.petling.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailScreen(
    scheduleId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val container = appContainer()
    val vm: ScheduleDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ScheduleDetailViewModel(scheduleId, container.scheduleRepository, container.characterRepository)
            }
        },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("일정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(scheduleId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "수정")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "삭제")
                    }
                },
            )
        },
    ) { padding ->
        when (val st = state) {
            is ScheduleDetailUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) { CircularProgressIndicator() }

            is ScheduleDetailUiState.NotFound -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Dimens.ScreenPaddingFocused),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("일정을 찾을 수 없어요.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.Space2))
                    Text(
                        "이미 삭제되었거나 만료된 알림일 수 있어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Dimens.Space4))
                    PetlingButton("돌아가기", onClick = onBack, style = PetlingButtonStyle.Secondary)
                }
            }

            is ScheduleDetailUiState.Loaded -> {
                val s = st.schedule
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = Dimens.ScreenPaddingFocused),
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space4),
                ) {
                    Spacer(Modifier.height(Dimens.Space4))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        StatusBadge(s.status)
                        Spacer(Modifier.width(Dimens.Space2))
                        if (s.isImportant) {
                            Text("⭐ 중요", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(s.title, style = MaterialTheme.typography.headlineSmall)

                    PetlingCard(modifier = Modifier.fillMaxWidth()) {
                        DetailLine("날짜", s.date.toString())
                        DetailLine("시간", s.timeLabel())
                        s.location?.let { DetailLine("장소", it) }
                        s.memo?.let { DetailLine("메모", it) }
                        s.category?.let {
                            Spacer(Modifier.height(Dimens.Space2))
                            CategoryBadge(it)
                        }
                    }

                    Spacer(Modifier.height(Dimens.Space2))
                    when (s.status) {
                        ScheduleStatus.COMPLETED -> {
                            PetlingButton("완료 취소", onClick = { vm.revert() }, style = PetlingButtonStyle.Secondary)
                        }
                        else -> {
                            PetlingButton("완료했어요!", onClick = { vm.complete() })
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("일정을 삭제할까요?") },
            text = { Text("삭제하면 예약된 알림도 함께 취소돼요.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.delete(onBack)
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun StatusBadge(status: ScheduleStatus) {
    val (label, color) = when (status) {
        ScheduleStatus.PENDING -> "예정" to MaterialTheme.colorScheme.primary
        ScheduleStatus.COMPLETED -> "완료" to MaterialTheme.colorScheme.tertiary
        ScheduleStatus.MISSED -> "놓침" to MaterialTheme.colorScheme.error
    }
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusSm))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = Dimens.Space3, vertical = Dimens.Space1),
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space1)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
