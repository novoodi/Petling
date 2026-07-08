package com.example.petling.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.CharacterAnimation
import com.example.petling.domain.model.CharacterSpec
import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Mood
import com.example.petling.ui.appContainer
import com.example.petling.ui.character.ModoriPalette
import com.example.petling.ui.character.drawCreature
import com.example.petling.ui.components.PetlingButton
import com.example.petling.ui.components.PetlingButtonStyle
import com.example.petling.ui.theme.Brand50
import com.example.petling.ui.theme.Dimens
import com.example.petling.ui.theme.SurfaceSubtle
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val container = appContainer()
    val vm: OnboardingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                OnboardingViewModel(container.characterRepository, container.settings, container.clock)
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    // 첫 화면이 아니면 시스템 뒤로를 가로채 이전 단계로 돌아간다.
    androidx.activity.compose.BackHandler(enabled = state.step != OnboardingStep.WELCOME) {
        vm.goBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceSubtle)
            .padding(Dimens.ScreenPaddingFocused),
    ) {
        AnimatedContent(targetState = state.step, label = "onboardingStep") { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(onNext = vm::goToSpecies)
                OnboardingStep.SPECIES -> SpeciesStep(onSelect = vm::selectSpecies)
                OnboardingStep.HATCH -> HatchStep(species = state.species, colorHue = state.colorHue, onHatched = vm::onHatched)
                OnboardingStep.QUIZ -> QuizStep(vm, state)
                OnboardingStep.NAMING -> NamingStep(vm, state)
                OnboardingStep.CATEGORY -> CategoryStep(container, onComplete)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // 알림 권한이 거부되면 재안내 상태로 전환한다. 알림은 리마인더 핵심 기능이지만
    // 필수는 아니므로 온보딩을 막지 않고, 다시 요청/설정 열기/그냥 계속을 제공한다.
    var notifDenied by remember { mutableStateOf(false) }
    var notifPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onNext()
        } else {
            val activity = context as? android.app.Activity
            val canAskAgain = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    it, android.Manifest.permission.POST_NOTIFICATIONS,
                )
            } ?: false
            notifPermanentlyDenied = !canAskAgain
            notifDenied = true
        }
    }

    fun requestOrProceed() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onNext()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Petling에 온 걸 환영해요", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Dimens.Space4))
        Text(
            "일정을 등록하고 지킬수록 나만의 작은 친구가 함께 자라요.\n\n" +
                "이 앱은 여러분의 화면이나 목소리를 서버로 보내지 않아요. 모든 건 이 기기 안에서만 처리돼요.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.Space10))
        if (!notifDenied) {
            PetlingButton("시작하기", onClick = { requestOrProceed() }, fillWidth = true)
        } else {
            Text(
                "알림을 꺼두면 캐릭터가 다가오는 일정을 알려주지 못해요.\n나중에 설정에서 언제든 켤 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Dimens.Space4))
            if (notifPermanentlyDenied) {
                PetlingButton("설정에서 알림 켜기", onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }, fillWidth = true)
            } else {
                PetlingButton("알림 허용하기", onClick = { requestOrProceed() }, fillWidth = true)
            }
            Spacer(Modifier.height(Dimens.Space3))
            PetlingButton(
                "알림 없이 계속",
                onClick = onNext,
                style = PetlingButtonStyle.Secondary,
                fillWidth = true,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SpeciesStep(onSelect: (com.example.petling.domain.model.Species) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Dimens.Space8))
        Text("함께할 친구를 골라요", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Dimens.Space2))
        Text(
            "고른 친구를 알에서 부화시켜 함께 키워요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.Space6))
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space3, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space3),
        ) {
            com.example.petling.domain.model.Species.entries.forEach { species ->
                SpeciesCard(species, onClick = { onSelect(species) })
            }
        }
    }
}

