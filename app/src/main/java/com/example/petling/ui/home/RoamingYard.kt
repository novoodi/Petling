package com.example.petling.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.petling.domain.model.CharacterAnimation
import com.example.petling.domain.model.CharacterSpec
import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.Mood
import com.example.petling.ui.character.LocalCharacterRenderer
import com.example.petling.ui.theme.Motion
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private val SPRITE = 140.dp
private val YARD = 200.dp

/** 마당 로밍 캐릭터의 행동 상태. 렌더 스펙(애니메이션/표정/기분) 오버라이드를 결정한다. */
enum class YardBehavior { PAUSE, WALK, HOP, SLEEP, REACT, DRAG, FALL, CELEBRATE }

/** 외부(탭/드래그/일정완료)에서 로밍 루프로 보내는 선점 이벤트. */
sealed interface YardEvent {
    data object Tap : YardEvent
    data object Drop : YardEvent
    data class Celebrate(val evolved: Boolean) : YardEvent
}

/**
 * 마당 캐릭터의 위치·상태 홀더. HomeScreen에서 remember해 말풍선 꼬리 앵커로도 공유한다.
 * 모든 위치 애니메이션(animateTo)은 로밍 루프가 단독 소유하고, 드래그는 snapTo만 쓴다.
 */
@Stable
class YardState {
    val x = Animatable(0f)      // 스프라이트 좌측 edge px(마당 로컬)
    val y = Animatable(0f)      // 0=착지, 음수=들림
    val dust = Animatable(0f)   // 착지 먼지 0..1
    val squash = Animatable(0f) // 착지 스쿼시 0..1
    val turn = Animatable(1f)   // 몸돌리기 가로 스쿼시(정면↔옆모습 전환·방향 반전)
    var behavior by mutableStateOf(YardBehavior.PAUSE)
    var facingLeft by mutableStateOf(false)
    var dragging by mutableStateOf(false)
    var spritePx by mutableFloatStateOf(0f)
    val events = Channel<YardEvent>(Channel.CONFLATED)

    /** 말풍선 꼬리가 따라올 캐릭터 중심 x(px). */
    fun centerX(): Float = x.value + spritePx / 2f

    fun celebrate(evolved: Boolean) {
        events.trySend(YardEvent.Celebrate(evolved))
    }
}

@Composable
fun rememberYardState(): YardState = remember { YardState() }

/** 렌더 스펙: 기본 스펙에 행동별 애니메이션/표정/기분 오버라이드를 얹는다(저장 안 함). */
private fun specFor(base: CharacterSpec, behavior: YardBehavior): CharacterSpec = when (behavior) {
    YardBehavior.PAUSE -> base
    YardBehavior.WALK -> base.copy(animation = CharacterAnimation.WALK)
    YardBehavior.HOP -> base.copy(
        animation = CharacterAnimation.BOUNCE,
        mood = Mood.HAPPY,
        expression = if (base.mood == Mood.HAPPY) Expression.EXCITED else Expression.HAPPY,
    )
    YardBehavior.SLEEP -> base.copy(
        animation = CharacterAnimation.SLEEP,
        mood = Mood.TIRED,
        expression = Expression.SLEEPY,
    )
    YardBehavior.REACT -> base.copy(mood = Mood.HAPPY, expression = Expression.EXCITED)
    YardBehavior.DRAG, YardBehavior.FALL -> base.copy(expression = Expression.WORRIED)
    YardBehavior.CELEBRATE -> base.copy(
        animation = CharacterAnimation.BOUNCE,
        mood = Mood.HAPPY,
        expression = Expression.EXCITED,
    )
}

