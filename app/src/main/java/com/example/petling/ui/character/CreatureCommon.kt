package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill

/**
 * 종별 드로잉이 공유하는 빌딩 블록(포유류 몸통/발/삼각형/그라디언트).
 *
 * 성능: 매 프레임 그려지므로 Path는 파일-프라이빗 [scratch] 하나를 reset()해 재사용한다.
 * Compose 드로우는 UI 스레드에서 단일 실행되므로 공유 스크래치가 안전하다.
 */

private val scratch = Path()

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

internal fun DrawScope.triangle(a: Offset, b: Offset, c: Offset, color: Color) {
    scratch.reset()
    scratch.moveTo(a.x, a.y); scratch.lineTo(b.x, b.y); scratch.lineTo(c.x, c.y); scratch.close()
    drawPath(scratch, color, style = Fill)
}

/** 머리+몸 2덩어리 + 배 패치 + 볼륨 셰이딩. 종별 몸통의 토대. */
internal fun DrawScope.drawMammalBody(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    palette: ModoriPalette, prop: Proportions, motion: CreatureMotion,
) {
    val cx = 0.5f
    val bry = prop.bodyRy * (1f + motion.breathe * 0.012f)

    // 몸통
    fillOval(p, d, cx, prop.bodyCy, prop.bodyRx, bry, vGrad(p, cx, prop.bodyCy, bry, palette.bodyHighlight, palette.body))
    // 하단 그림자 초승달
    drawOval(
        palette.bodyShadow.copy(alpha = 0.38f),
        topLeft = p(cx - prop.bodyRx * 0.92f, prop.bodyCy + bry * 0.12f),
        size = Size(d(prop.bodyRx * 1.84f), d(bry * 0.88f)),
    )
    // 가슴·배 크림 패치
    drawOval(
        palette.belly.copy(alpha = 0.92f),
        topLeft = p(cx - prop.bodyRx * 0.44f, prop.bodyCy - bry * 0.18f),
        size = Size(d(prop.bodyRx * 0.88f), d(bry * 1.12f)),
    )

    // 머리
    fillOval(p, d, cx, prop.headCy, prop.headR, prop.headR, vGrad(p, cx, prop.headCy, prop.headR, palette.bodyHighlight, palette.body))
    // 머리 상단 하이라이트
    drawOval(
        palette.bodyHighlight.copy(alpha = 0.5f),
        topLeft = p(cx - prop.headR * 0.5f, prop.headCy - prop.headR * 0.72f),
        size = Size(d(prop.headR * 0.7f), d(prop.headR * 0.45f)),
    )
}

/** 앞발 두 개(limb>0일 때). FULL LOD면 발가락 선. */
internal fun DrawScope.drawPaws(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    palette: ModoriPalette, prop: Proportions, lod: Lod,
) {
    if (prop.limb <= 0f) return
    val cy = prop.bodyCy + prop.bodyRy * 0.78f
    val dx = prop.bodyRx * 0.5f
    val r = prop.limb
    listOf(-dx, dx).forEach { off ->
        val ccx = 0.5f + off
        drawOval(palette.bodyShadow, topLeft = p(ccx - r, cy - r * 0.75f), size = Size(d(r * 2), d(r * 1.5f)))
        if (lod == Lod.FULL) {
            listOf(-0.35f, 0.35f).forEach { t ->
                drawLine(
                    palette.marking.copy(alpha = 0.4f),
                    p(ccx + r * t, cy + r * 0.1f), p(ccx + r * t, cy + r * 0.6f),
                    strokeWidth = d(0.006f),
                )
            }
        }
    }
}

/** 삼각 귀 하나(끝 색/속귀 색). pose로 처짐·젖힘·쫑긋을 반영. */
internal fun DrawScope.drawTriEar(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    baseCx: Float, baseCy: Float, len: Float, w: Float,
    outer: Color, inner: Color, tipColor: Color?,
    outward: Float, // 바깥 방향(-1 왼귀, +1 오른귀)
    droop: Float, perk: Float, twitch: Float,
) {
    // 처짐·쫑긋에 따라 귀끝을 안/밖·상/하로 이동
    val lift = (perk - 1f) * 0.4f + twitch * 0.15f
    val tipX = baseCx + outward * (w * 0.6f + droop * len * 0.9f)
    val tipY = baseCy - len * (1f - droop * 0.85f) - len * lift
    val b1 = p(baseCx - w * 0.5f, baseCy + w * 0.2f)
    val b2 = p(baseCx + w * 0.5f, baseCy + w * 0.2f)
    val tip = p(tipX, tipY)
    triangle(b1, b2, tip, outer)
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
 * 꼬리 — 곡선을 따라 원을 겹쳐 그린다(메타볼). tailWag/tailLift 반영은 호출부에서 회전으로 처리.
 * [bushy]면 가운데가 부푼 풍성한 플룸(여우), 아니면 균일한 가는 꼬리(고양이).
 */
internal fun DrawScope.drawFluffyTail(
    p: (Float, Float) -> Offset, d: (Float) -> Float,
    rootX: Float, rootY: Float, len: Float, thick: Float,
    color: Color, tipColor: Color?, dir: Float, bushy: Boolean,
) {
    val n = 12
    val p0x = rootX; val p0y = rootY
    val p1x = rootX + dir * len * 0.62f; val p1y = rootY - len * 0.35f  // 옆으로 났다가
    val p2x = rootX + dir * len * 0.20f; val p2y = rootY - len * 1.05f  // 위로 말려 올라감
    for (i in 0..n) {
        val t = i / n.toFloat()
        val mt = 1f - t
        val x = mt * mt * p0x + 2 * mt * t * p1x + t * t * p2x
        val y = mt * mt * p0y + 2 * mt * t * p1y + t * t * p2y
        val prof = if (bushy) 0.55f + 0.75f * kotlin.math.sin((t * Math.PI).toFloat()) else 0.9f + 0.1f * (1f - t)
        val col = if (tipColor != null && t > 0.78f) tipColor else color
        drawCircle(col, radius = d(thick * prof), center = p(x, y))
    }
}

/** 스크래치 Path 접근(같은 파일 밖 이펙트/알 드로잉에서 재사용). reset 후 넘긴다. */
internal fun scratchPath(): Path {
    scratch.reset()
    return scratch
}