@Composable
private fun SpeciesCard(species: com.example.petling.domain.model.Species, onClick: () -> Unit) {
    val palette = ModoriPalette.from(species.defaultHue)
    Column(
        modifier = Modifier
            .size(width = 100.dp, height = 128.dp)
            .clip(RoundedCornerShape(Dimens.RadiusLg))
            .background(Brand50)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(76.dp)) {
            drawCreature(
                species = species,
                stage = GrowthStage.JUVENILE,
                branch = null,
                mood = Mood.HAPPY,
                expression = Expression.HAPPY,
                palette = palette,
                eyeStyle = 0,
            )
        }
        Text(species.displayName, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HatchStep(species: com.example.petling.domain.model.Species, colorHue: Float, onHatched: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val crack = if (taps >= 3) 1f else taps / 3f

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("알을 톡톡 두드려 깨워보세요", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Dimens.Space8))
        val palette = ModoriPalette.from(colorHue)
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .clickable {
                    if (taps < 3) {
                        taps++
                        scope.launch {
                            shake.snapTo(0f)
                            shake.animateTo(1f, androidx.compose.animation.core.tween(80))
                            shake.animateTo(-1f, androidx.compose.animation.core.tween(80))
                            shake.animateTo(0f, androidx.compose.animation.core.tween(80))
                            if (taps >= 3) {
                                flash.animateTo(1f, androidx.compose.animation.core.tween(200))
                                onHatched()
                            }
                        }
                    }
                },
        ) {
            rotate(shake.value * 8f) {
                drawCreature(
                    species = species,
                    stage = if (taps >= 3) GrowthStage.JUVENILE else GrowthStage.EGG,
                    branch = null,
                    mood = Mood.CALM,
                    expression = Expression.HAPPY,
                    palette = palette,
                    eyeStyle = 0,
                    crackProgress = crack,
                )
            }
        }
        Spacer(Modifier.height(Dimens.Space6))
        Text("${taps} / 3", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun QuizStep(vm: OnboardingViewModel, state: OnboardingUiState) {
    val question = vm.questions[state.currentQuestion.coerceIn(0, vm.questions.lastIndex)]
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(Dimens.Space8))
        Text(
            "${state.currentQuestion + 1} / ${vm.questions.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Dimens.Space3))
        Text(question.text, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Dimens.Space8))
        question.options.forEachIndexed { index, option ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.Space2)
                    .clip(RoundedCornerShape(Dimens.RadiusLg))
                    .background(Brand50)
                    .clickable { vm.answerQuestion(index) }
                    .padding(Dimens.Space4),
            ) {
                Text(option.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun NamingStep(vm: OnboardingViewModel, state: OnboardingUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Dimens.Space6))
        Text("${state.determinedPersonality?.displayName} 친구네요!", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Dimens.Space4))
        val palette = ModoriPalette.from(state.colorHue)
        Canvas(modifier = Modifier.size(160.dp)) {
            drawCreature(
                species = state.species,
                stage = GrowthStage.JUVENILE,
                branch = null,
                mood = Mood.HAPPY,
                expression = Expression.HAPPY,
                palette = palette,
                eyeStyle = state.eyeStyle,
            )
        }
        Spacer(Modifier.height(Dimens.Space4))
        OutlinedTextField(
            value = state.name,
            onValueChange = vm::updateName,
            label = { Text("이름을 지어주세요") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.Space5))
        Text("색상", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
        Slider(
            value = state.colorHue,
            onValueChange = vm::updateColorHue,
            valueRange = 0f..360f,
        )
        Spacer(Modifier.height(Dimens.Space2))
        Text("눈 모양", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
        Row3EyeStyles(state.eyeStyle, vm::updateEyeStyle)
        Spacer(Modifier.height(Dimens.Space6))
        PetlingButton(
            "이 친구와 시작하기",
            onClick = { vm.completeNaming() },
            enabled = state.name.isNotBlank(),
        )
        Spacer(Modifier.height(Dimens.Space4))
    }
}

/** 온보딩 마지막: 자주 모을 분류 고르기(설정과 동일 UI 재사용). */
@Composable
private fun CategoryStep(container: com.example.petling.di.AppContainer, onComplete: () -> Unit) {
    val vm: com.example.petling.ui.category.CategoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { com.example.petling.ui.category.CategoryViewModel(container.categoryRepository) }
        },
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
    ) {
        Spacer(Modifier.height(Dimens.Space4))
        Text("어떤 걸 자주 모으나요?", style = MaterialTheme.typography.headlineSmall)
        com.example.petling.ui.category.CategoryManageBody(vm)
        Spacer(Modifier.height(Dimens.Space4))
        PetlingButton("이대로 시작하기", onClick = onComplete, fillWidth = true)
        Spacer(Modifier.height(Dimens.Space6))
    }
}

@Composable
private fun Row3EyeStyles(selected: Int, onSelect: (Int) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space2),
    ) {
        (0..2).forEach { style ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusMd))
                    .background(if (selected == style) Brand50 else SurfaceSubtle)
                    .clickable { onSelect(style) }
                    .padding(horizontal = Dimens.Space5, vertical = Dimens.Space3),
            ) {
                Text("눈 ${style + 1}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
