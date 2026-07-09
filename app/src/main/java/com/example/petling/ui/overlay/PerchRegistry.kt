package com.example.petling.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.abs

/**
 * 캐릭터가 올라앉을 수 있는 화면 요소(perch)의 전역 레지스트리.
 *
 * 좌표는 root 기준([androidx.compose.ui.layout.LayoutCoordinates.boundsInRoot]). 오버레이가
 * 같은 root 좌표계에 스프라이트를 배치하므로 변환 없이 정렬된다.
 *
 * **Recomposition 격리**: 화면(writer)은 [Modifier.perch]로 map에 쓰기만 하고 읽지 않는다 →
 * 카드/화면은 recompose되지 않는다. 오버레이(reader)는 map을 composition에서 읽지 않고
 * 로밍 코루틴에서만 [rectOf]/[candidates]로 읽는다(스냅샷 읽기 → recomposition 미유발).
 */
@Stable
class PerchRegistry {
    private val map = mutableStateMapOf<String, PerchInfo>()

    /** 스크롤/레이아웃마다 호출됨. EPS 미만 변화는 write를 스킵해 불필요한 스냅샷 무효화를 막는다. */
    fun report(id: String, rect: Rect, weight: Float) {
        val cur = map[id]
        if (cur == null || !cur.rect.closeTo(rect) || cur.weight != weight) {
            map[id] = PerchInfo(id, rect, weight)
        }
    }

    fun remove(id: String) {
        map.remove(id)
    }

    fun rectOf(id: String): Rect? = map[id]?.rect

    fun candidates(): List<PerchInfo> = map.values.toList()
}

data class PerchInfo(val id: String, val rect: Rect, val weight: Float)

private const val EPS = 0.5f

private fun Rect.closeTo(o: Rect): Boolean =
    abs(left - o.left) < EPS && abs(top - o.top) < EPS &&
        abs(right - o.right) < EPS && abs(bottom - o.bottom) < EPS

/** 미제공 시 null — [Modifier.perch]는 no-op(프리뷰·오버레이 없는 화면 안전). */
val LocalPerchRegistry = staticCompositionLocalOf<PerchRegistry?> { null }

/**
 * "여기 캐릭터가 앉을 수 있다"를 선언. 명시적으로 붙인 곳에만 perch가 생긴다(자동 탐지 없음).
 * [weight]는 선호도(예: 마감 임박도) — 오버레이가 도메인 무지로 이 값만 소비한다.
 */
@Composable
fun Modifier.perch(id: String, weight: Float = 0f): Modifier {
    val reg = LocalPerchRegistry.current ?: return this
    DisposableEffect(id, reg) { onDispose { reg.remove(id) } }
    return this.onGloballyPositioned { reg.report(id, it.boundsInRoot(), weight) }
}
