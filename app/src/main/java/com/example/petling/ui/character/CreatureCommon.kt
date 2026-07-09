package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * 종별 드로잉이 공유하는 빌딩 블록.
 *
 * 리디자인(포켓몬식) 스타일 규칙:
 * - 주 실루엣(몸·머리·귀·꼬리·발)은 웜 다크 아웃라인([ModoriPalette.outline])으로 감싼다.
 * - 마킹(배 패치·볼·무늬)은 아웃라인 없이 면으로만 얹는다.
 * - 실루엣은 종별 전용 — 공용 몸통은 앉은 자세([drawSittingBody])와 물방울([drawEggBody]) 두 가지 뼈대만 제공한다.
 *
 * 성능: 매 프레임 그려지므로 Path는 파일-프라이빗 [scratch] 하나를 reset()해 재사용한다.
 * Compose 드로우는 UI 스레드에서 단일 실행되므로 공유 스크래치가 안전하다.
 */

private val scratch = Path()

/** 주 실루엣 아웃라인 두께(정규화). 소형 디테일은 [OUT_W_S]. */
internal const val OUT_W = 0.011f
internal const val OUT_W_S = 0.007f

internal fun outlineStroke(d: (Float) -> Float, w: Float = OUT_W): Stroke =
    Stroke(width = d(w), join = StrokeJoin.Round, cap = StrokeCap.Round)

/** 세로 그라디언트(위=밝게 → 아래=본색)로 타원 볼륨을 낸다. */
internal fun DrawScope.vGrad(
    p: (Float, Float) -> Offset,
    cx: Float, cy: Float, ry: Float,
    top: Color, bottom: Color,
): Brush = Brush.verticalGradient(
    colors = listOf(top, bottom),
    startY = p(cx, cy - ry).y,
    endY = p(cx, cy + ry).y,
)

internal fun DrawScope.fillOval(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    cx: Float, cy: Float, rx: Float, ry: Float, brush: Brush,
) = drawOval(brush, topLeft = p(cx - rx, cy - ry), size = Size(d(rx * 2), d(ry * 2)))

internal fun DrawScope.fillOval(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    cx: Float, cy: Float, rx: Float, ry: Float, color: Color,
) = drawOval(color, topLeft = p(cx - rx, cy - ry), size = Size(d(rx * 2), d(ry * 2)))

/** 아웃라인 타원: 면 + 스트로크. */
internal fun DrawScope.outlinedOval(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    cx: Float, cy: Float, rx: Float, ry: Float,
    brush: Brush, outline: Color, ow: Float = OUT_W,
) {
    fillOval(p, d, cx, cy, rx, ry, brush)
    drawOval(outline, topLeft = p(cx - rx, cy - ry), size = Size(d(rx * 2), d(ry * 2)), style = outlineStroke(d, ow))
}

internal fun DrawScope.outlinedOval(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    cx: Float, cy: Float, rx: Float, ry: Float,
    color: Color, outline: Color, ow: Float = OUT_W,
) {
    fillOval(p, d, cx, cy, rx, ry, color)
    drawOval(outline, topLeft = p(cx - rx, cy - ry), size = Size(d(rx * 2), d(ry * 2)), style = outlineStroke(d, ow))
}

internal fun DrawScope.outlinedCircle(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    cx: Float, cy: Float, r: Float,
    color: Color, outline: Color, ow: Float = OUT_W,
) {
    drawCircle(color, radius = d(r), center = p(cx, cy))
    drawCircle(outline, radius = d(r), center = p(cx, cy), style = outlineStroke(d, ow))
}

/** 아웃라인 Path: 면 + 스트로크(라운드 조인). */
internal fun DrawScope.outlinedPath(
    d: (Float) -> Float, path: Path, brush: Brush, outline: Color, ow: Float = OUT_W,
) {
    drawPath(path, brush, style = Fill)
    drawPath(path, outline, style = outlineStroke(d, ow))
}

internal fun DrawScope.outlinedPath(
    d: (Float) -> Float, path: Path, color: Color, outline: Color, ow: Float = OUT_W,
) {
    drawPath(path, color, style = Fill)
    drawPath(path, outline, style = outlineStroke(d, ow))
}

internal fun DrawScope.triangle(
    a: Offset, b: Offset, c: Offset, color: Color,
    outline: Color? = null, d: ((Float) -> Float)? = null, ow: Float = OUT_W_S,
) {
    scratch.reset()
    scratch.moveTo(a.x, a.y); scratch.lineTo(b.x, b.y); scratch.lineTo(c.x, c.y); scratch.close()
    drawPath(scratch, color, style = Fill)
    if (outline != null && d != null) drawPath(scratch, outline, style = outlineStroke(d, ow))
}

