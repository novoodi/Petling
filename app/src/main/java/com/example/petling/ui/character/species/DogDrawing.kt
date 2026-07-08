package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.petling.domain.model.Species
import kotlin.math.PI
import kotlin.math.sin

/** 강아지 정면: 좌우로 늘어진 타원 귀 + 주둥이 + 등 새들 마킹 + 옆에 말린 꼬리. */
internal fun DrawScope.drawDog(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
) {
    val cx = 0.5f

    // 말린 꼬리(뒤, 몸 옆 위) — 살랑
    val wag = sin(motion.tailWag * 2 * PI).toFloat() * pose.tailWagAmp * 14f
    rotate(wag, pivot = p(cx + prop.bodyRx * 0.7f, prop.bodyCy)) {
        drawCircle(palette.body, radius = d(prop.tailThick * 1.1f), center = p(cx + prop.bodyRx * 0.88f, prop.bodyCy - prop.bodyRy * 0.45f))
        drawCircle(palette.bodyHighlight.copy(alpha = 0.6f), radius = d(prop.tailThick * 0.5f), center = p(cx + prop.bodyRx * 0.88f, prop.bodyCy - prop.bodyRy * 0.5f))
    }

    // 늘어진 귀(뒤) — 좌우로 처진 타원, earDroop이면 더 처짐
    val earTop = prop.headCy - prop.headR * 0.55f
    listOf(-1f, 1f).forEach { s ->
        rotate(s * (35f + pose.earDroop * 15f - (pose.earPerk - 1f) * 40f), pivot = p(cx + s * prop.headR * 0.55f, earTop)) {
            drawOval(
                palette.bodyShadow,
                topLeft = p(cx + s * prop.headR * 0.55f - prop.earLen * 0.28f, earTop - prop.earLen * 0.1f),
                size = Size(d(prop.earLen * 0.56f), d(prop.earLen * 1.15f)),
            )
        }
    }

    drawMammalBody(p, d, palette, prop, motion)
    drawPaws(p, d, palette, prop, lod)

    // 등 새들 마킹(FULL)
    if (lod == Lod.FULL) {
        drawOval(
            palette.marking.copy(alpha = 0.30f),
            topLeft = p(cx - prop.bodyRx * 0.55f, prop.bodyCy - prop.bodyRy * 0.9f),
            size = Size(d(prop.bodyRx * 1.1f), d(prop.bodyRy * 0.55f)),
        )
    }

    // 주둥이 + 코
    if (prop.muzzle > 0.1f) {
        val mcy = prop.headCy + prop.headR * (0.42f + prop.muzzle * 0.26f)
        drawOval(
            palette.belly,
            topLeft = p(cx - prop.headR * 0.30f, mcy - prop.headR * 0.24f),
            size = Size(d(prop.headR * 0.60f), d(prop.headR * 0.50f)),
        )
    }
    val noseCy = prop.headCy + prop.headR * (0.44f + prop.muzzle * 0.26f)
    drawCircle(palette.nose, radius = d(prop.headR * 0.10f), center = p(cx, noseCy))
}

/** 강아지 옆모습(보행): 늘어진 귀가 걸음에 맞춰 흔들리고, 말린 꼬리·새들 마킹. */
internal fun DrawScope.drawDogSide(
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
    val gait = gaitFor(Species.DOG)

    // 말린 꼬리(엉덩이 끝에 붙은 나선 근사) — 몸 실루엣에 겹치게
    val tailX = sp.bodyCx - sp.bodyHalfLen * 1.02f
    val tailY = sp.bodyCy - sp.bodyHalfHt * 0.55f
    val wag = sin(motion.tailWag * 2 * PI).toFloat() * 10f
    rotate(wag, pivot = p(tailX + 0.02f, tailY + 0.03f)) {
        drawCircle(palette.body, radius = d(0.048f), center = p(tailX, tailY))
        drawCircle(palette.bodyHighlight.copy(alpha = 0.55f), radius = d(0.022f), center = p(tailX, tailY - 0.012f))
    }

    drawQuadrupedCore(p, d, palette, sp, phi, gait, lod)

    // 등 새들 마킹(FULL)
    if (lod == Lod.FULL) {
        drawOval(
            palette.marking.copy(alpha = 0.28f),
            topLeft = p(sp.bodyCx - sp.bodyHalfLen * 0.6f, sp.bodyCy - sp.bodyHalfHt * 1.0f),
            size = Size(d(sp.bodyHalfLen * 1.1f), d(sp.bodyHalfHt * 0.7f)),
        )
    }

    // 늘어진 귀(머리 옆, 걸음에 맞춰 ±8° 스윙)
    val earSwing = sin(phi * 2 * PI).toFloat() * 8f
    rotate(10f + earSwing + pose.earDroop * 12f, pivot = p(sp.headCx - sp.headR * 0.1f, sp.headCy - sp.headR * 0.55f)) {
        drawOval(
            palette.bodyShadow,
            topLeft = p(sp.headCx - sp.headR * 0.35f, sp.headCy - sp.headR * 0.6f),
            size = Size(d(sp.headR * 0.5f), d(sp.headR * 1.05f)),
        )
    }

    // 주둥이 + 코
    val mzY = sp.headCy + sp.headR * 0.2f
    drawOval(palette.belly, topLeft = p(sp.headCx + sp.headR * 0.32f, mzY - sp.headR * 0.26f), size = Size(d(sp.headR * 0.7f), d(sp.headR * 0.48f)))
    drawCircle(palette.nose, radius = d(sp.headR * 0.10f), center = p(sp.headCx + sp.headR * 0.95f, mzY - sp.headR * 0.08f))

    drawSideFace(p, d, palette, sp.headCx, sp.headCy, sp.headR, pose, irisFor(Species.DOG, palette), eyeStyle, blink, lod)
}
