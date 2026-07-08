package com.example.petling.ui.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.domain.model.Category
import com.example.petling.domain.model.CharacterSpec
import com.example.petling.ui.appContainer
import com.example.petling.ui.character.LocalCharacterRenderer
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.PetlingButtonStyle
import com.example.petling.ui.library.CaptureThumbnail
import com.example.petling.ui.library.categoryColors
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.SurfaceSubtle
import com.example.petling.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureScreen(
    initialImageUri: Uri?,
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val container = appContainer()
    val vm: CaptureViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CaptureViewModel(
                    container.captureRepository,
                    container.categoryRepository,
                    container.characterRepository.characterState,
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val character by vm.character.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val categoriesByKey by vm.categoriesByKey.collectAsStateWithLifecycle()
    val renderer = LocalCharacterRenderer.current
    val startUri = remember { initialImageUri }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) vm.processImage(uri) else onBack() }

    LaunchedEffect(Unit) {
        if (startUri != null) {
            vm.processImage(startUri)
        } else {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("자동 정리") },
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
                .padding(horizontal = Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.stage) {
                IngestStage.PROCESSING -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(Dimens.Space4))
                    Text("살펴보고 정리하는 중이에요…", color = TextTertiary)
                }

                IngestStage.FAILED -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("이미지를 불러오지 못했어요.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.Space4))
                    PetlingButton("다시 고르기", onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                }

                IngestStage.DONE -> {
                    val result = state.result!!
                    val currentKey = state.currentKey ?: result.item.categoryKey
                    val category = categoriesByKey[currentKey]
                    Spacer(Modifier.height(Dimens.Space4))
                    // 캐릭터 리액션
                    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                        character?.let {
                            renderer.Render(
                                CharacterSpec.from(it, animation = com.example.petling.domain.model.CharacterAnimation.BOUNCE),
                                Modifier.size(140.dp),
                            )
                        }
                    }
                    Text("${category?.label ?: "기타"}(으)로 정리했어요!", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(Dimens.Space1))
                    result.growth?.let { g ->
                        val msg = if (g.isNewType) "새로운 종류를 모았어요! +${g.xpAmount} XP" else "+${g.xpAmount} XP"
                        Text(msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(Modifier.height(Dimens.Space4))
                    // 썸네일 + 제목
                    CaptureThumbnail(
                        result.item.imagePath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(Dimens.RadiusLg)),
                    )
                    Spacer(Modifier.height(Dimens.Space2))
                    Text(result.item.title, style = MaterialTheme.typography.bodyLarge)

                    Spacer(Modifier.height(Dimens.Space4))
                    Text("분류 바꾸기", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Dimens.Space2))
                    CategoryChips(categories = categories, selectedKey = currentKey, onSelect = vm::changeCategory)

                    Spacer(Modifier.height(Dimens.Space5))
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    // 비서: 자동 캘린더 등록 결과
                    if (result.registeredScheduleId != null && !state.scheduleUndone) {
                        AssistantScheduleCard(onUndo = { vm.undoSchedule() })
                        Spacer(Modifier.height(Dimens.Space2))
                    } else if (result.scheduleSeed != null && !state.scheduleRegistered) {
                        PetlingButton("일정으로 등록", onClick = { vm.registerAsSchedule() })
                        Spacer(Modifier.height(Dimens.Space2))
                    } else if (state.scheduleRegistered) {
                        Text("일정에 등록됐어요 ✓", color = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.height(Dimens.Space2))
                    }
                    // 비서: 링크 열기
                    result.item.linkUrl?.let { url ->
                        PetlingButton(
                            "🔗 링크 열기",
                            onClick = { com.example.petling.ui.ActionIntents.openUrl(ctx, url) },
                            style = PetlingButtonStyle.Secondary,
                        )
                        Spacer(Modifier.height(Dimens.Space2))
                    }
                    // 비서: 장소 → 지도 열기
                    if (category?.baseType == com.example.petling.domain.model.CaptureType.PLACE) {
                        PetlingButton(
                            "📍 지도에서 보기",
                            onClick = { com.example.petling.ui.ActionIntents.openMap(ctx, result.item.title) },
                            style = PetlingButtonStyle.Secondary,
                        )
                        Spacer(Modifier.height(Dimens.Space2))
                    }
                    PetlingButton("보관함에서 보기", onClick = onOpenLibrary, style = PetlingButtonStyle.Secondary)
                    Spacer(Modifier.height(Dimens.Space6))
                }
            }
        }
    }
}

@Composable
private fun AssistantScheduleCard(onUndo: () -> Unit) {
    com.example.petling.ui.components.PetlingCard(modifier = Modifier.fillMaxWidth()) {
        Text("🗓️ 캘린더에 등록했어요", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Dimens.Space1))
        Text(
            "하루 전 저녁과 당일 아침에 미리 알려줄게요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Space2))
        Text(
            "일정 취소",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.RadiusSm))
                .clickable(onClick = onUndo)
                .padding(vertical = Dimens.Space1, horizontal = Dimens.Space2),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(categories: List<Category>, selectedKey: String?, onSelect: (String) -> Unit) {
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
