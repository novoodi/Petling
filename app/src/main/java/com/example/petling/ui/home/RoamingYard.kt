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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
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
import com.example.petling.domain.engine.AffectionLevel
import com.example.petling.domain.model.CharacterAnimation
import com.example.petling.domain.model.CharacterSpec
import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.Mood
import com.example.petling.domain.model.Species
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

/** 외부(탭/드래그/일정완료/앱재개/간식)에서 로밍 루프로 보내는 선점 이벤트. */
sealed interface YardEvent {
    data object Tap : YardEvent
    data object Drop : YardEvent
    data object Greet : YardEvent
    data class Celebrate(val evolved: Boolean) : YardEvent
    data class Snack(val xFraction: Float) : YardEvent
}

/**
 * 마당 캐릭터의 위치·상태 홀더. NavHost에서 remember해 전 탭이 공유한다.
 * 모든 위치 애니메이션(animateTo)은 로밍 루프가 단독 소유하고, 드래그는 snapTo만 쓴다.
 * 주의: events는 CONFLATED — 동시 다발 이벤트(Greet/Snack/Celebrate)는 마지막 것만 남는다(저빈도 허용).
 */
@Stable
class YardState {
    val x = Animatable(0f)        // 스프라이트 좌측 edge px(오버레이 root)
    val y = Animatable(0f)        // 0=발판 위(착지), 음수=점프로 들림
    // 발판의 스프라이트-top y(root px). 배회=지면선, 착지=perch 상단. 수직 위치 = baseY + y.
    val baseY = Animatable(0f)
    val dust = Animatable(0f)     // 착지 먼지 0..1
    val squash = Animatable(0f)   // 착지 스쿼시 0..1
    val turn = Animatable(1f)     // 몸돌리기 가로 스쿼시(정면↔옆모습·방향 반전)
    val rotation = Animatable(0f) // 데굴데굴(도토리 ROLL)
    val snackDrop = Animatable(0f) // 간식 낙하 0..1
    var behavior by mutableStateOf(YardBehavior.PAUSE)
    var facingLeft by mutableStateOf(false)
    var dragging by mutableStateOf(false)
    var spritePx by mutableFloatStateOf(0f)
    var snackX by mutableStateOf<Float?>(null) // 바닥 간식 중심 x(px). null=없음
    val events = Channel<YardEvent>(Channel.CONFLATED)

    /** 말풍선 꼬리가 따라올 캐릭터 중심 x(px). */
    fun centerX(): Float = x.value + spritePx / 2f

    fun celebrate(evolved: Boolean) {
        events.trySend(YardEvent.Celebrate(evolved))
    }
}

@Composable
fun rememberYardState(): YardState = remember { YardState() }

/** 로밍 루프가 참조하는 캐릭터 컨텍스트(종·기분·호감도). */
data class YardContext(val species: Species, val mood: Mood, val affection: Int) {
    val level: AffectionLevel get() = affectionLevel(affection)
}

/** 마당 치수·속도(px, 오버레이 root 기준). */
private class YardDims(
    val rangePx: Float,
    val yMinPx: Float,
    val hidePx: Float,
    val jumpPx: Float,
    val dp1: Float, // 1dp의 px
    val groundTopY: Float, // 지면선의 스프라이트-top y(오버레이 하단 기준)
) {
    fun speed(style: WalkStyle, mood: Mood): Float {
        val mul = when (mood) {
            Mood.HAPPY -> 1.2f
            Mood.TIRED -> 0.7f
            Mood.CALM -> 1f
        }
        return walkSpeedDp(style) * dp1 * mul
    }

    val runSpeed: Float get() = 130f * dp1
}

