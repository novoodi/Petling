package com.example.petling.ui.characterdetail

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.petling.data.local.entity.GrowthSnapshotEntity
import com.example.petling.domain.model.CharacterState
import com.example.petling.ui.appContainer
import com.example.petling.ui.character.LocalCharacterRenderer
import com.example.petling.ui.components.PetlingCard
import com.example.petling.ui.theme.Dimens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(onBack: () -> Unit = {}) {
    val container = appContainer()
    val character by container.characterRepository.characterState.collectAsStateWithLifecycle(initialValue = null)
    val snapshots by container.characterRepository.snapshots.collectAsStateWithLifecycle(initialValue = emptyList())
    var tab by remember { mutableIntStateOf(0) }
    val renderer = LocalCharacterRenderer.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(character?.name ?: "내 친구") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("상태") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("성장 다이어리") })
            }
            when (tab) {
                0 -> character?.let { StatusTab(it, renderer) }
                else -> DiaryTab(snapshots, character?.hatchedAt ?: 0L, character?.personality) { container.characterRepository.specFromSnapshotJson(it) }
            }
        }
    }
}

@Composable
private fun StatusTab(
    character: CharacterState,
    renderer: com.example.petling.ui.character.CharacterRenderer,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Dimens.Space4))
        Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            renderer.Render(com.example.petling.domain.model.CharacterSpec.from(character), Modifier.size(180.dp))
        }
        Text(character.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            "${character.stage.displayName} · ${character.personality.displayName}" +
                (character.branch?.let { " · ${it.displayName}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Space4))
        AffectionGauge(character.affection)
        Spacer(Modifier.height(Dimens.Space4))
        PetlingCard(modifier = Modifier.fillMaxWidth()) {
            // 성장(단계)은 모은 캡처가 구동한다 → 지표를 함께 노출해 성장 근거를 명확히.
            StatLine("모은 캡처", "${character.captureCount}개")
            StatLine("총 경험치", "${character.totalXp} XP")
            StatLine("완료한 일정", "${character.completedCount}건")
            StatLine("현재 연속", "${character.currentStreakDays}일")
            StatLine("최고 연속", "${character.bestStreakDays}일")
            StatLine("오늘 컨디션", character.mood.displayName)
        }
    }
}

/**
 * 호감도 게이지: 하트 5개(20씩 부분 채움) + 단계명. 기질·원값은 숨기고 관계 단계만 보여준다.
 */
@Composable
private fun AffectionGauge(affection: Int) {
    val level = com.example.petling.domain.engine.AffectionRules.levelFor(affection)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            val fill = ((affection - i * 20) / 20f).coerceIn(0f, 1f)
            Text(
                text = if (fill >= 0.75f) "❤️" else if (fill >= 0.25f) "🧡" else "🤍",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.size(Dimens.Space2))
        Text(
            level.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space2),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun DiaryTab(
    snapshots: List<GrowthSnapshotEntity>,
    fallbackSeed: Long,
    fallbackPersonality: com.example.petling.domain.model.Personality?,
    specOf: (String) -> com.example.petling.domain.model.CharacterSpec,
) {
    val renderer = LocalCharacterRenderer.current
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }
    if (snapshots.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("아직 기록이 없어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space3),
    ) {
        items(snapshots, key = { it.id }) { snap ->
            PetlingCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(72.dp)) {
                        // 레거시 스냅샷(seed·성격 없음)은 현재 캐릭터 값으로 폴백해 과거 모습을 현재 개체와 일관되게 보인다.
                        val spec = specOf(snap.characterSpecJson).let {
                            it.copy(
                                seed = if (it.seed == 0L) fallbackSeed else it.seed,
                                personality = it.personality ?: fallbackPersonality,
                            )
                        }
                        renderer.Render(spec, Modifier.size(72.dp))
                    }
                    Spacer(Modifier.size(Dimens.Space3))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(snap.note ?: snap.stage.displayName, style = MaterialTheme.typography.titleSmall)
                        val date = Instant.ofEpochMilli(snap.achievedAt).atZone(ZoneId.systemDefault()).toLocalDate()
                        Text(date.format(formatter), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