// ─────────────────────────── 몸통 뼈대 ───────────────────────────

/**
 * 앉은 자세 몸통(여우·고양이·강아지): 목에서 좁고 엉덩이로 퍼지는 서양배 실루엣.
 * 아웃라인 + 세로 그라디언트 + 가슴 크림 패치. 발은 [drawSitPaws]로 따로 얹는다.
 */
internal fun DrawScope.drawSittingBody(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    palette: ModoriPalette, prop: Proportions, motion: CreatureMotion,
    widthMul: Float = 1f,
) {
    val cx = 0.5f
    val breathe = 1f + motion.breathe * 0.012f
    val rx = prop.bodyRx * widthMul
    val top = prop.bodyCy - prop.bodyRy * breathe
    val bot = prop.bodyCy + prop.bodyRy * breathe
    val h = bot - top

    val path = scratchPath()
    path.moveTo(p(cx - rx, bot).x, p(cx - rx, bot).y)
    path.cubicTo(
        p(cx - rx * 1.16f, top + h * 0.36f).x, p(cx - rx * 1.16f, top + h * 0.36f).y,
        p(cx - rx * 0.70f, top).x, p(cx - rx * 0.70f, top).y,
        p(cx, top).x, p(cx, top).y,
    )
    path.cubicTo(
        p(cx + rx * 0.70f, top).x, p(cx + rx * 0.70f, top).y,
        p(cx + rx * 1.16f, top + h * 0.36f).x, p(cx + rx * 1.16f, top + h * 0.36f).y,
        p(cx + rx, bot).x, p(cx + rx, bot).y,
    )
    // 둥근 바닥
    path.quadraticBezierTo(p(cx, bot + h * 0.10f).x, p(cx, bot + h * 0.10f).y, p(cx - rx, bot).x, p(cx - rx, bot).y)
    path.close()
    outlinedPath(d, path, vGrad(p, cx, prop.bodyCy, prop.bodyRy, palette.bodyHighlight, palette.body), palette.outline)

    // 가슴·배 크림 패치(면 마킹)
    drawOval(
        palette.belly.copy(alpha = 0.95f),
        topLeft = p(cx - rx * 0.50f, top + h * 0.28f),
        size = Size(d(rx * 1.00f), d(h * 0.72f)),
    )
}

/** 앉은 자세 앞발 두 개(아웃라인 타원). [color]로 삭스(여우) 표현. */
internal fun DrawScope.drawSitPaws(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    palette: ModoriPalette, prop: Proportions, color: Color,
) {
    if (prop.limb <= 0f) return
    val cy = prop.bodyCy + prop.bodyRy * 0.88f
    val dx = prop.bodyRx * 0.47f
    val r = prop.limb
    listOf(-dx, dx).forEach { off ->
        outlinedOval(p, d, 0.5f + off, cy, r * 1.25f, r * 0.85f, color, palette.outline, OUT_W_S)
    }
}

/**
 * 물방울(에그) 몸통(병아리·펭귄·햄스터): 머리·몸 구분 없는 한 덩어리.
 * 가장 넓은 지점이 중간보다 아래(무게중심 낮음). 아웃라인 포함.
 */
internal fun DrawScope.drawEggBody(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    top: Float, bot: Float, halfW: Float,
    brush: Brush, outline: Color,
) {
    val cx = 0.5f
    val path = scratchPath()
    path.moveTo(p(cx, top).x, p(cx, top).y)
    path.cubicTo(
        p(cx + halfW * 0.70f, top).x, p(cx + halfW * 0.70f, top).y,
        p(cx + halfW * 1.04f, lerpF(top, bot, 0.28f)).x, p(cx + halfW * 1.04f, lerpF(top, bot, 0.28f)).y,
        p(cx + halfW, lerpF(top, bot, 0.58f)).x, p(cx + halfW, lerpF(top, bot, 0.58f)).y,
    )
    path.cubicTo(
        p(cx + halfW * 0.96f, lerpF(top, bot, 0.86f)).x, p(cx + halfW * 0.96f, lerpF(top, bot, 0.86f)).y,
        p(cx + halfW * 0.56f, bot).x, p(cx + halfW * 0.56f, bot).y,
        p(cx, bot).x, p(cx, bot).y,
    )
    path.cubicTo(
        p(cx - halfW * 0.56f, bot).x, p(cx - halfW * 0.56f, bot).y,
        p(cx - halfW * 0.96f, lerpF(top, bot, 0.86f)).x, p(cx - halfW * 0.96f, lerpF(top, bot, 0.86f)).y,
        p(cx - halfW, lerpF(top, bot, 0.58f)).x, p(cx - halfW, lerpF(top, bot, 0.58f)).y,
    )
    path.cubicTo(
        p(cx - halfW * 1.04f, lerpF(top, bot, 0.28f)).x, p(cx - halfW * 1.04f, lerpF(top, bot, 0.28f)).y,
        p(cx - halfW * 0.70f, top).x, p(cx - halfW * 0.70f, top).y,
        p(cx, top).x, p(cx, top).y,
    )
    path.close()
    outlinedPath(d, path, brush, outline)
}

private fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

// ─────────────────────────── 체형 아키타입 뼈대 ───────────────────────────

/**
 * 아래로 무게중심이 쏠린 블롭 실루엣 경로(scratchPath 재사용).
 * [wideBiasDown]=최광폭이 중앙에서 바닥 쪽으로 쏠린 정도(0..1),
 * [topNarrow]=상단 폭 비율(작을수록 어깨가 좁아 뾰족).
 */
private fun blobPath(
    p: (Float, Float) -> Offset,
    cx: Float, top: Float, bot: Float, halfW: Float,
    wideBiasDown: Float, topNarrow: Float,
): Path {
    val h = bot - top
    val wy = top + h * (0.5f + 0.5f * wideBiasDown) // 최광폭 y(바닥 쪽 쏠림)
    val tw = halfW * topNarrow
    val lx = cx - halfW
    val rx = cx + halfW
    val path = scratchPath()
    path.moveTo(p(lx, wy).x, p(lx, wy).y)
    path.cubicTo(
        p(lx, wy - (wy - top) * 0.72f).x, p(lx, wy - (wy - top) * 0.72f).y,
        p(cx - tw, top).x, p(cx - tw, top).y,
        p(cx, top).x, p(cx, top).y,
    )
    path.cubicTo(
        p(cx + tw, top).x, p(cx + tw, top).y,
        p(rx, wy - (wy - top) * 0.72f).x, p(rx, wy - (wy - top) * 0.72f).y,
        p(rx, wy).x, p(rx, wy).y,
    )
    path.cubicTo(
        p(rx, wy + (bot - wy) * 0.62f).x, p(rx, wy + (bot - wy) * 0.62f).y,
        p(cx + halfW * 0.48f, bot).x, p(cx + halfW * 0.48f, bot).y,
        p(cx, bot).x, p(cx, bot).y,
    )
    path.cubicTo(
        p(cx - halfW * 0.48f, bot).x, p(cx - halfW * 0.48f, bot).y,
        p(lx, wy + (bot - wy) * 0.62f).x, p(lx, wy + (bot - wy) * 0.62f).y,
        p(lx, wy).x, p(lx, wy).y,
    )
    path.close()
    return path
}

/**
 * 웅크린 공 몸통(햄스터): 가로가 세로보다 넓은 땅딸보, 최광폭이 하단 1/3.
 * [cheekBulge]는 개체 변이(4단계)에서 볼·몸통 부피를 미세 조정하는 주입 지점.
 */
internal fun DrawScope.drawCrouchedBallBody(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    top: Float, bot: Float, halfW: Float,
    brush: Brush, outline: Color, cheekBulge: Float = 1f,
) {
    val path = blobPath(p, 0.5f, top, bot, halfW * cheekBulge, wideBiasDown = 0.30f, topNarrow = 0.62f)
    outlinedPath(d, path, brush, outline)
}

/**
 * 엉덩이 뭉치 몸통(토끼): 작은 상체 + 옆으로 불룩한 큰 힙(하단이 넓고 어깨가 좁다).
 * [haunchMul]은 개체 변이에서 힙 부피 조정용.
 */
internal fun DrawScope.drawHaunchBody(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    top: Float, bot: Float, halfW: Float,
    brush: Brush, outline: Color, haunchMul: Float = 1f,
) {
    val path = blobPath(p, 0.5f, top, bot, halfW * haunchMul, wideBiasDown = 0.42f, topNarrow = 0.46f)
    outlinedPath(d, path, brush, outline)
}

/**
 * 묵직한 반원 몸통(판다): 넓고 살짝 둥근 바닥 + 큰 상단 아치.
 * 어깨·엉덩이 구분 없는 "산 모양" — 가장 부피감 있는 실루엣.
 */