/** 렌더 스펙: 행동·컨텍스트별 애니메이션/표정/기분 오버라이드(저장 안 함). */
private fun specFor(base: CharacterSpec, behavior: YardBehavior, ctx: YardContext): CharacterSpec = when (behavior) {
    YardBehavior.PAUSE, YardBehavior.GREET, YardBehavior.SIT -> base
    YardBehavior.WALK, YardBehavior.WALK_TO_PERCH -> base.copy(animation = CharacterAnimation.WALK)
    YardBehavior.HOP, YardBehavior.CELEBRATE -> base.copy(
        animation = CharacterAnimation.BOUNCE,
        mood = Mood.HAPPY,
        expression = Expression.EXCITED,
    )
    YardBehavior.BINKY -> base.copy(
        animation = CharacterAnimation.BOUNCE,
        mood = Mood.HAPPY,
        expression = Expression.EXCITED,
    )
    YardBehavior.SLEEP -> base.copy(
        animation = CharacterAnimation.SLEEP,
        mood = Mood.TIRED,
        expression = Expression.SLEEPY,
    )
    YardBehavior.REACT -> when (reactStyleFor(ctx.species, ctx.level)) {
        ReactStyle.COOL -> base.copy(expression = Expression.NEUTRAL)
        ReactStyle.STARTLE_DASH, ReactStyle.STARTLE_HIDE -> base.copy(expression = Expression.WORRIED)
        else -> base.copy(mood = Mood.HAPPY, expression = Expression.EXCITED)
    }
    YardBehavior.DRAG, YardBehavior.FALL, YardBehavior.DISMOUNT -> base.copy(expression = Expression.WORRIED)
    YardBehavior.PEEK -> base.copy(expression = Expression.NEUTRAL)
    YardBehavior.GROOM -> base.copy(animation = CharacterAnimation.GROOM, expression = Expression.NEUTRAL)
    YardBehavior.PECK -> base.copy(animation = CharacterAnimation.PECK)
    YardBehavior.EAT -> base.copy(animation = CharacterAnimation.EAT, expression = Expression.HAPPY, mood = Mood.HAPPY)
}

