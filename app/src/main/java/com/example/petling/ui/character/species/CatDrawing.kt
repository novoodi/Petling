package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.petling.domain.model.Species
import kotlin.math.PI
import kotlin.math.sin

/**
 * 고양이 "도도한 관찰자".
 * 실루엣: 날씬한 몸 + 곧게 치켜든 긴 꼬리 / 컬러: 블루그레이+크림 배 /
 * 시그니처: 이마 M 무늬 + 꼬리 줄무늬 링.
 */
internal fun DrawScope.drawCat(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
) {
    val cx = 0.5f
    val hr = prop.headR * 1.02f
    val hc = prop.headCy

    // 치켜든 긴 꼬리(뒤): S커브 스트로크 + 링 무늬
    val rootX = cx + prop.bodyRx * 0.66f
    val rootY = prop.bodyCy + prop.bodyRy * 0.70f
    val tailLen = prop.tailLen * 1.35f + 0.02f
    val wag = sin(motion.tailWag * 2 * PI).toFloat() * pose.tailWagAmp * 8f - pose.tailLift * 7f
    rotate(wag, pivot = p(rootX, rootY)) {
        // 제어점(위로 뻗는 S커브)
        val c1x = rootX + tailLen * 0.58f; val c1y = rootY - tailLen * 0.26f
        val e2x = rootX + tailLen * 0.34f; val e2y = rootY - tailLen * 1.00f
        drawStrokeTail(d, prop.tailThick * 1.15f, palette.body, palette.outline) { path ->
            val a = p(rootX, rootY)
            val b = p(c1x, c1y)
            val c = p(e2x, e2y)
            path.moveTo(a.x, a.y)
            path.quadraticBezierTo(b.x, b.y, c.x, c.y)
        }
        // 꼬리 링(FULL·성숙)
        if (lod == Lod.FULL && prop.fluff > 0.5f) {
            listOf(0.62f, 0.82f).forEach { t ->
                val mt = 1f - t
                val qx = mt * mt * rootX + 2 * mt * t * c1x + t * t * e2x
                val qy = mt * mt * rootY + 2 * mt * t * c1y + t * t * e2y
                // 접선의 수직 방향으로 짧은 줄
                val tx = 2 * mt * (c1x - rootX) + 2 * t * (e2x - c1x)
                val ty = 2 * mt * (c1y - rootY) + 2 * t * (e2y - c1y)
                val invLen = 1f / (kotlin.math.sqrt(tx * tx + ty * ty) + 1e-6f)
                val nx = -ty * invLen; val ny = tx * invLen
                val half = prop.tailThick * 0.62f
                drawLine(
                    palette.marking,
                    p(qx - nx * half, qy - ny * half),
                    p(qx + nx * half, qy + ny * half),
                    strokeWidth = d(prop.tailThick * 0.55f),
                )
            }
        }
    }

    // 긴 삼각귀(뒤): 핑크 속귀 + 아웃라인
    val earCy = hc - hr * 0.60f
    val earLen = prop.earLen * 1.55f
    drawTriEar(p, d, cx - hr * 0.52f, earCy, earLen, prop.earW, palette.body, palette.earInner, null, -1f, pose.earDroop, pose.earPerk, motion.earTwitch, palette.outline)
    drawTriEar(p, d, cx + hr * 0.52f, earCy, earLen, prop.earW, palette.body, palette.earInner, null, 1f, pose.earDroop, pose.earPerk, motion.earTwitch, palette.outline)

    // 날씬한 앉은 몸통 + 앞발
    drawSittingBody(p, d, palette, prop, motion, widthMul = 0.76f)
    drawSitPaws(p, d, palette, prop, color = palette.body)

    // 머리: 볼이 살짝 넓은 타원 + 아웃라인
    outlinedOval(p, d, cx, hc, hr * 1.06f, hr * 0.92f, vGrad(p, cx, hc, hr, palette.bodyHighlight, palette.body), palette.outline)

    // 이마 M 무늬(FULL)
    if (lod == Lod.FULL) {
        val topY = hc - hr * 0.62f
        listOf(-0.30f, 0f, 0.30f).forEach { t ->
            drawLine(
                palette.marking,
                p(cx + t * hr, topY),
                p(cx + t * hr * 0.72f, topY + hr * 0.30f),
                strokeWidth = d(0.010f),
            )
        }
    }

    // 코(분홍 삼각)
    val noseCy = hc + hr * 0.42f
    triangle(
        p(cx - hr * 0.09f, noseCy - hr * 0.04f),
        p(cx + hr * 0.09f, noseCy - hr * 0.04f),
        p(cx, noseCy + hr * 0.07f),
        palette.nose,
    )

    // 수염(FULL)
    if (lod == Lod.FULL) {
        val w = palette.outline.copy(alpha = 0.45f)
        val wy = noseCy + hr * 0.05f
        listOf(-0.04f, 0.02f).forEach { dy ->
            drawLine(w, p(cx - hr * 0.30f, wy + dy * hr), p(cx - hr * 1.02f, wy + dy * hr * 2f - hr * 0.05f), strokeWidth = d(0.005f))
            drawLine(w, p(cx + hr * 0.30f, wy + dy * hr), p(cx + hr * 1.02f, wy + dy * hr * 2f - hr * 0.05f), strokeWidth = d(0.005f))
        }
    }
}

