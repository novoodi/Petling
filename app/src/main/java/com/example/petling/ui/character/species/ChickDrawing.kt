package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.petling.domain.model.GrowthStage
import kotlin.math.PI
import kotlin.math.sin

private val BEAK = Color(0xFFF2A23A)
private val BEAK_DARK = Color(0xFFDA8420)
private val COMB = Color(0xFFE85C52)

/**
 * 병아리 "천진난만 막내".
 * 실루엣: 머리·몸 구분 없는 물방울 한 덩어리 / 컬러: 샛노랑+주황 부리·발 /
 * 시그니처: 정수리 솜털 세 가닥. 성장하면 볏·깃이 늘어 약병아리가 된다.
 */
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

    val breathe = 1f + motion.breathe * 0.012f
    val top = prop.headCy - prop.headR * 1.05f
    val bot = prop.bodyCy + prop.bodyRy * 0.92f * breathe
    val halfW = prop.bodyRx * 1.18f

    // 꽁지깃(뒤)
    if (g1plus) {
        val tailN = if (g2plus) 3 else 2
        val rootX = cx + halfW * 0.72f
        val rootY = bot - (bot - top) * 0.32f
        val wag = sin(motion.tailWag * 2 * PI).toFloat() * pose.tailWagAmp * 6f
        for (i in 0 until tailN) {
            val spread = (i - (tailN - 1) / 2f) * 16f
            rotate(spread + wag, pivot = p(rootX, rootY)) {
                outlinedOval(p, d, rootX + 0.06f, rootY, 0.065f, 0.026f, palette.bodyShadow, palette.outline, OUT_W_S)
            }
        }
    }

    // 주황 발(몸 아래로 빼꼼)
    listOf(-1f, 1f).forEach { s ->
        val fx = cx + s * halfW * 0.34f
        drawLine(BEAK_DARK, p(fx, bot - 0.01f), p(fx, bot + 0.030f), strokeWidth = d(0.016f))
        listOf(-0.030f, 0f, 0.030f).forEach { tx ->
            drawLine(BEAK_DARK, p(fx, bot + 0.028f), p(fx + tx, bot + 0.055f), strokeWidth = d(0.011f))
        }
    }

    // 물방울 몸통(머리+몸 한 덩어리)
    drawEggBody(p, d, top, bot, halfW, vGrad(p, cx, (top + bot) / 2f, (bot - top) / 2f, palette.bodyHighlight, palette.body), palette.outline)

    // 배 솜털(밝은 면 마킹)
    drawOval(
        palette.belly.copy(alpha = if (mature) 1f else 0.85f),
        topLeft = p(cx - halfW * 0.55f, bot - (bot - top) * 0.42f),
        size = Size(d(halfW * 1.10f), d((bot - top) * 0.34f)),
    )

    // 날개(양옆 작은 삼각 날갯죽지 + 아웃라인)
    val wingY = bot - (bot - top) * 0.44f
    listOf(-1f, 1f).forEach { s ->
        val wx = cx + s * halfW * 0.94f
        val wing = scratchPath()
        val a = p(wx - s * 0.015f, wingY - 0.055f)
        val b = p(wx + s * 0.055f, wingY + 0.045f)
        val c = p(wx - s * 0.045f, wingY + 0.052f)
        wing.moveTo(a.x, a.y)
        wing.quadraticBezierTo(p(wx + s * 0.055f, wingY - 0.03f).x, p(wx + s * 0.055f, wingY - 0.03f).y, b.x, b.y)
        wing.quadraticBezierTo(p(wx, wingY + 0.07f).x, p(wx, wingY + 0.07f).y, c.x, c.y)
        wing.close()
        outlinedPath(d, wing, palette.body, palette.outline, OUT_W_S)
        // 깃 레이어(FULL·성장기)
        if (lod == Lod.FULL && g1plus) {
            val layers = if (g2plus) 2 else 1
            for (k in 1..layers) {
                drawArc(
                    palette.marking.copy(alpha = 0.5f), 20f, 140f, false,
                    topLeft = p(wx - 0.035f, wingY - 0.02f + k * 0.025f),
                    size = Size(d(0.07f), d(0.04f)), style = Stroke(d(0.005f)),
                )
            }
        }
    }

    // 정수리: 아기~성장기는 솜털 세 가닥, 성숙하면 빨간 볏
    if (g2plus) {
        val combN = if (mature) 3 else 2
        for (i in 0 until combN) {
            val dx = (i - (combN - 1) / 2f) * prop.headR * 0.24f
            outlinedCircle(p, d, cx + dx, top + 0.004f - kotlin.math.abs(dx) * 0.3f, prop.headR * (0.11f + if (mature) 0.02f else 0f), COMB, palette.outline, OUT_W_S)
        }
    } else {
        val fluffC = palette.outline.copy(alpha = 0.85f)
        drawLine(fluffC, p(cx - 0.020f, top + 0.008f), p(cx - 0.042f, top - 0.038f), strokeWidth = d(0.008f))
        drawLine(fluffC, p(cx, top + 0.004f), p(cx - 0.008f, top - 0.052f), strokeWidth = d(0.008f))
        drawLine(fluffC, p(cx + 0.020f, top + 0.008f), p(cx + 0.040f, top - 0.036f), strokeWidth = d(0.008f))
    }

    // 부리(주황 다이아몬드 + 아웃라인)
    val beakCy = prop.headCy + prop.headR * (0.42f + if (mature) 0.05f else 0f)
    val bw = prop.headR * 0.20f
    val bh = prop.headR * (0.15f + if (mature) 0.04f else 0f)
    val beak = scratchPath()
    val l = p(cx - bw, beakCy); val r = p(cx + bw, beakCy)
    val t = p(cx, beakCy - bh); val b = p(cx, beakCy + bh)
    beak.moveTo(l.x, l.y); beak.lineTo(t.x, t.y); beak.lineTo(r.x, r.y); beak.lineTo(b.x, b.y); beak.close()
    drawPath(beak, BEAK)
    drawPath(beak, palette.outline, style = outlineStroke(d, OUT_W_S))
    // 아랫부리 음영(안쪽으로 살짝 인셋해 아웃라인을 가리지 않음 — triangle이 scratch를 재사용하므로 스트로크 후에)
    triangle(
        p(cx - bw * 0.72f, beakCy + bh * 0.10f),
        p(cx + bw * 0.72f, beakCy + bh * 0.10f),
        p(cx, beakCy + bh * 0.74f),
        BEAK_DARK,
    )
}