@Composable
fun RoamingYard(
    baseSpec: CharacterSpec,
    state: YardState,
    modifier: Modifier = Modifier,
) {
    val renderer = LocalCharacterRenderer.current
    val density = LocalDensity.current
    val moodProvider = rememberUpdatedState(baseSpec.mood)
    val scope = rememberCoroutineScope()

    val wobble by rememberInfiniteTransition(label = "wobble").animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(140, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobbleV",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(YARD),
    ) {
        val spritePx = with(density) { SPRITE.toPx() }
        val rangePx = (with(density) { maxWidth.toPx() } - spritePx).coerceAtLeast(0f)
        val yMinPx = -(with(density) { maxHeight.toPx() } - spritePx).coerceAtLeast(0f)
        val speedWalk = with(density) { 60.dp.toPx() }
        val speedHappy = with(density) { 75.dp.toPx() }
        val speedTired = with(density) { 40.dp.toPx() }
        val speedRun = with(density) { 130.dp.toPx() }
        val jumpPx = with(density) { 12.dp.toPx() }
        state.spritePx = spritePx

        LaunchedEffect(rangePx, spritePx) {
            if (rangePx <= 0f) return@LaunchedEffect
            state.x.updateBounds(0f, rangePx)
            if (state.x.value !in 0f..rangePx) state.x.snapTo(rangePx / 2f)
            roamLoop(state, rangePx, yMinPx, moodProvider, speedWalk, speedHappy, speedTired, speedRun, jumpPx)
        }

        val spec = specFor(baseSpec, state.behavior)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(SPRITE)
                .offset { IntOffset(state.x.value.roundToInt(), state.y.value.roundToInt()) }
                .graphicsLayer {
                    scaleX = (if (state.facingLeft) -1f else 1f) * state.turn.value
                    rotationZ = if (state.dragging) wobble * 8f else 0f
                    val sq = state.squash.value
                    if (sq > 0f) {
                        scaleY = scaleY * (1f - sq * 0.12f)
                        // scaleX은 방향부호 유지한 채 가로 확장
                        scaleX = scaleX * (1f + sq * 0.10f)
                    }
                    transformOrigin = TransformOrigin(0.5f, 0.9f)
                }
                .pointerInput(state) {
                    detectTapGestures {
                        if (!state.dragging) state.events.trySend(YardEvent.Tap)
                    }
                }
                .pointerInput(state, rangePx, yMinPx) {
                    detectDragGestures(
                        onDragStart = {
                            state.dragging = true
                            state.behavior = YardBehavior.DRAG
                        },
                        onDragEnd = {
                            state.dragging = false
                            state.events.trySend(YardEvent.Drop)
                        },
                        onDragCancel = {
                            state.dragging = false
                            state.events.trySend(YardEvent.Drop)
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            scope.launch {
                                state.x.snapTo((state.x.value + delta.x).coerceIn(0f, rangePx))
                                state.y.snapTo((state.y.value + delta.y).coerceIn(yMinPx, 0f))
                            }
                        },
                    )
                },
        ) {
            renderer.Render(spec, Modifier.fillMaxSize())
        }

        // 착지 먼지 오버레이(스프라이트와 형제 → scaleX 반전 영향 없음)
        if (state.dust.value > 0f) {
            val d = state.dust.value
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(SPRITE)
                    .offset { IntOffset(state.x.value.roundToInt(), 0) },
            ) {
                val fy = size.height * 0.97f
                val cx = size.width / 2f
                val a = (0.55f * (1f - d)).coerceIn(0f, 1f)
                listOf(-1f, 1f).forEach { s ->
                    drawCircle(
                        Color(0xFFB7A98F).copy(alpha = a),
                        radius = size.width * (0.05f + d * 0.05f),
                        center = Offset(cx + s * size.width * (0.14f + d * 0.10f), fy),
                    )
                }
            }
        }
    }
}

// ─────────────────────────── 로밍 루프 ───────────────────────────

private suspend fun roamLoop(
    state: YardState,
    rangePx: Float,
    yMinPx: Float,
    moodProvider: androidx.compose.runtime.State<Mood>,
    speedWalk: Float,
    speedHappy: Float,
    speedTired: Float,
    speedRun: Float,
    jumpPx: Float,
) {
    var pending: YardEvent? = null
    var lastBehavior = YardBehavior.PAUSE
    while (coroutineContext.isActive) {
        if (state.dragging) {
            snapshotFlow { state.dragging }.first { !it }
            continue
        }
        val ev = pending ?: state.events.tryReceive().getOrNull()
        pending = null
        val behavior = when (ev) {
            is YardEvent.Drop -> YardBehavior.FALL
            is YardEvent.Tap -> YardBehavior.REACT
            is YardEvent.Celebrate -> YardBehavior.CELEBRATE
            null -> chooseNext(lastBehavior, moodProvider.value)
        }
        val interrupt = raceEvents(state) {
            setBehavior(state, behavior)
            execute(state, behavior, ev, rangePx, moodProvider.value, speedWalk, speedHappy, speedTired, speedRun, jumpPx)
        }
        // 전환 스쿼시 중 선점되면 눌린 채로 남지 않게 복원
        if (state.turn.value != 1f) state.turn.snapTo(1f)
        pending = interrupt
        lastBehavior = if (behavior == YardBehavior.WALK || behavior == YardBehavior.PAUSE) behavior else YardBehavior.PAUSE
    }
}

/** 정면↔옆모습(WALK) 경계를 넘을 때 짧은 가로 스쿼시로 "몸을 돌리는" 전환. */
private suspend fun setBehavior(state: YardState, new: YardBehavior) {
    val crossing = (state.behavior == YardBehavior.WALK) != (new == YardBehavior.WALK)
    if (crossing) {
        state.turn.animateTo(0.15f, tween(90, easing = LinearEasing))
        state.behavior = new
        state.turn.animateTo(1f, tween(90, easing = LinearEasing))
    } else {
        state.behavior = new
    }
}

/** block을 실행하되 선점 이벤트가 오면 취소하고 그 이벤트를 반환. */
private suspend fun raceEvents(state: YardState, block: suspend () -> Unit): YardEvent? = coroutineScope {
    val work = launch { block() }
    val result = select {
        work.onJoin { null }
        state.events.onReceive { it }
    }
    if (result != null) work.cancel()
    result
}

