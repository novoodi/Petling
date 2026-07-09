package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * 토끼 "겁 많은 스프린터".
 * 실루엣: 머리만 한 긴 귀 + 앞으로 뻗은 큰 뒷발 / 컬러: 크림 화이트+핑크 속귀 /
 * 시그니처: 한쪽만 꺾인 귀 + 앞니.
 */
internal fun DrawScope.drawRabbit(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
) {
    val cx = 0.5f
    val hr = prop.headR * 1.06f
    val hc = prop.headCy

    // 폼폼 꼬리(뒤)
    outlinedCircle(p, d, cx + prop.bodyRx * 0.88f, prop.bodyCy + prop.bodyRy * 0.28f, prop.tailThick * 1.1f, palette.bodyHighlight, palette.outline, OUT_W_S)

    // 긴 귀(뒤): 왼쪽 곧게, 오른쪽은 꺾임(시그니처). 아기는 짧고 한쪽이 접힘.
    val baby = prop.earFold > 0.05f
    drawRabbitEar(p, d, palette, prop, pose, motion, sideSign = -1f, bendDeg = if (baby) -44f else -7f, lenMul = if (baby) 0.78f else 1f)
    drawRabbitEar(p, d, palette, prop, pose, motion, sideSign = 1f, bendDeg = if (baby) 10f else 40f, lenMul = if (baby) 0.88f else 0.94f)

    // 둥근 몸통(볼 형태) + 흰 배
    outlinedOval(p, d, cx, prop.bodyCy, prop.bodyRx * 1.02f, prop.bodyRy * 0.94f, vGrad(p, cx, prop.bodyCy, prop.bodyRy, palette.bodyHighlight, palette.body), palette.outline)
    drawCircle(palette.belly, radius = d(prop.bodyRx * 0.55f), center = p(cx, prop.bodyCy + prop.bodyRy * 0.22f))

    // 큰 뒷발(앞으로 뻗음, 성장할수록 커짐)
    val fr = 0.042f + prop.fluff * 0.028f
    val fy = prop.bodyCy + prop.bodyRy * 0.82f
    listOf(-1f, 1f).forEach { s ->
        outlinedOval(p, d, cx + s * prop.bodyRx * 0.62f, fy, fr * 1.75f, fr * 0.80f, palette.body, palette.outline, OUT_W_S)
    }

    // 머리: 동글 타원 + 아웃라인
    outlinedOval(p, d, cx, hc, hr * 1.06f, hr * 0.96f, vGrad(p, cx, hc, hr, palette.bodyHighlight, palette.body), palette.outline)

    // 코(분홍 Y) + 앞니
    val noseCy = hc + hr * 0.40f
    triangle(
        p(cx - hr * 0.08f, noseCy - hr * 0.03f),
        p(cx + hr * 0.08f, noseCy - hr * 0.03f),
        p(cx, noseCy + hr * 0.06f),
        palette.nose,
    )
    drawLine(palette.nose, p(cx, noseCy + hr * 0.06f), p(cx, noseCy + hr * 0.15f), strokeWidth = d(0.007f))
    // 앞니(흰 사각 + 가운데 골)
    val toothW = hr * 0.16f
    val toothH = hr * 0.15f
    val toothTop = noseCy + hr * 0.15f
    drawRoundRect(
        color = palette.bodyHighlight,
        topLeft = p(cx - toothW / 2f, toothTop),
        size = Size(d(toothW), d(toothH)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(d(toothW * 0.25f)),
    )
    drawRoundRect(
        color = palette.outline,
        topLeft = p(cx - toothW / 2f, toothTop),
        size = Size(d(toothW), d(toothH)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(d(toothW * 0.25f)),
        style = outlineStroke(d, OUT_W_S * 0.8f),
    )
    drawLine(palette.outline, p(cx, toothTop), p(cx, toothTop + toothH), strokeWidth = d(0.005f))
}

/** 귀 하나: 아웃라인 타원 + 핑크 속귀. [bendDeg]로 곧음/꺾임, pose(처짐·쫑긋)와 합성. */
private fun DrawScope.drawRabbitEar(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    sideSign: Float,
    bendDeg: Float,
    lenMul: Float,
) {
    val hr = prop.headR * 1.06f
    val baseX = 0.5f + sideSign * hr * 0.38f
    val baseY = prop.headCy - hr * 0.55f
    val len = prop.earLen * 1.1f * lenMul
    val w = prop.earW * 2.1f
    val tilt = bendDeg +
        (pose.earPerk - 1f) * -8f * sideSign +
        pose.earDroop * sideSign * 34f +
        motion.earTwitch * sideSign * 6f
    rotate(tilt, pivot = p(baseX, baseY)) {
        outlinedOval(p, d, baseX, baseY - len / 2f, w / 2f, len / 2f + w * 0.2f, palette.body, palette.outline, OUT_W_S)
        fillOval(p, d, baseX, baseY - len / 2f, w * 0.24f, len / 2f * 0.74f, palette.earInner)
    }
}