@Composable
fun RoamingYard(
    baseSpec: CharacterSpec,
    affection: Int,
    state: YardState,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
    perchRegistry: com.example.petling.ui.overlay.PerchRegistry? = null,
    modifier: Modifier = Modifier,
) {
    val renderer = LocalCharacterRenderer.current
    val density = LocalDensity.current
    val ctxProvider = rememberUpdatedState(YardContext(baseSpec.species, baseSpec.mood, affection))
    val scope = rememberCoroutineScope()

    val wobble by rememberInfiniteTransition(label = "wobble").animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(140, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobbleV",
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val dp1 = with(density) { 1.dp.toPx() }
        val spritePx = with(density) { SPRITE.toPx() }
        val bottomInsetPx = with(density) { bottomInset.toPx() }
        val rangePx = (with(density) { maxWidth.toPx() } - spritePx).coerceAtLeast(0f)
        // 지면선 = 오버레이 하단 − 네비바 inset − 스프라이트(발이 네비바 위에 서게)
        val groundTopY = (with(density) { maxHeight.toPx() } - bottomInsetPx - spritePx).coerceAtLeast(0f)
        val dims = YardDims(
            rangePx = rangePx,
            yMinPx = -groundTopY,
            hidePx = spritePx * 0.65f,
            jumpPx = 12f * dp1,
            dp1 = dp1,
            groundTopY = groundTopY,
        )
        state.spritePx = spritePx

        LaunchedEffect(rangePx, spritePx, groundTopY) {
            if (rangePx <= 0f) return@LaunchedEffect
            // PEEK/GREET가 가장자리 밖으로 나갈 수 있게 확장 bounds(로밍·드래그는 [0,range] 유지)
            state.x.updateBounds(-dims.hidePx, rangePx + dims.hidePx)
            if (state.x.value !in 0f..rangePx) state.x.snapTo(rangePx / 2f)
            state.baseY.snapTo(groundTopY) // 지면선에 발판 고정
            roamLoop(state, dims, ctxProvider, perchRegistry)
        }

        val ctx = ctxProvider.value
        val spec = specFor(baseSpec, state.behavior, ctx)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(SPRITE)
                .offset { IntOffset(state.x.value.roundToInt(), (state.baseY.value + state.y.value).roundToInt()) }
                .graphicsLayer {
                    scaleX = (if (state.facingLeft) -1f else 1f) * state.turn.value
                    rotationZ = (if (state.dragging) wobble * 8f else 0f) + state.rotation.value
                    val sq = state.squash.value
                    if (sq > 0f) {
                        scaleY = scaleY * (1f - sq * 0.12f)
                        scaleX = scaleX * (1f + sq * 0.10f)
                    }
                    transformOrigin = TransformOrigin(0.5f, 0.9f)
                }
                .pointerInput(state) {
                    detectTapGestures {
                        if (!state.dragging) state.events.trySend(YardEvent.Tap)
                    }
                }
                .pointerInput(state, rangePx, dims.yMinPx) {
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
                                state.y.snapTo((state.y.value + delta.y).coerceIn(dims.yMinPx, 0f))
                            }
                        },
                    )
                },
        ) {
            renderer.Render(spec, Modifier.fillMaxSize())
        }

        // 바닥 간식(도토리 쿠키) — 스프라이트와 형제라 반전 영향 없음
        state.snackX?.let { sx ->
            val drop = state.snackDrop.value
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(28.dp)
                    .offset {
                        val fall = (1f - drop) * 60f * dims.dp1
                        IntOffset((sx - 14f * dims.dp1).roundToInt(), (-8f * dims.dp1 - fall).roundToInt())
                    },
            ) {
                val r = size.minDimension / 2f
                drawCircle(Color(0xFFC98A4B), radius = r * 0.7f, center = Offset(r, r * 1.1f))
                drawCircle(Color(0xFF8A5A30), radius = r * 0.5f, center = Offset(r, r * 0.55f))
                listOf(-0.3f, 0.1f, 0.4f).forEach { t ->
                    drawCircle(Color(0xFF8A5A30), radius = r * 0.08f, center = Offset(r + t * r, r * 1.15f))
                }
            }
        }

        // 착지 먼지 오버레이(스프라이트 발판을 따라감)
        if (state.dust.value > 0f) {
            val d = state.dust.value
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(SPRITE)
                    .offset { IntOffset(state.x.value.roundToInt(), state.baseY.value.roundToInt()) },
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
    dims: YardDims,
    ctxProvider: State<YardContext>,
    registry: com.example.petling.ui.overlay.PerchRegistry?,
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
        val ctx = ctxProvider.value
        // 이벤트가 없는 idle 차례엔 확률적으로 근처 perch로 이동해 앉는다(성격 편향은 커밋3).
        val perchId = if (ev == null) choosePerch(registry, state, dims, lastBehavior) else null
        var chosen = YardBehavior.PAUSE
        val interrupt = raceEvents(state) {
            if (perchId != null) {
                chosen = YardBehavior.SIT
                executePerchVisit(state, dims, ctxProvider, perchId, registry!!)
            } else {
                val behavior = when (ev) {
                    is YardEvent.Drop -> YardBehavior.FALL
                    is YardEvent.Tap -> YardBehavior.REACT
                    is YardEvent.Celebrate -> YardBehavior.CELEBRATE
                    is YardEvent.Greet -> YardBehavior.GREET
                    is YardEvent.Snack -> YardBehavior.EAT
                    null -> weightedPick(idleWeights(ctx.species, lastBehavior, ctx.mood))
                }
                chosen = behavior
                setBehavior(state, behavior)
                try {
                    execute(state, behavior, ev, ctx, dims)
                } finally {
                    if (behavior == YardBehavior.EAT) state.snackX = null
                }
            }
        }
        // 전환 스쿼시 중 선점되면 눌린 채로 남지 않게 복원. perch에서 선점되면 바닥으로 복귀.
        if (state.turn.value != 1f) state.turn.snapTo(1f)
        if (state.rotation.value != 0f && chosen != YardBehavior.WALK) state.rotation.snapTo(0f)
        if (state.baseY.value != dims.groundTopY) state.baseY.snapTo(dims.groundTopY)
        if (state.y.value != 0f) state.y.snapTo(0f)
        pending = interrupt
        lastBehavior = chosen
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

// ─────────────────────────── perch(착지) ───────────────────────────

/** idle 차례에 근처 perch로 갈지 결정. 커밋2는 단순 확률; 성격 편향은 커밋3. */
private fun choosePerch(
    registry: com.example.petling.ui.overlay.PerchRegistry?,
    state: YardState,
    dims: YardDims,
    last: YardBehavior,
): String? {
    if (registry == null || last == YardBehavior.SIT) return null // 방금 앉았다 내려왔으면 잠시 배회
    val cand = registry.candidates().filter { inViewport(it.rect, dims) }
    if (cand.isEmpty() || Random.nextFloat() >= 0.35f) return null
    return cand.random().id
}

/** perch가 오버레이 콘텐츠 영역 안에 온전히 보이는지. */
private fun inViewport(rect: androidx.compose.ui.geometry.Rect, dims: YardDims): Boolean =
    rect.width > 0f && rect.top >= 0f && rect.top <= dims.groundTopY

/** perch 상단에 발을 맞춘 스프라이트-top y(root px). 발=박스 하단이 카드 상단에 살짝 얹히게. */
private fun perchBaseY(rect: androidx.compose.ui.geometry.Rect, spritePx: Float, dims: YardDims): Float =
    rect.top - spritePx + 8f * dims.dp1

/** perch 방문: 걸어가 → 등반 홉 → 착지 → 추적/체류 → 뛰어내림. */
private suspend fun executePerchVisit(
    state: YardState,
    dims: YardDims,
    ctxProvider: State<YardContext>,
    perchId: String,
    registry: com.example.petling.ui.overlay.PerchRegistry,
) {
    val ctx = ctxProvider.value
    val t = temperamentFor(ctx.species)
    // 1) 지면에서 perch 아래로 걸어가기
    val rect0 = registry.rectOf(perchId) ?: return
    setBehavior(state, YardBehavior.WALK_TO_PERCH)
    val targetX = (rect0.center.x - state.spritePx / 2f).coerceIn(0f, dims.rangePx)
    walkTo(state, targetX, dims.speed(t.walkStyle, ctx.mood), t.walkStyle, dims)
    // 2) 등반 홉 (걷는 사이 사라졌으면 취소)
    val rect = registry.rectOf(perchId) ?: return
    setBehavior(state, YardBehavior.SIT)
    coroutineScope {
        launch {
            state.y.animateTo(-dims.jumpPx * 1.2f, tween(180, easing = Motion.Decelerate))
            state.y.animateTo(0f, tween(160, easing = Motion.Bounce))
        }
        state.baseY.animateTo(perchBaseY(rect, state.spritePx, dims), tween(320, easing = Motion.Decelerate))
    }
    land(state)
    // 3) 스크롤 추적 + 체류
    followPerch(state, dims, registry, perchId)
    // 4) 바닥으로 뛰어내림
    dismount(state, dims)
}

/** perch를 따라 x/baseY를 갱신하며 체류. 소멸/화면이탈 시 조기 종료. */
private suspend fun followPerch(
    state: YardState,
    dims: YardDims,
    registry: com.example.petling.ui.overlay.PerchRegistry,
    perchId: String,
) {
    val start = System.currentTimeMillis()
    val dwellMs = 6000L // 성격 편향(체류 시간)은 커밋3
    while (coroutineContext.isActive) {
        val rect = registry.rectOf(perchId) ?: return
        if (!inViewport(rect, dims)) return
        state.x.snapTo(rect.center.x - state.spritePx / 2f)
        state.baseY.snapTo(perchBaseY(rect, state.spritePx, dims))
        if (System.currentTimeMillis() - start > dwellMs) return
        delay(32)
    }
}

/** perch에서 바닥으로 뛰어내려 배회 복귀. */
private suspend fun dismount(state: YardState, dims: YardDims) {
    setBehavior(state, YardBehavior.DISMOUNT)
    state.baseY.animateTo(dims.groundTopY, tween(340, easing = Motion.Decelerate))
    land(state)
}

// ─────────────────────────── 행동 실행 ───────────────────────────

private suspend fun execute(
    state: YardState,
    behavior: YardBehavior,
    ev: YardEvent?,
    ctx: YardContext,
    dims: YardDims,
) {
    val t = temperamentFor(ctx.species)
    when (behavior) {
        YardBehavior.PAUSE -> {
            val dur = Random.nextLong(t.pauseMs.first, t.pauseMs.last)
            if (Random.nextFloat() < 0.4f) {
                delay(dur / 2)
                state.facingLeft = !state.facingLeft
                delay(dur / 2)
            } else {
                delay(dur)
            }
        }
        YardBehavior.WALK -> walkTo(state, randomTarget(state.x.value, dims.rangePx), dims.speed(t.walkStyle, ctx.mood), t.walkStyle, dims)
        YardBehavior.HOP -> delay(Random.nextLong(700, 1200))
        YardBehavior.SLEEP -> {
            val edge = if (state.x.value < dims.rangePx / 2f) dims.rangePx * 0.12f else dims.rangePx * 0.88f
            setBehavior(state, YardBehavior.WALK)
            walkTo(state, edge, dims.speed(t.walkStyle, Mood.TIRED), t.walkStyle, dims)
            setBehavior(state, YardBehavior.SLEEP)
            delay(Random.nextLong(8000, 20000))
        }
        YardBehavior.REACT -> executeReact(state, ctx, dims)
        YardBehavior.FALL -> {
            val h = abs(state.y.value)
            val dur = (240 + h * 0.6f).toInt().coerceAtMost(460)
            state.y.animateTo(0f, tween(dur, easing = Motion.Bounce))
            land(state)
        }
        YardBehavior.CELEBRATE -> {
            val evolved = (ev as? YardEvent.Celebrate)?.evolved == true
            val laps = if (evolved) 2 else 1
            val start = state.x.value.coerceIn(0f, dims.rangePx)
            setBehavior(state, YardBehavior.WALK)
            repeat(laps) {
                walkTo(state, dims.rangePx, dims.runSpeed, t.walkStyle, dims)
                walkTo(state, 0f, dims.runSpeed, t.walkStyle, dims)
            }
            walkTo(state, start, dims.runSpeed, t.walkStyle, dims)
            setBehavior(state, YardBehavior.CELEBRATE)
            delay(900)
        }
        YardBehavior.GREET -> executeGreet(state, ctx, dims)
        YardBehavior.PEEK -> executePeek(state, ctx, dims, walkFirst = true)
        YardBehavior.GROOM -> {
            val dur = Random.nextLong(4000, 7000)
            if (Random.nextFloat() < 0.4f) {
                delay(dur / 2)
                state.facingLeft = !state.facingLeft
                delay(dur / 2)
            } else {
                delay(dur)
            }
        }
        YardBehavior.BINKY -> executeBinky(state, dims)
        YardBehavior.PECK -> {
            delay(Random.nextLong(2000, 4000))
            if (Random.nextFloat() < 0.5f) state.facingLeft = !state.facingLeft
        }
        YardBehavior.EAT -> executeEat(state, ev as? YardEvent.Snack, ctx, dims)
        YardBehavior.DRAG -> snapshotFlow { state.dragging }.first { !it }
        // perch 행동은 executePerchVisit에서 직접 구동 — 일반 execute 경로엔 오지 않는다.
        YardBehavior.WALK_TO_PERCH, YardBehavior.SIT, YardBehavior.DISMOUNT -> Unit
    }
}

/** 탭 반응 — 호감도 게이트. */
private suspend fun executeReact(state: YardState, ctx: YardContext, dims: YardDims) {
    when (reactStyleFor(ctx.species, ctx.level)) {
        ReactStyle.COOL -> {
            // 힐끗 보고 무시(도도)
            state.facingLeft = !state.facingLeft
            delay(500)
            state.facingLeft = !state.facingLeft
            delay(600)
        }
        ReactStyle.STARTLE_DASH -> {
            // 움찔 → 반대편으로 도망(토끼)
            state.y.animateTo(-dims.jumpPx * 0.6f, tween(120))
            state.y.animateTo(0f, tween(160, easing = Motion.Bounce))
            val target = if (state.x.value < dims.rangePx / 2f) dims.rangePx * 0.9f else dims.rangePx * 0.1f
            setBehavior(state, YardBehavior.WALK)
            walkTo(state, target, dims.runSpeed, temperamentFor(ctx.species).walkStyle, dims)
            setBehavior(state, YardBehavior.PAUSE)
        }
        ReactStyle.STARTLE_HIDE -> {
            // 움찔 → 가장자리로 도망가 빼꼼(여우)
            state.y.animateTo(-dims.jumpPx * 0.6f, tween(120))
            state.y.animateTo(0f, tween(160, easing = Motion.Bounce))
            executePeek(state, ctx, dims, walkFirst = true)
        }
        ReactStyle.NORMAL -> {
            state.y.animateTo(-dims.jumpPx, tween(220, easing = Motion.Decelerate))
            state.y.animateTo(0f, tween(300, easing = Motion.Bounce))
            delay(1100)
        }
        ReactStyle.LOVING -> {
            repeat(2) {
                state.y.animateTo(-dims.jumpPx, tween(200, easing = Motion.Decelerate))
                state.y.animateTo(0f, tween(260, easing = Motion.Bounce))
            }
            delay(1500)
        }
    }
}

/** 가장자리에 몸을 숨기고 얼굴만 내밀어 훔쳐보기(여우 습성). */
private suspend fun executePeek(state: YardState, ctx: YardContext, dims: YardDims, walkFirst: Boolean) {
    val leftEdge = state.x.value < dims.rangePx / 2f
    if (walkFirst) {
        setBehavior(state, YardBehavior.WALK)
        walkTo(state, if (leftEdge) 0f else dims.rangePx, dims.speed(WalkStyle.TROT, ctx.mood), WalkStyle.TROT, dims)
    }
    setBehavior(state, YardBehavior.PEEK)
    state.facingLeft = !leftEdge // 화면 안쪽을 바라봄
    val hidden = if (leftEdge) -dims.hidePx else dims.rangePx + dims.hidePx
    state.x.animateTo(hidden, tween(500))
    val watch = Random.nextLong(3000, 6000)
    if (Random.nextFloat() < 0.3f) {
        delay(watch / 2)
        // 한 번 쏙 완전히 숨었다가 다시 빼꼼
        val fullHide = if (leftEdge) -state.spritePx else dims.rangePx + state.spritePx
        state.x.animateTo(fullHide.coerceIn(-state.spritePx, dims.rangePx + state.spritePx), tween(250))
        delay(400)
        state.x.animateTo(hidden, tween(300))
        delay(watch / 2)
    } else {
        delay(watch)
    }
    state.x.animateTo(if (leftEdge) 0f else dims.rangePx, tween(400))
}

/** 신나는 지그재그 점프(토끼 빙키). */
private suspend fun executeBinky(state: YardState, dims: YardDims) {
    repeat(3) {
        val dx = (Random.nextFloat() * 80f - 40f) * dims.dp1
        val target = (state.x.value + dx).coerceIn(0f, dims.rangePx)
        coroutineScope {
            launch { state.x.animateTo(target, tween(320)) }
            state.y.animateTo(-dims.jumpPx * 1.6f, tween(160, easing = Motion.Decelerate))
            state.y.animateTo(0f, tween(200, easing = Motion.Bounce))
        }
        land(state)
    }
}

/** 주인이 앱에 들어왔을 때의 종별 인사. */
private suspend fun executeGreet(state: YardState, ctx: YardContext, dims: YardDims) {
    val t = temperamentFor(ctx.species)
    val center = dims.rangePx / 2f
    when (t.greetStyle) {
        GreetStyle.RUSH -> {
            // 후다닥 달려와 방방(앵김)
            setBehavior(state, YardBehavior.WALK)
            walkTo(state, center, dims.runSpeed, t.walkStyle, dims)
            setBehavior(state, YardBehavior.HOP)
            val hops = when {
                ctx.level >= AffectionLevel.BONDED -> 4
                ctx.level >= AffectionLevel.CLOSE -> 3
                else -> 2
            }
            repeat(hops) {
                state.y.animateTo(-dims.jumpPx, tween(180, easing = Motion.Decelerate))
                state.y.animateTo(0f, tween(220, easing = Motion.Bounce))
            }
            delay(600)
        }
        GreetStyle.ALOOF_OR_RUB -> {
            if (ctx.level >= AffectionLevel.CLOSE) {
                // 친해지면: 천천히 다가와 몸 비비기
                setBehavior(state, YardBehavior.WALK)
                walkTo(state, center, dims.speed(WalkStyle.STROLL, ctx.mood), WalkStyle.STROLL, dims)
                setBehavior(state, YardBehavior.REACT)
                repeat(3) {
                    state.x.animateTo(state.x.value + 6f * dims.dp1, tween(160))
                    state.x.animateTo(state.x.value - 6f * dims.dp1, tween(160))
                }
                delay(700)
            } else {
                // 도도: 힐끗 보고 그루밍
                state.facingLeft = state.x.value > dims.rangePx / 2f
                delay(800)
                setBehavior(state, YardBehavior.GROOM)
                delay(3000)
            }
        }
        GreetStyle.PEEK_IN -> {
            // 가장자리 밖에서 빼꼼 등장(수줍음)
            val leftEdge = state.x.value < dims.rangePx / 2f
            state.x.snapTo(if (leftEdge) -dims.hidePx else dims.rangePx + dims.hidePx)
            state.behavior = YardBehavior.PEEK
            state.facingLeft = !leftEdge
            delay(300)
            val peekAt = if (leftEdge) -dims.hidePx * 0.5f else dims.rangePx + dims.hidePx * 0.5f
            state.x.animateTo(peekAt, tween(600))
            delay(2500)
            if (ctx.level == AffectionLevel.STRANGER) {
                state.x.animateTo(if (leftEdge) -dims.hidePx else dims.rangePx + dims.hidePx, tween(300))
                delay(500)
                state.x.animateTo(peekAt, tween(400))
                delay(1000)
            }
            setBehavior(state, YardBehavior.WALK)
            walkTo(state, dims.rangePx * if (leftEdge) 0.3f else 0.7f, dims.speed(WalkStyle.TROT, ctx.mood), WalkStyle.TROT, dims)
            setBehavior(state, YardBehavior.PAUSE)
        }
        GreetStyle.BINKY_IN -> executeBinky(state, dims)
        GreetStyle.ROLL_IN, GreetStyle.WADDLE_IN, GreetStyle.SCURRY_IN -> {
            setBehavior(state, YardBehavior.WALK)
            walkTo(state, center, dims.speed(t.walkStyle, ctx.mood), t.walkStyle, dims)
            setBehavior(state, YardBehavior.HOP)
            state.y.animateTo(-dims.jumpPx, tween(200, easing = Motion.Decelerate))
            state.y.animateTo(0f, tween(260, easing = Motion.Bounce))
            land(state)
            delay(500)
        }
        GreetStyle.SLOW_APPROACH -> {
            // 느긋: 천천히 걸어와 자리 잡기(판다)
            setBehavior(state, YardBehavior.WALK)
            walkTo(state, center, dims.speed(WalkStyle.STROLL, Mood.TIRED), WalkStyle.STROLL, dims)
            setBehavior(state, YardBehavior.PAUSE)
            delay(1200)
        }
    }
}

/** 간식: 떨어지면 걸어가서 냠냠, 시크한 고양이는 먹고 딴청. */
private suspend fun executeEat(state: YardState, snack: YardEvent.Snack?, ctx: YardContext, dims: YardDims) {
    val frac = snack?.xFraction ?: 0.5f
    val snackCenter = (frac * dims.rangePx + state.spritePx / 2f).coerceIn(state.spritePx * 0.3f, dims.rangePx + state.spritePx * 0.7f)
    state.snackX = snackCenter
    state.snackDrop.snapTo(0f)
    state.snackDrop.animateTo(1f, tween(400, easing = Motion.Bounce))

    // 간식 옆으로 이동(도도한 고양이는 저호감이면 일부러 느긋하게)
    val t = temperamentFor(ctx.species)
    val aloof = ctx.species == Species.CAT && ctx.level < AffectionLevel.CLOSE
    val speed = if (aloof) dims.speed(WalkStyle.STROLL, Mood.TIRED) else dims.speed(t.walkStyle, Mood.HAPPY)
    if (aloof) delay(700) // 못 이기는 척 뜸 들이기
    setBehavior(state, YardBehavior.WALK)
    walkTo(state, (snackCenter - state.spritePx * 0.5f).coerceIn(0f, dims.rangePx), speed, t.walkStyle, dims)

    setBehavior(state, YardBehavior.EAT)
    delay(1500)
    state.snackX = null // 먹어서 사라짐
    delay(300)

    if (aloof) {
        // 새침: 먹고 딴청
        setBehavior(state, YardBehavior.PAUSE)
        state.facingLeft = !state.facingLeft
        delay(800)
    } else {
        setBehavior(state, YardBehavior.HOP)
        delay(800)
    }
}

private fun randomTarget(current: Float, rangePx: Float): Float {
    if (rangePx < 1f) return current
    val minStep = maxOf(rangePx * 0.15f, 1f)
    repeat(6) {
        val t = Random.nextFloat() * rangePx
        if (abs(t - current) >= minStep) return t.coerceIn(0f, rangePx)
    }
    return ((current + rangePx / 2f) % rangePx).coerceIn(0f, rangePx)
}

private suspend fun walkTo(
    state: YardState,
    target: Float,
    speedPxPerSec: Float,
    style: WalkStyle,
    dims: YardDims,
) {
    val dist = abs(target - state.x.value)
    if (dist < 1f) return
    val newFacing = target < state.x.value
    if (newFacing != state.facingLeft) {
        // 걷는 중 방향 반전도 몸돌리기 스쿼시로
        state.turn.animateTo(0.3f, tween(70, easing = LinearEasing))
        state.facingLeft = newFacing
        state.turn.animateTo(1f, tween(70, easing = LinearEasing))
    }
    val dur = (dist / speedPxPerSec * 1000f).toInt().coerceIn(120, 8000)
    if (style == WalkStyle.ROLL) {
        // 데굴데굴: 이동 거리만큼 회전(몸폭당 360°), 방향 부호 반영
        val dir = if (newFacing) -1f else 1f
        val degrees = dist / state.spritePx.coerceAtLeast(1f) * 360f * dir
        coroutineScope {
            launch { state.rotation.animateTo(state.rotation.value + degrees, tween(dur, easing = LinearEasing)) }
            state.x.animateTo(target, tween(dur, easing = LinearEasing))
        }
        state.rotation.snapTo(0f)
    } else {
        state.x.animateTo(target, tween(dur, easing = LinearEasing))
    }
}

private suspend fun land(state: YardState) {
    state.squash.snapTo(1f)
    state.dust.snapTo(1f)
    coroutineScope {
        launch { state.squash.animateTo(0f, tween(260, easing = Motion.Bounce)) }
        launch { state.dust.animateTo(0f, tween(400, easing = LinearEasing)) }
    }
}
