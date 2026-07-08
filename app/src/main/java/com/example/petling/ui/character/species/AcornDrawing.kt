package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.petling.domain.model.GrowthStage

private val SPROUT = Color(0xFF6BBF59)
private val SPROUT_DARK = Color(0xFF4E9E42)
private val STEM = Color(0xFF7A5A34)

private fun nutRx(prop: Proportions) = prop.bodyRx * 1.08f
private fun nutRy(prop: Proportions) = prop.bodyRy * 1.12f
private const val NUT_CY = 0.58f

/** 도토리 몸통 위에 얼굴이 오도록 face용 비율을 보정. */
internal fun acornFaceProp(prop: Proportions): Proportions = prop.copy(
    headCy = NUT_CY - nutRy(prop) * 0.18f,
    headR = nutRx(prop) * 0.82f,
    muzzle = 0f,
    eyeGap = 0.5f,
    eyeY = 0.05f,
)

/** 도토리(마스코트): 왁시 견과 곡선 + 깍정이(격자·스캘럽) + 단계별 새싹. */
internal fun DrawScope.drawAcorn(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    motion: CreatureMotion,
    lod: Lod,
    stage: GrowthStage,
) {
    val cx = 0.5f
    val rx = nutRx(prop)
    val ry = nutRy(prop) * (1f + motion.breathe * 0.012f)
    val cy = NUT_CY

    // 견과 몸통: 위는 둥글고 아래로 뾰족
    val path = scratchPath()
    val top = p(cx, cy - ry)
    path.moveTo(p(cx - rx, cy - ry * 0.2f).x, p(cx - rx, cy - ry * 0.2f).y)
    path.cubicTo(
        p(cx - rx, cy - ry * 0.9f).x, p(cx - rx, cy - ry * 0.9f).y,
        p(cx + rx, cy - ry * 0.9f).x, p(cx + rx, cy - ry * 0.9f).y,
        p(cx + rx, cy - ry * 0.2f).x, p(cx + rx, cy - ry * 0.2f).y,
    )
    // 오른쪽 → 바닥 팁
    path.cubicTo(
        p(cx + rx, cy + ry * 0.7f).x, p(cx + rx, cy + ry * 0.7f).y,
        p(cx + rx * 0.35f, cy + ry).x, p(cx + rx * 0.35f, cy + ry).y,
        p(cx, cy + ry * 1.05f).x, p(cx, cy + ry * 1.05f).y,
    )
    // 바닥 팁 → 왼쪽
    path.cubicTo(
        p(cx - rx * 0.35f, cy + ry).x, p(cx - rx * 0.35f, cy + ry).y,
        p(cx - rx, cy + ry * 0.7f).x, p(cx - rx, cy + ry * 0.7f).y,
        p(cx - rx, cy - ry * 0.2f).x, p(cx - rx, cy - ry * 0.2f).y,
    )
    path.close()
    drawPath(path, palette.body, style = Fill)
    // 왁시 광택(상단 하이라이트)
    drawOval(palette.bodyHighlight.copy(alpha = 0.55f), topLeft = p(cx - rx * 0.55f, cy - ry * 0.55f), size = Size(d(rx * 0.7f), d(ry * 0.5f)))
    // 하단 음영
    drawOval(palette.bodyShadow.copy(alpha = 0.3f), topLeft = p(cx - rx * 0.7f, cy + ry * 0.1f), size = Size(d(rx * 1.4f), d(ry * 0.8f)))
    // 나뭇결(FULL)
    if (lod == Lod.FULL) {
        listOf(-0.3f, 0.3f).forEach { t ->
            drawArc(palette.bodyShadow.copy(alpha = 0.28f), 70f, 40f, false, topLeft = p(cx - rx * 0.5f + t * rx, cy - ry * 0.3f), size = Size(d(rx), d(ry * 1.2f)), style = Stroke(d(0.006f)))
        }
    }

    // 깍정이(모자)
    val capRx = rx * 1.06f
    val capTop = cy - ry - 0.02f
    drawArc(palette.cap, 180f, 180f, true, topLeft = p(cx - capRx, capTop), size = Size(d(capRx * 2), d(capRx * 1.4f)))
    // 스캘럽 테두리
    for (i in -2..2) {
        drawCircle(palette.cap, radius = d(capRx * 0.16f), center = p(cx + i * capRx * 0.4f, capTop + capRx * 0.7f))
    }
    // 격자 텍스처(FULL)
    if (lod == Lod.FULL) {
        for (i in -2..2) {
            drawLine(palette.capShadow.copy(alpha = 0.6f), p(cx + i * capRx * 0.32f, capTop + capRx * 0.1f), p(cx + i * capRx * 0.32f, capTop + capRx * 0.7f), strokeWidth = d(0.005f))
        }
        listOf(0.3f, 0.55f).forEach { t ->
            drawArc(palette.capShadow.copy(alpha = 0.5f), 200f, 140f, false, topLeft = p(cx - capRx * (1f - t * 0.3f), capTop + capRx * 0.1f * t), size = Size(d(capRx * 2 * (1f - t * 0.3f)), d(capRx * t)), style = Stroke(d(0.005f)))
        }
    }
    // 꼭지
    drawOval(palette.capShadow, topLeft = p(cx - 0.018f, capTop - 0.05f), size = Size(d(0.036f), d(0.06f)))

    // 단계별 새싹(꼭지 옆)
    when (stage) {
        GrowthStage.GROWTH1 -> drawSprout(p, d, cx + 0.03f, capTop - 0.03f, 1)
        GrowthStage.GROWTH2 -> drawSprout(p, d, cx + 0.03f, capTop - 0.03f, 2)
        else -> Unit
    }
}

private fun DrawScope.drawSprout(p: (Float, Float) -> Offset, d: (Float) -> Float, x: Float, y: Float, leaves: Int) {
    drawLine(STEM, p(x, y + 0.03f), p(x, y - 0.05f), strokeWidth = d(0.008f))
    drawOval(SPROUT, topLeft = p(x, y - 0.07f), size = Size(d(0.05f), d(0.045f)))
    if (leaves >= 2) {
        drawOval(SPROUT_DARK, topLeft = p(x - 0.05f, y - 0.05f), size = Size(d(0.05f), d(0.04f)))
    }
}
