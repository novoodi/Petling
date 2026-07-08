package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.petling.domain.model.Species

/** 햄스터 정면: 원형 몸 + 볼주머니 불룩 + 작은 반원 귀 + 가슴 앞 두 손 모으기. */
internal fun DrawScope.drawHamster(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
) {
    val cx = 0.5f

    // 작은 반원 귀
    val earY = prop.headCy - prop.headR * 0.72f
    listOf(-1f, 1f).forEach { s ->
        val ex = cx + s * prop.headR * 0.5f
        drawCircle(palette.body, radius = d(prop.earLen), center = p(ex, earY - pose.earDroop * -0.01f))
        drawCircle(palette.earInner, radius = d(prop.earLen * 0.55f), center = p(ex, earY))
    }

    drawMammalBody(p, d, palette, prop, motion)

    // 볼주머니 불룩(성장할수록 빵빵)
    val cheekBulge = prop.headR * (0.32f + prop.fluff * 0.12f)
    listOf(-1f, 1f).forEach { s ->
        drawOval(
            palette.bodyHighlight.copy(alpha = 0.75f),
            topLeft = p(cx + s * prop.headR * 0.42f - cheekBulge / 2f, prop.headCy + prop.headR * 0.28f - cheekBulge / 2f),
            size = Size(d(cheekBulge), d(cheekBulge * 0.85f)),
        )
    }

    // 가슴 앞 두 손 모으기(작은 앞발 위로)
    val handY = prop.bodyCy - prop.bodyRy * 0.15f
    listOf(-1f, 1f).forEach { s ->
        drawCircle(palette.bodyShadow, radius = d(0.030f), center = p(cx + s * 0.035f, handY))
    }

    // 코(분홍 점)
    drawCircle(palette.earInner, radius = d(prop.headR * 0.07f), center = p(cx, prop.headCy + prop.headR * 0.46f))
}

/** 햄스터 옆모습(종종걸음): 짧은 다리 빠른 스윙 + 볼주머니 + 꼬마 꼬리. */
internal fun DrawScope.drawHamsterSide(
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
    val gait = gaitFor(Species.HAMSTER)

    // 꼬마 꼬리
    drawCircle(palette.bodyShadow, radius = d(0.018f), center = p(sp.bodyCx - sp.bodyHalfLen * 1.02f, sp.bodyCy + sp.bodyHalfHt * 0.2f))

    drawQuadrupedCore(p, d, palette, sp, phi, gait, lod)

    // 작은 반원 귀
    drawCircle(palette.body, radius = d(sp.headR * 0.30f), center = p(sp.headCx - sp.headR * 0.05f, sp.headCy - sp.headR * 0.75f))
    drawCircle(palette.earInner, radius = d(sp.headR * 0.16f), center = p(sp.headCx - sp.headR * 0.05f, sp.headCy - sp.headR * 0.75f))

    // 볼주머니(앞볼 불룩)
    drawOval(
        palette.bodyHighlight.copy(alpha = 0.75f),
        topLeft = p(sp.headCx + sp.headR * 0.25f, sp.headCy + sp.headR * 0.1f),
        size = Size(d(sp.headR * 0.55f), d(sp.headR * 0.45f)),
    )

    // 코(분홍 점)
    drawCircle(palette.earInner, radius = d(sp.headR * 0.08f), center = p(sp.headCx + sp.headR * 0.9f, sp.headCy + sp.headR * 0.05f))

    drawSideFace(p, d, palette, sp.headCx, sp.headCy, sp.headR, pose, irisFor(Species.HAMSTER, palette), eyeStyle, blink, lod)
}
