package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.petling.domain.model.Species
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.petling.domain.model.GrowthStage
import kotlin.math.PI
import kotlin.math.sin

private val BEAK = Color(0xFFF2A03D)
private val BEAK_DARK = Color(0xFFDA8420)
private val COMB = Color(0xFFE85C52)

/** 병아리: 유생 솜털 공 → 성숙 약병아리(볏·층진 날개깃·꽁지깃). */
internal fun DrawScope.drawChick(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
    stage: GrowthStage,
) {
    val cx = 0.5f
    val mature = stage == GrowthStage.MATURE
    val g2plus = stage == GrowthStage.GROWTH2 || mature
    val g1plus = stage == GrowthStage.GROWTH1 || g2plus

    // 꽁지깃(뒤)
    if (g1plus) {
        val tailN = if (mature) 3 else if (g2plus) 3 else 2
        val rootX = cx + prop.bodyRx * 0.75f
        val rootY = prop.bodyCy + prop.bodyRy * 0.1f
        val wag = sin(motion.tailWag * 2 * PI).toFloat() * pose.tailWagAmp * 6f
        for (i in 0 until tailN) {
            val spread = (i - (tailN - 1) / 2f) * 16f
            rotate(spread + wag, pivot = p(rootX, rootY)) {
                drawOval(palette.bodyShadow, topLeft = p(rootX, rootY - 0.02f), size = Size(d(0.14f), d(0.05f)))
                if (lod == Lod.FULL) drawLine(palette.marking.copy(alpha = 0.4f), p(rootX + 0.02f, rootY), p(rootX + 0.12f, rootY), strokeWidth = d(0.005f))
            }
        }
    }

    // 정수리 솜털/볏
    if (mature || g2plus) {
        // 볏(빨강 반원 몇 개)
        val combN = if (mature) 3 else 2
        val topY = prop.headCy - prop.headR * 0.95f
        for (i in 0 until combN) {
            val dx = (i - (combN - 1) / 2f) * prop.headR * 0.22f
            drawCircle(COMB, radius = d(prop.headR * (0.10f + if (mature) 0.02f else 0f)), center = p(cx + dx, topY + prop.headR * 0.05f))
        }
    } else {
        // 아기 정수리 솜털 2가닥
        listOf(-0.03f, 0.03f).forEach { dx ->
            drawLine(palette.bodyShadow, p(cx + dx, prop.headCy - prop.headR * 0.85f), p(cx + dx * 2.2f, prop.headCy - prop.headR * 1.15f), strokeWidth = d(0.012f))
        }
    }

    drawMammalBody(p, d, palette, prop, motion)

    // 솜털 질감 범프(FULL, 유생·성장기)
    if (lod == Lod.FULL && !mature) {
        val n = 7
        for (i in 0 until n) {
            val a = PI * (0.15 + 0.7 * i / (n - 1))
            val bx = cx - prop.bodyRx * sin(a).toFloat() * 0.98f
            val by = prop.bodyCy + prop.bodyRy * 0.9f * (-kotlin.math.cos(a).toFloat())
            drawCircle(palette.bodyHighlight.copy(alpha = 0.4f), radius = d(prop.bodyRx * 0.09f), center = p(bx, by))
        }
    }

    // 가슴 크림(성숙)
    if (mature) {
        drawOval(palette.belly, topLeft = p(cx - prop.bodyRx * 0.32f, prop.bodyCy - prop.bodyRy * 0.35f), size = Size(d(prop.bodyRx * 0.64f), d(prop.bodyRy * 0.9f)))
    }

    // 날개
    val wingY = prop.bodyCy - prop.bodyRy * 0.05f
    listOf(-1f, 1f).forEach { s ->
        val wx = cx + s * prop.bodyRx * 0.82f
        drawOval(palette.bodyShadow.copy(alpha = 0.85f), topLeft = p(wx - 0.055f, wingY - 0.06f), size = Size(d(0.11f), d(0.16f)))
        if (lod == Lod.FULL && g1plus) {
            val layers = if (mature) 3 else if (g2plus) 2 else 1
            for (k in 1..layers) {
                drawArc(palette.marking.copy(alpha = 0.4f), 0f, 180f, false, topLeft = p(wx - 0.05f, wingY - 0.02f + k * 0.03f), size = Size(d(0.10f), d(0.06f)), style = androidx.compose.ui.graphics.drawscope.Stroke(d(0.005f)))
            }
        }
    }

    // 부리(다이아몬드)
    val beakCy = prop.headCy + prop.headR * (0.5f + if (mature) 0.06f else 0f)
    val bw = prop.headR * 0.16f
    val bh = prop.headR * (0.14f + if (mature) 0.04f else 0f)
    triangle(p(cx - bw, beakCy), p(cx + bw, beakCy), p(cx, beakCy - bh), BEAK)
    triangle(p(cx - bw, beakCy), p(cx + bw, beakCy), p(cx, beakCy + bh), BEAK_DARK)

    // 발(주황 3발가락)
    val fy = prop.bodyCy + prop.bodyRy * 0.92f
    listOf(-1f, 1f).forEach { s ->
        val fx = cx + s * prop.bodyRx * 0.32f
        drawLine(BEAK_DARK, p(fx, fy - 0.02f), p(fx, fy + 0.03f), strokeWidth = d(0.012f))
        listOf(-0.03f, 0f, 0.03f).forEach { tx ->
            drawLine(BEAK_DARK, p(fx, fy + 0.03f), p(fx + tx, fy + 0.06f), strokeWidth = d(0.008f))
        }
    }
}