/** 고양이 옆모습(보행): 가는 다리·S커브 꼬리(위로)·둥근 귀·짧은 주둥이·수염. */
internal fun DrawScope.drawCatSide(
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
    val phi = motion.walkCycle
    val gait = gaitFor(Species.CAT)
    val spCat = sp.copy(legThick = 0.035f)

    // S커브 꼬리(맨 뒤, 위로 든 형태 + 살랑)
    val rearX = sp.bodyCx - sp.bodyHalfLen * 0.92f
    val sway = sin(motion.tailWag * 2 * PI).toFloat() * 0.025f + pose.tailLift * 0.02f
    val n = 10
    for (i in 0..n) {
        val t = i / n.toFloat()
        val tx = rearX - 0.045f * sin((t * Math.PI * 0.9).toFloat()) - t * 0.01f
        val ty = sp.bodyCy - t * (0.16f + sway)
        drawCircle(palette.body, radius = d(0.020f * (1f - t * 0.25f)), center = p(tx, ty))
    }

    drawQuadrupedCore(p, d, palette, spCat, phi, gait, lod)

    // 등 줄무늬(FULL)
    if (lod == Lod.FULL) {
        listOf(-0.3f, 0f, 0.3f).forEach { t ->
            drawLine(
                palette.marking.copy(alpha = 0.6f),
                p(sp.bodyCx + t * sp.bodyHalfLen * 0.8f, sp.bodyCy - sp.bodyHalfHt * 0.95f),
                p(sp.bodyCx + t * sp.bodyHalfLen * 0.8f - 0.01f, sp.bodyCy - sp.bodyHalfHt * 0.45f),
                strokeWidth = d(0.010f),
            )
        }
    }

    // 둥근 삼각 귀(+아웃라인)
    val earBase = sp.headCy - sp.headR * 0.7f
    triangle(
        p(sp.headCx - sp.headR * 0.1f, earBase + 0.02f),
        p(sp.headCx + sp.headR * 0.4f, earBase + 0.02f),
        p(sp.headCx + sp.headR * (0.15f - pose.earBack * 0.25f), earBase - sp.headR * (0.6f * (1f - pose.earDroop * 0.6f))),
        palette.body, palette.outline, d,
    )
    // 주둥이 + 분홍 코 + 수염
    val mzY = sp.headCy + sp.headR * 0.2f
    drawOval(palette.belly.copy(alpha = 0.9f), topLeft = p(sp.headCx + sp.headR * 0.4f, mzY - sp.headR * 0.22f), size = Size(d(sp.headR * 0.55f), d(sp.headR * 0.4f)))
    triangle(
        p(sp.headCx + sp.headR * 0.78f, mzY - sp.headR * 0.12f),
        p(sp.headCx + sp.headR * 0.95f, mzY - sp.headR * 0.12f),
        p(sp.headCx + sp.headR * 0.86f, mzY - sp.headR * 0.02f),
        palette.nose,
    )
    if (lod == Lod.FULL) {
        val w = palette.outline.copy(alpha = 0.4f)
        listOf(-0.02f, 0.01f).forEach { dy ->
            drawLine(w, p(sp.headCx + sp.headR * 0.6f, mzY + dy), p(sp.headCx + sp.headR * 1.15f, mzY + dy * 2f + 0.01f), strokeWidth = d(0.004f))
        }
    }

    drawSideFace(p, d, palette, sp.headCx, sp.headCy, sp.headR, pose, irisFor(Species.CAT, palette), eyeStyle, blink, lod)
}
