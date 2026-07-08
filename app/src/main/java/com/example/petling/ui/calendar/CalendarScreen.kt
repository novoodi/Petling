package com.example.petling.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onAddSchedule: (Long) -> Unit,
    onOpenSchedule: (Long) -> Unit,
) {
    val container = appContainer()
    val vm: CalendarViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CalendarViewModel(container.scheduleRepository, container.clock) }
        },
    )
    val month by vm.visibleMonth.collectAsStateWithLifecycle()
    val selected by vm.selectedDate.collectAsStateWithLifecycle()
    val monthSchedules by vm.monthSchedules.collectAsStateWithLifecycle()
    val daySchedules by vm.daySchedules.collectAsStateWithLifecycle()

    val today = container.clock.today()
    val scheduledDays = monthSchedules.groupBy { it.date }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddSchedule(selected.toEpochDay()) },
                containerColor = Brand500,
            ) {
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
            MonthHeader(
                month = month,
                onPrev = { vm.changeMonth(month.minusMonths(1)) },
                onNext = { vm.changeMonth(month.plusMonths(1)) },
                onToday = { vm.goToToday() },
            )
            WeekdayHeader()
            MonthGrid(
                month = month,
                selected = selected,
                today = today,
                scheduledDays = scheduledDays,
                onSelect = vm::selectDate,
            )
            Spacer(Modifier.height(Dimens.Space4))
            Text(
                "${selected.monthValue}월 ${selected.dayOfMonth}일",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Dimens.Space2))
            if (daySchedules.isEmpty()) {
                Text("이 날은 일정이 없어요.", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.Space2)) {
                    items(daySchedules, key = { it.id }) { s ->
                        DayScheduleRow(s) { onOpenSchedule(s.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("${month.year}년 ${month.monthValue}월", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToday) { Text("오늘") }
            IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, "이전 달") }
            IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, "다음 달") }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val days = listOf("일", "월", "화", "수", "목", "금", "토")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { d ->
            Text(
                d,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    scheduledDays: Map<LocalDate, List<Schedule>>,
    onSelect: (LocalDate) -> Unit,
) {
    val firstDay = month.atDay(1)
    // 일요일 시작(getDayOfWeek: MON=1..SUN=7 → 일요일 인덱스 0)
    val leadingBlanks = firstDay.dayOfWeek.value % 7
    val totalCells = leadingBlanks + month.lengthOfMonth()
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNum)
                            DayCell(
                                date = date,
                                isSelected = date == selected,
                                isToday = date == today,
                                schedules = scheduledDays[date].orEmpty(),
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    schedules: List<Schedule>,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
            .then(if (isToday && !isSelected) Modifier.border(1.dp, Brand500, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val hasImportant = schedules.any { it.isImportant }
            if (schedules.isNotEmpty()) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (hasImportant) MaterialTheme.colorScheme.error else Brand500),
                )
            }
        }
    }
}

@Composable
private fun DayScheduleRow(schedule: Schedule, onClick: () -> Unit) {
    PetlingCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    schedule.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (schedule.status == ScheduleStatus.COMPLETED) TextTertiary else MaterialTheme.colorScheme.onSurface,
                )
                Text(schedule.timeLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            schedule.category?.let { CategoryBadge(it) }
        }
    }
}
