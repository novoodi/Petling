package com.example.petling.ui.schedule

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
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.domain.model.ScheduleCategory
import com.example.petling.ui.appContainer
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.categoryColors
import com.example.petling.ui.components.formatTime
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.SurfaceSubtle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditScreen(
    scheduleId: Long?,
    presetEpochDay: Long?,
    seed: com.example.petling.domain.model.ParsedDraftSeed? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val container = appContainer()
    val vm: ScheduleEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ScheduleEditViewModel(container.scheduleRepository, container.settings, container.clock)
            }
        },
    )
    androidx.compose.runtime.LaunchedEffect(scheduleId, presetEpochDay) {
        vm.load(scheduleId, presetEpochDay, seed)
    }
    val state by vm.state.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (scheduleId == null) "일정 추가" else "일정 수정") },
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
                .padding(horizontal = Dimens.ScreenPaddingFocused)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space4),
        ) {
            Spacer(Modifier.height(Dimens.Space2))
            if (state.needsReview) {
                Text(
                    "AI가 읽은 내용이에요. 맞는지 확인하고 고쳐주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.RadiusMd))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(Dimens.Space3),
                )
            }
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::updateTitle,
                label = { Text("제목") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            FieldRow(label = "날짜", value = state.date.toString()) { showDatePicker = true }
            if (state.date.isBefore(LocalDate.now())) {
                Text(
                    "지난 날짜예요. 저장하면 바로 '놓침'으로 표시돼요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("종일", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.isAllDay, onCheckedChange = vm::updateAllDay)
            }
            if (!state.isAllDay) {
                FieldRow(label = "시간", value = formatTime(state.startMinuteOfDay)) { showTimePicker = true }
            }

            OutlinedTextField(
                value = state.location,
                onValueChange = vm::updateLocation,
                label = { Text("장소 (선택)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.memo,
                onValueChange = vm::updateMemo,
                label = { Text("메모 (선택)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("분류 (선택)", style = MaterialTheme.typography.labelMedium)
            CategoryChips(state.category, vm::updateCategory)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("중요 일정", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.isImportant, onCheckedChange = vm::updateImportant)
            }

            Text("알림", style = MaterialTheme.typography.labelMedium)
            ReminderChips(state.reminderOffsetMin, vm::updateReminderOffset)

            Spacer(Modifier.height(Dimens.Space4))
            PetlingButton("저장", onClick = { vm.save(onSaved) }, enabled = state.canSave)
            Spacer(Modifier.height(Dimens.Space6))
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toEpochDay() * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let {
                        vm.updateDate(LocalDate.ofEpochDay(it / 86_400_000L))
                    }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } },
        ) { DatePicker(state = dpState) }
    }

    if (showTimePicker) {
        val tpState = rememberTimePickerState(
            initialHour = (state.startMinuteOfDay ?: 540) / 60,
            initialMinute = (state.startMinuteOfDay ?: 540) % 60,
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateTime(tpState.hour, tpState.minute)
                    showTimePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("취소") } },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Dimens.Space4),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { TimePicker(state = tpState) }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .background(SurfaceSubtle)
            .clickable(onClick = onClick)
            .padding(Dimens.Space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CategoryChips(selected: ScheduleCategory?, onSelect: (ScheduleCategory?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space2), modifier = Modifier.fillMaxWidth()) {
        ScheduleCategory.entries.forEach { category ->
            val (fg, bg) = categoryColors(category)
            val isSel = selected == category
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSel) fg else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(if (isSel) bg else SurfaceSubtle)
                    .clickable { onSelect(if (isSel) null else category) }
                    .padding(vertical = Dimens.Space2),
            )
        }
    }
}

@Composable
private fun ReminderChips(selected: Int?, onSelect: (Int?) -> Unit) {
    data class Opt(val label: String, val value: Int?)
    val options = listOf(
        Opt("없음", null),
        Opt("정시", 0),
        Opt("10분 전", 10),
        Opt("30분 전", 30),
        Opt("1시간 전", 60),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space1),
        modifier = Modifier.fillMaxWidth().horizontalScrollSafe(),
    ) {
        options.forEach { opt ->
            val isSel = selected == opt.value
            Text(
                text = opt.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusSm))
                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else SurfaceSubtle)
                    .clickable { onSelect(opt.value) }
                    .padding(horizontal = Dimens.Space3, vertical = Dimens.Space2),
            )
        }
    }
}

private fun Modifier.horizontalScrollSafe(): Modifier = this
