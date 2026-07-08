package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

private val RAB_NOSE = Color(0xFFDD7C93)

/** 토끼: 긴 귀(아기 땐 한쪽 접힘) + 흰 폼폼 꼬리 + 큰 뒷발. */
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
    // 폼폼 꼬리(뒤)
    drawCircle(palette.belly, radius = d(prop.tailThick), center = p(cx + prop.bodyRx * 0.8f, prop.bodyCy + prop.bodyRy * 0.35f))

    // 귀(뒤)
    drawRabbitEar(p, d, palette, prop, pose, motion, sideSign = -1f, folded = prop.earFold > 0.05f)
    drawRabbitEar(p, d, palette, prop, pose, motion, sideSign = 1f, folded = false)

    drawMammalBody(p, d, palette, prop, motion)

    // 큰 뒷발(성장할수록 커짐)
    if (prop.fluff > 0.3f) {
        val fy = prop.bodyCy + prop.bodyRy * 0.72f
        val fr = 0.05f + prop.fluff * 0.03f
        listOf(-1f, 1f).forEach { s ->
            drawOval(palette.body, topLeft = p(cx + s * prop.bodyRx * 0.55f - fr, fy - fr * 0.5f), size = Size(d(fr * 2), d(fr * 1.2f)))
            drawOval(palette.belly.copy(alpha = 0.8f), topLeft = p(cx + s * prop.bodyRx * 0.55f - fr * 0.6f, fy + fr * 0.1f), size = Size(d(fr * 1.2f), d(fr * 0.5f)))
        }
    } else {
        drawPaws(p, d, palette, prop, lod)
    }

    // 코(분홍 Y) — 입은 face가 그림
    val noseCy = prop.headCy + prop.headR * 0.48f
    triangle(p(cx - prop.headR * 0.07f, noseCy - prop.headR * 0.03f), p(cx + prop.headR * 0.07f, noseCy - prop.headR * 0.03f), p(cx, noseCy + prop.headR * 0.05f), RAB_NOSE)
    drawLine(RAB_NOSE, p(cx, noseCy + prop.headR * 0.05f), p(cx, noseCy + prop.headR * 0.16f), strokeWidth = d(0.008f))
}

private fun DrawScope.drawRabbitEar(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    sideSign: Float,
    folded: Boolean,
) {
    val baseX = cxRabbitEar(prop, sideSign)
    val baseY = prop.headCy - prop.headR * 0.45f
    val len = prop.earLen
    val w = prop.earW * 1.9f
    val tilt = sideSign * 9f + (pose.earPerk - 1f) * -6f * sideSign + pose.earDroop * sideSign * 40f + motion.earTwitch * sideSign * 6f
    rotate(tilt, pivot = p(baseX, baseY)) {
        if (folded) {
            // 아기: 짧고 위쪽이 바깥으로 꺾임
            val shortLen = len * 0.6f
            drawOval(palette.body, topLeft = p(baseX - w * 0.5f, baseY - shortLen), size = Size(d(w), d(shortLen)))
            drawOval(palette.earInner, topLeft = p(baseX - w * 0.24f, baseY - shortLen * 0.9f), size = Size(d(w * 0.48f), d(shortLen * 0.7f)))
            // 꺾인 끝
            rotate(sideSign * 55f, pivot = p(baseX, baseY - shortLen)) {
                drawOval(palette.body, topLeft = p(baseX - w * 0.45f, baseY - shortLen - len * 0.35f), size = Size(d(w * 0.9f), d(len * 0.4f)))
            }
        } else {
            drawOval(palette.body, topLeft = p(baseX - w * 0.5f, baseY - len), size = Size(d(w), d(len)))
            drawOval(palette.earInner, topLeft = p(baseX - w * 0.26f, baseY - len * 0.94f), size = Size(d(w * 0.52f), d(len * 0.8f)))
        }
    }
}

private fun cxRabbitEar(prop: Proportions, sideSign: Float): Float = 0.5f + sideSign * prop.headR * 0.35f
