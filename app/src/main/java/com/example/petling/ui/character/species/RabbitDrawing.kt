package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.petling.domain.model.Species
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

/** 토끼 옆모습(홉 보행): 도약 아치 + 뒷다리 신전 + 귀 뒤로 휘날림 + 폼폼 꼬리. */
internal fun DrawScope.drawRabbitSide(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    sp: SideProportions,
    pose: FacePose,
    motion: CreatureMotion,
    eyeStyle: Int,
    blink: Float,
    lod: Lod,
) {
    val (dy, extend, pitch) = hopProfile(motion.walkCycle)
    val cx = sp.bodyCx
    val cy = sp.bodyCy + dy

    rotate(-pitch, pivot = p(cx, cy)) {
        val thighR = sp.bodyHalfHt * 0.85f
        val hindAngle = -32f * extend        // 신전 시 뒤로 뻗음
        val frontAngle = 22f * extend        // 신전 시 앞으로 턱

        // 원측 다리
        drawSideLeg(p, d, sp.hipX - 0.012f, cy + sp.bodyHalfHt * 0.4f, sp.legLen, sp.legThick, hindAngle, lerpDark(palette), null, lod)
        drawSideLeg(p, d, sp.shoulderX - 0.012f, cy + sp.bodyHalfHt * 0.35f, sp.legLen * 0.8f, sp.legThick * 0.85f, frontAngle, lerpDark(palette), null, lod)

        // 폼폼 꼬리
        drawCircle(palette.belly, radius = d(0.035f), center = p(cx - sp.bodyHalfLen * 1.0f, cy - sp.bodyHalfHt * 0.2f))

        // 몸통(둥근, 엉덩이 쪽 두툼)
        fillOval(p, d, cx, cy, sp.bodyHalfLen, sp.bodyHalfHt, vGrad(p, cx, cy, sp.bodyHalfHt, palette.bodyHighlight, palette.body))
        // 뒷넓적다리 볼륨
        drawOval(palette.bodyShadow.copy(alpha = 0.35f), topLeft = p(sp.hipX - thighR, cy - thighR * 0.4f), size = Size(d(thighR * 2f), d(thighR * 1.6f)))
        // 배 크림
        drawOval(palette.belly.copy(alpha = 0.85f), topLeft = p(cx - sp.bodyHalfLen * 0.4f, cy + sp.bodyHalfHt * 0.1f), size = Size(d(sp.bodyHalfLen * 0.9f), d(sp.bodyHalfHt * 0.8f)))

        // 근측 다리
        drawSideLeg(p, d, sp.hipX, cy + sp.bodyHalfHt * 0.4f, sp.legLen, sp.legThick * 1.15f, hindAngle, palette.body, palette.bodyShadow, lod)
        drawSideLeg(p, d, sp.shoulderX, cy + sp.bodyHalfHt * 0.35f, sp.legLen * 0.8f, sp.legThick * 0.85f, frontAngle, palette.body, palette.bodyShadow, lod)

        // 머리
        val hx = sp.headCx
        val hy = sp.headCy + dy * 0.6f
        fillOval(p, d, hx, hy, sp.headR, sp.headR, vGrad(p, hx, hy, sp.headR, palette.bodyHighlight, palette.body))

        // 긴 귀(뒤로 휘날림: 체공일수록 뒤로)
        val earSweep = 12f + extend.coerceAtLeast(0f) * 22f + pose.earDroop * 30f
        listOf(0f, 1f).forEach { far ->
            val col = if (far > 0f) lerpDark(palette) else palette.body
            val inner = if (far > 0f) null else palette.earInner
            rotate(-earSweep - far * 8f, pivot = p(hx - sp.headR * 0.1f, hy - sp.headR * 0.6f)) {
                drawOval(col, topLeft = p(hx - sp.headR * 0.25f - far * 0.02f, hy - sp.headR * 0.6f - sp.headR * 1.5f), size = Size(d(sp.headR * 0.34f), d(sp.headR * 1.55f)))
                inner?.let {
                    drawOval(it, topLeft = p(hx - sp.headR * 0.17f, hy - sp.headR * 0.6f - sp.headR * 1.35f), size = Size(d(sp.headR * 0.18f), d(sp.headR * 1.2f)))
                }
            }
        }

        // 코(분홍)
        triangle(
            p(hx + sp.headR * 0.78f, hy + sp.headR * 0.02f),
            p(hx + sp.headR * 0.95f, hy + sp.headR * 0.02f),
            p(hx + sp.headR * 0.86f, hy + sp.headR * 0.14f),
            RAB_NOSE,
        )

        drawSideFace(p, d, palette, hx, hy, sp.headR, pose, irisFor(Species.RABBIT, palette), eyeStyle, blink, lod)
    }
}

private fun lerpDark(palette: ModoriPalette): Color =
    androidx.compose.ui.graphics.lerp(palette.body, palette.bodyShadow, 0.55f)
