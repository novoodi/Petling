package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.petling.domain.model.Species
import kotlin.math.PI
import kotlin.math.sin

private val P_BEAK = Color(0xFFF2A03D)
private val P_FEET = Color(0xFFE8933A)

/** 펭귄 정면: 다크 슬레이트 몸 + 큰 흰 배 + 플리퍼 + 부리·주황 발. */
internal fun DrawScope.drawPenguin(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
) {
    val cx = 0.5f

    drawMammalBody(p, d, palette, prop, motion)

    // 큰 흰 배(얼굴 아래부터 몸 하단까지)
    drawOval(
        palette.belly,
        topLeft = p(cx - prop.bodyRx * 0.52f, prop.headCy + prop.headR * 0.35f),
        size = Size(d(prop.bodyRx * 1.04f), d((prop.bodyCy + prop.bodyRy * 0.85f) - (prop.headCy + prop.headR * 0.35f))),
    )
    // 얼굴 흰 패치(눈 주변)
    listOf(-1f, 1f).forEach { s ->
        drawOval(
            palette.belly.copy(alpha = 0.9f),
            topLeft = p(cx + s * prop.headR * 0.42f - prop.headR * 0.3f, prop.headCy - prop.headR * 0.28f),
            size = Size(d(prop.headR * 0.6f), d(prop.headR * 0.62f)),
        )
    }

    // 플리퍼(몸 옆 아래로)
    val flap = sin(motion.tailWag * 2 * PI).toFloat() * 6f
    listOf(-1f, 1f).forEach { s ->
        rotate(s * (18f + flap), pivot = p(cx + s * prop.bodyRx * 0.85f, prop.bodyCy - prop.bodyRy * 0.35f)) {
            drawOval(
                palette.bodyShadow,
                topLeft = p(cx + s * prop.bodyRx * 0.85f - 0.035f, prop.bodyCy - prop.bodyRy * 0.4f),
                size = Size(d(0.07f), d(prop.bodyRy * 0.95f)),
            )
        }
    }

    // 부리(다이아몬드)
    val beakCy = prop.headCy + prop.headR * 0.42f
    val bw = prop.headR * 0.15f
    triangle(p(cx - bw, beakCy), p(cx + bw, beakCy), p(cx, beakCy - prop.headR * 0.12f), P_BEAK)
    triangle(p(cx - bw, beakCy), p(cx + bw, beakCy), p(cx, beakCy + prop.headR * 0.13f), P_BEAK)

    // 주황 발
    val fy = prop.bodyCy + prop.bodyRy * 0.9f
    listOf(-1f, 1f).forEach { s ->
        drawOval(P_FEET, topLeft = p(cx + s * prop.bodyRx * 0.3f - 0.035f, fy - 0.012f), size = Size(d(0.07f), d(0.032f)))
    }
}

/** 펭귄 옆모습(뒤뚱 보행): 직립 몸 + 흰 배 + 플리퍼 + 두 발 교차, 몸 롤은 렌더러 tilt. */
internal fun DrawScope.drawPenguinSide(
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
    val gait = gaitFor(Species.PENGUIN)

    drawBipedCore(p, d, palette, sp, phi, gait, P_FEET, lod)

    // 흰 배(앞쪽 반)
    drawOval(
        palette.belly,
        topLeft = p(sp.bodyCx - sp.bodyHalfLen * 0.15f, sp.bodyCy - sp.bodyHalfHt * 0.55f),
        size = Size(d(sp.bodyHalfLen * 1.05f), d(sp.bodyHalfHt * 1.5f)),
    )

    // 머리(몸 상단과 한 덩어리)
    fillOval(p, d, sp.headCx, sp.headCy, sp.headR, sp.headR, vGrad(p, sp.headCx, sp.headCy, sp.headR, palette.bodyHighlight, palette.body))
    // 얼굴 흰 패치
    drawOval(
        palette.belly.copy(alpha = 0.9f),
        topLeft = p(sp.headCx + sp.headR * 0.05f, sp.headCy - sp.headR * 0.3f),
        size = Size(d(sp.headR * 0.65f), d(sp.headR * 0.7f)),
    )

    // 플리퍼(근측, 걸음 반동으로 살짝 흔들)
    val flap = sin(phi * 2 * PI).toFloat() * 10f
    rotate(12f + flap, pivot = p(sp.bodyCx + sp.bodyHalfLen * 0.1f, sp.bodyCy - sp.bodyHalfHt * 0.35f)) {
        drawOval(
            palette.bodyShadow,
            topLeft = p(sp.bodyCx + sp.bodyHalfLen * 0.1f - 0.03f, sp.bodyCy - sp.bodyHalfHt * 0.4f),
            size = Size(d(0.06f), d(sp.bodyHalfHt * 1.1f)),
        )
    }

    // 부리(앞으로 뾰족)
    val by = sp.headCy + sp.headR * 0.05f
    triangle(
        p(sp.headCx + sp.headR * 0.55f, by - sp.headR * 0.12f),
        p(sp.headCx + sp.headR * 0.55f, by + sp.headR * 0.10f),
        p(sp.headCx + sp.headR * 1.1f, by),
        P_BEAK,
    )

    drawSideFace(p, d, palette, sp.headCx, sp.headCy, sp.headR, pose, irisFor(Species.PENGUIN, palette), eyeStyle, blink, lod)
}
