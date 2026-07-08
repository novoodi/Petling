package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.Mood
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val HEART = Color(0xFFF47C97)
private val SPARK = Color(0xFFFFD25E)
private val ZZZ = Color(0xFF9FA6B2)
private val SWEAT = Color(0xFF7FC8E8)
private val DUST = Color(0x55B7A98F)

/** 기분·표정에 따른 이펙트(하트/반짝임/zzz/땀/착지먼지). */
internal fun DrawScope.drawMoodEffects(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    mood: Mood,
    expression: Expression,
    prop: Proportions,
    motion: CreatureMotion,
    lod: Lod,
) {
    // 착지 먼지(홉 착지 순간)
    if (motion.dust > 0f) {
        val fy = 0.86f
        val spread = motion.dust * 0.06f
        listOf(-1f, 1f).forEach { s ->
            val a = 0.7f - motion.dust * 0.5f
            drawCircle(DUST.copy(alpha = DUST.alpha * a), radius = d(0.02f + motion.dust * 0.02f), center = p(0.5f + s * (0.08f + spread), fy))
        }
        drawCircle(DUST.copy(alpha = DUST.alpha * (0.7f - motion.dust * 0.5f)), radius = d(0.015f), center = p(0.5f, fy + 0.005f))
    }

    val ph = motion.effectPhase
    val rise = ph * prop.headR * 1.6f
    val fade = (1f - ph).coerceIn(0f, 1f)

    when (mood) {
        Mood.HAPPY -> {
            val topY = prop.headCy - prop.headR
            drawHeart(p, d, 0.5f - prop.headR * 0.9f, topY - rise, prop.headR * 0.18f, HEART.copy(alpha = fade))
            drawHeart(p, d, 0.5f + prop.headR * 0.7f, topY - rise * 0.7f - prop.headR * 0.2f, prop.headR * 0.14f, HEART.copy(alpha = fade * 0.9f))
            if (lod == Lod.FULL) {
                drawSparkle(p(0.5f + prop.headR * 1.0f, topY + prop.headR * 0.3f), d(prop.headR * 0.14f), SPARK.copy(alpha = fade))
                drawSparkle(p(0.5f - prop.headR * 1.1f, topY + prop.headR * 0.6f), d(prop.headR * 0.10f), SPARK.copy(alpha = fade))
            }
        }
        Mood.TIRED -> {
            val zx = 0.5f + prop.headR * 0.9f
            val zy = prop.headCy - prop.headR * 0.9f
            for (i in 0 until 3) {
                val sz = prop.headR * (0.14f + i * 0.06f)
                val yy = zy - rise * 0.8f - i * prop.headR * 0.45f
                val xx = zx + i * prop.headR * 0.22f
                val al = (fade * (1f - i * 0.2f)).coerceIn(0f, 1f)
                drawZ(p, d, xx, yy, sz, ZZZ.copy(alpha = al))
            }
        }
        Mood.CALM -> Unit
    }

    if (expression == Expression.WORRIED) {
        drawSweat(p, d, 0.5f + prop.headR * 0.85f, prop.headCy - prop.headR * 0.2f, prop.headR * 0.16f)
    }
}

private fun DrawScope.drawHeart(p: (Float, Float) -> Offset, d: (Float) -> Float, cx: Float, cy: Float, r: Float, color: Color) {
    val path = scratchPath()
    val c = p(cx, cy)
    val rr = d(r)
    path.moveTo(c.x, c.y + rr * 0.9f)
    path.cubicTo(c.x - rr * 1.4f, c.y - rr * 0.2f, c.x - rr * 0.5f, c.y - rr * 1.2f, c.x, c.y - rr * 0.4f)
    path.cubicTo(c.x + rr * 0.5f, c.y - rr * 1.2f, c.x + rr * 1.4f, c.y - rr * 0.2f, c.x, c.y + rr * 0.9f)
    path.close()
    drawPath(path, color, style = Fill)
}

private fun DrawScope.drawZ(p: (Float, Float) -> Offset, d: (Float) -> Float, cx: Float, cy: Float, sz: Float, color: Color) {
    val w = d(0.008f)
    drawLine(color, p(cx - sz * 0.5f, cy - sz * 0.5f), p(cx + sz * 0.5f, cy - sz * 0.5f), strokeWidth = w)
    drawLine(color, p(cx + sz * 0.5f, cy - sz * 0.5f), p(cx - sz * 0.5f, cy + sz * 0.5f), strokeWidth = w)
    drawLine(color, p(cx - sz * 0.5f, cy + sz * 0.5f), p(cx + sz * 0.5f, cy + sz * 0.5f), strokeWidth = w)
}

private fun DrawScope.drawSweat(p: (Float, Float) -> Offset, d: (Float) -> Float, cx: Float, cy: Float, r: Float) {
    val path = scratchPath()
    val c = p(cx, cy)
    val rr = d(r)
    path.moveTo(c.x, c.y - rr)
    path.cubicTo(c.x + rr * 0.9f, c.y + rr * 0.1f, c.x + rr * 0.6f, c.y + rr, c.x, c.y + rr)
    path.cubicTo(c.x - rr * 0.6f, c.y + rr, c.x - rr * 0.9f, c.y + rr * 0.1f, c.x, c.y - rr)
    path.close()
    drawPath(path, SWEAT, style = Fill)
    drawCircle(Color.White.copy(alpha = 0.6f), radius = d(r * 0.2f), center = p(cx - r * 0.25f, cy + r * 0.1f))
}

internal fun DrawScope.drawSparkle(center: Offset, r: Float, color: Color) {
    val path = scratchPath()
    for (i in 0 until 4) {
        val a = i * (PI / 2)
        val outer = Offset(center.x + (r * cos(a)).toFloat(), center.y + (r * sin(a)).toFloat())
        val innerA = a + PI / 4
        val inner = Offset(center.x + (r * 0.35f * cos(innerA)).toFloat(), center.y + (r * 0.35f * sin(innerA)).toFloat())
        if (i == 0) path.moveTo(outer.x, outer.y) else path.lineTo(outer.x, outer.y)
        path.lineTo(inner.x, inner.y)
    }
    path.close()
    drawPath(path, color, style = Fill)
}