private fun chooseNext(last: YardBehavior, mood: Mood): YardBehavior {
    val happy = mood == Mood.HAPPY
    val tired = mood == Mood.TIRED
    val weights: List<Pair<YardBehavior, Int>> = when (last) {
        YardBehavior.WALK -> listOf(
            YardBehavior.PAUSE to 70, YardBehavior.HOP to 20, YardBehavior.WALK to 10,
        )
        YardBehavior.HOP -> listOf(YardBehavior.PAUSE to 80, YardBehavior.WALK to 20)
        else -> buildList { // PAUSE 등
            add(YardBehavior.WALK to 55)
            add(YardBehavior.HOP to if (happy) 25 else 15)
            add(YardBehavior.PAUSE to 10)
            if (tired) add(YardBehavior.SLEEP to 30)
        }
    }
    return weightedPick(weights)
}

private fun weightedPick(weights: List<Pair<YardBehavior, Int>>): YardBehavior {
    val total = weights.sumOf { it.second }
    if (total <= 0) return YardBehavior.PAUSE
    var r = Random.nextInt(total)
    for ((b, w) in weights) {
        if (r < w) return b
        r -= w
    }
    return weights.first().first
}

private suspend fun execute(
    state: YardState,
    behavior: YardBehavior,
    ev: YardEvent?,
    rangePx: Float,
    mood: Mood,
    speedWalk: Float,
    speedHappy: Float,
    speedTired: Float,
    speedRun: Float,
    jumpPx: Float,
) {
    when (behavior) {
        YardBehavior.PAUSE -> {
            val dur = Random.nextLong(1500, 4000)
            if (Random.nextFloat() < 0.4f) {
                delay(dur / 2)
                state.facingLeft = !state.facingLeft
                delay(dur / 2)
            } else {
                delay(dur)
            }
        }
        YardBehavior.WALK -> {
            val speed = when (mood) {
                Mood.HAPPY -> speedHappy
                Mood.TIRED -> speedTired
                Mood.CALM -> speedWalk
            }
            walkTo(state, randomTarget(state.x.value, rangePx), speed)
        }
        YardBehavior.HOP -> {
            delay(Random.nextLong(700, 1200))
        }
        YardBehavior.SLEEP -> {
            // 가까운 가장자리로 걸어가 눕기
            val edge = if (state.x.value < rangePx / 2f) rangePx * 0.12f else rangePx * 0.88f
            setBehavior(state, YardBehavior.WALK)
            walkTo(state, edge, speedTired)
            setBehavior(state, YardBehavior.SLEEP)
            delay(Random.nextLong(8000, 20000))
        }
        YardBehavior.REACT -> {
            state.y.animateTo(-jumpPx, tween(220, easing = Motion.Decelerate))
            state.y.animateTo(0f, tween(300, easing = Motion.Bounce))
            delay(1300)
        }
        YardBehavior.FALL -> {
            val h = abs(state.y.value)
            val dur = (240 + h * 0.6f).toInt().coerceAtMost(460)
            state.y.animateTo(0f, tween(dur, easing = Motion.Bounce))
            land(state)
        }
        YardBehavior.CELEBRATE -> {
            val evolved = (ev as? YardEvent.Celebrate)?.evolved == true
            val laps = if (evolved) 2 else 1
            val start = state.x.value
            repeat(laps) {
                walkTo(state, rangePx, speedRun)
                walkTo(state, 0f, speedRun)
            }
            walkTo(state, start.coerceIn(0f, rangePx), speedRun)
        }
        YardBehavior.DRAG -> {
            // 드래그는 제스처가 구동 — 여기 도달 시 해제될 때까지 대기
            snapshotFlow { state.dragging }.first { !it }
        }
    }
}

private fun randomTarget(current: Float, rangePx: Float): Float {
    if (rangePx < 1f) return current
    val minStep = maxOf(rangePx * 0.15f, 1f)
    repeat(6) {
        val t = Random.nextFloat() * rangePx
        if (abs(t - current) >= minStep) return t
    }
    return (current + rangePx / 2f) % rangePx
}

private suspend fun walkTo(state: YardState, target: Float, speedPxPerSec: Float) {
    val dist = abs(target - state.x.value)
    if (dist < 1f) return
    val newFacing = target < state.x.value
    if (newFacing != state.facingLeft) {
        // 걷는 중 방향 반전도 몸돌리기 스쿼시로
        state.turn.animateTo(0.3f, tween(70, easing = LinearEasing))
        state.facingLeft = newFacing
        state.turn.animateTo(1f, tween(70, easing = LinearEasing))
    }
    val dur = (dist / speedPxPerSec * 1000f).toInt().coerceIn(120, 6000)
    state.x.animateTo(target, tween(dur, easing = LinearEasing))
}

private suspend fun land(state: YardState) {
    state.squash.snapTo(1f)
    state.dust.snapTo(1f)
    coroutineScope {
        launch { state.squash.animateTo(0f, tween(260, easing = Motion.Bounce)) }
        launch { state.dust.animateTo(0f, tween(400, easing = LinearEasing)) }
    }
}