/** 병아리 옆모습(2족 보행): 세로 몸통 + 두 다리 교차 + 날개 퍼덕 + 부리. */
internal fun DrawScope.drawChickSide(
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
    val gait = gaitFor(Species.CHICK)

    // 꽁지 솜털(뒤)
    listOf(0.0f, 0.12f).forEach { t ->
        drawCircle(
            palette.bodyShadow.copy(alpha = 0.8f),
            radius = d(0.035f - t * 0.1f),
            center = p(sp.bodyCx - sp.bodyHalfLen * 1.05f - t * 0.2f, sp.bodyCy - sp.bodyHalfHt * 0.15f - t),
        )
    }

    drawBipedCore(p, d, palette, sp, phi, gait, BEAK_DARK, lod)

    // 머리(몸 위 앞쪽, 한 덩어리로 겹침)
    fillOval(p, d, sp.headCx, sp.headCy, sp.headR, sp.headR, vGrad(p, sp.headCx, sp.headCy, sp.headR, palette.bodyHighlight, palette.body))

    // 가슴 크림(앞쪽)
    drawOval(
        palette.belly.copy(alpha = 0.9f),
        topLeft = p(sp.bodyCx, sp.bodyCy - sp.bodyHalfHt * 0.3f),
        size = Size(d(sp.bodyHalfLen * 0.9f), d(sp.bodyHalfHt * 1.1f)),
    )

    // 날개(근측, 걸음에 맞춰 미세 퍼덕)
    val flap = sin(phi * 2 * PI).toFloat() * 4f
    rotate(flap, pivot = p(sp.bodyCx, sp.bodyCy - sp.bodyHalfHt * 0.2f)) {
        drawOval(
            palette.bodyShadow.copy(alpha = 0.85f),
            topLeft = p(sp.bodyCx - sp.bodyHalfLen * 0.55f, sp.bodyCy - sp.bodyHalfHt * 0.35f),
            size = Size(d(sp.bodyHalfLen * 0.9f), d(sp.bodyHalfHt * 0.8f)),
        )
    }

    // 정수리 솜털
    listOf(-0.015f, 0.015f).forEach { dx ->
        drawLine(
            palette.bodyShadow,
            p(sp.headCx + dx, sp.headCy - sp.headR * 0.8f),
            p(sp.headCx + dx * 2.2f, sp.headCy - sp.headR * 1.1f),
            strokeWidth = d(0.010f),
        )
    }

    // 부리(앞)
    val by = sp.headCy + sp.headR * 0.12f
    triangle(
        p(sp.headCx + sp.headR * 0.55f, by - sp.headR * 0.14f),
        p(sp.headCx + sp.headR * 0.55f, by + sp.headR * 0.10f),
        p(sp.headCx + sp.headR * 1.05f, by - sp.headR * 0.02f),
        BEAK,
    )

    drawSideFace(p, d, palette, sp.headCx, sp.headCy, sp.headR, pose, irisFor(Species.CHICK, palette), eyeStyle, blink, lod)
}