internal fun DrawScope.drawDomeBody(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    top: Float, bot: Float, halfW: Float,
    brush: Brush, outline: Color,
) {
    val cx = 0.5f
    val h = bot - top
    val path = scratchPath()
    val bl = p(cx - halfW, bot)
    path.moveTo(bl.x, bl.y)
    path.cubicTo(
        p(cx - halfW, top + h * 0.28f).x, p(cx - halfW, top + h * 0.28f).y,
        p(cx - halfW * 0.46f, top).x, p(cx - halfW * 0.46f, top).y,
        p(cx, top).x, p(cx, top).y,
    )
    path.cubicTo(
        p(cx + halfW * 0.46f, top).x, p(cx + halfW * 0.46f, top).y,
        p(cx + halfW, top + h * 0.28f).x, p(cx + halfW, top + h * 0.28f).y,
        p(cx + halfW, bot).x, p(cx + halfW, bot).y,
    )
    // 살짝 둥근 바닥
    path.cubicTo(
        p(cx + halfW * 0.5f, bot + h * 0.06f).x, p(cx + halfW * 0.5f, bot + h * 0.06f).y,
        p(cx - halfW * 0.5f, bot + h * 0.06f).x, p(cx - halfW * 0.5f, bot + h * 0.06f).y,
        bl.x, bl.y,
    )
    path.close()
    outlinedPath(d, path, brush, outline)
}

// ─────────────────────────── 귀·꼬리 ───────────────────────────

/** 삼각 귀 하나(끝 색/속귀 색 + 아웃라인). pose로 처짐·젖힘·쫑긋을 반영. */
internal fun DrawScope.drawTriEar(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    baseCx: Float, baseCy: Float, len: Float, w: Float,
    outer: Color, inner: Color, tipColor: Color?,
    outward: Float, // 바깥 방향(-1 왼귀, +1 오른귀)
    droop: Float, perk: Float, twitch: Float,
    outline: Color? = null,
) {
    // 처짐·쫑긋에 따라 귀끝을 안/밖·상/하로 이동
    val lift = (perk - 1f) * 0.4f + twitch * 0.15f
    val tipX = baseCx + outward * (w * 0.6f + droop * len * 0.9f)
    val tipY = baseCy - len * (1f - droop * 0.85f) - len * lift
    val b1 = p(baseCx - w * 0.5f, baseCy + w * 0.2f)
    val b2 = p(baseCx + w * 0.5f, baseCy + w * 0.2f)
    val tip = p(tipX, tipY)
    triangle(b1, b2, tip, outer, outline, d, OUT_W_S)
    // 속귀
    val ib1 = p(baseCx - w * 0.28f, baseCy + w * 0.05f)
    val ib2 = p(baseCx + w * 0.28f, baseCy + w * 0.05f)
    val itip = p(tipX * 0.72f + baseCx * 0.28f, tipY * 0.7f + baseCy * 0.3f)
    triangle(ib1, ib2, itip, inner)
    // 귀끝 마킹(여우 등)
    tipColor?.let {
        val tb1 = p(tipX + (baseCx - tipX) * 0.4f - w * 0.12f, tipY + (baseCy - tipY) * 0.4f)
        val tb2 = p(tipX + (baseCx - tipX) * 0.4f + w * 0.12f, tipY + (baseCy - tipY) * 0.4f)
        triangle(tb1, tb2, tip, it)
    }
}

/**
 * 플룸 꼬리(여우): 아웃라인 타원 한 덩어리 + 밝은 끝. 몸 옆에서 위로 뻗는다.
 * [angleDeg]로 살랑(호출부에서 wag 회전 계산).
 */
internal fun DrawScope.drawPlumeTail(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    rootX: Float, rootY: Float, len: Float, thick: Float,
    color: Color, tipColor: Color, outline: Color,
    angleDeg: Float,
) {
    rotate(angleDeg, pivot = p(rootX, rootY)) {
        val cy = rootY - len * 0.52f
        val rx = thick * 1.45f
        val ry = len * 0.62f
        fillOval(p, d, rootX, cy, rx, ry, color)
        // 밝은 끝(면 마킹) — 타원 위쪽에 얹힘
        drawCircle(tipColor, radius = d(rx * 0.78f), center = p(rootX, cy - ry * 0.68f))
        drawOval(outline, topLeft = p(rootX - rx, cy - ry), size = Size(d(rx * 2), d(ry * 2)), style = outlineStroke(d))
    }
}

/**
 * 스트로크 꼬리(고양이·강아지 말림): 곡선 Path를 아웃라인(굵게)→본색(가늘게) 두 번 그린다.
 * [build]가 scratch Path에 곡선을 채운다.
 */
internal fun DrawScope.drawStrokeTail(
    d: (Float) -> Float,
    thick: Float,
    color: Color, outline: Color,
    build: (Path) -> Unit,
) {
    val path = scratchPath()
    build(path)
    drawPath(path, outline, style = Stroke(d(thick) + d(OUT_W) * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color, style = Stroke(d(thick), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** 스크래치 Path 접근(같은 파일 밖 이펙트/알 드로잉에서 재사용). reset 후 넘긴다. */
internal fun scratchPath(): Path {
    scratch.reset()
    return scratch
}
