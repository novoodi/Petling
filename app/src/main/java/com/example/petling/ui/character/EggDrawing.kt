package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

private val CRACK_DARK = Color(0xFF5C4326)
private val GLOW = Color(0xFFFFF2C4)

/** 알 + 부화 크랙 연출. crackProgress 0→1 로 3단계, 마지막에 셸 캡이 들리며 빛이 샌다. */
internal fun DrawScope.drawEgg(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    crackProgress: Float,
    alpha: Float,
) {
    val cx = 0.5f
    val topY = 0.24f
    val botY = 0.80f
    val midY = 0.55f
    val halfW = 0.21f

    // 달걀 곡률 실루엣(상협하광)
    val path = scratchPath()
    val t = p(cx, topY)
    path.moveTo(t.x, t.y)
    path.cubicTo(p(cx + halfW * 0.85f, topY + 0.02f).x, p(cx + halfW * 0.85f, topY + 0.02f).y, p(cx + halfW, midY - 0.05f).x, p(cx + halfW, midY - 0.05f).y, p(cx + halfW, midY).x, p(cx + halfW, midY).y)
    path.cubicTo(p(cx + halfW, botY - 0.08f).x, p(cx + halfW, botY - 0.08f).y, p(cx + halfW * 0.62f, botY).x, p(cx + halfW * 0.62f, botY).y, p(cx, botY).x, p(cx, botY).y)
    path.cubicTo(p(cx - halfW * 0.62f, botY).x, p(cx - halfW * 0.62f, botY).y, p(cx - halfW, botY - 0.08f).x, p(cx - halfW, botY - 0.08f).y, p(cx - halfW, midY).x, p(cx - halfW, midY).y)
    path.cubicTo(p(cx - halfW, midY - 0.05f).x, p(cx - halfW, midY - 0.05f).y, p(cx - halfW * 0.85f, topY + 0.02f).x, p(cx - halfW * 0.85f, topY + 0.02f).y, t.x, t.y)
    path.close()

    val brush = Brush.verticalGradient(
        colors = listOf(palette.bodyHighlight.copy(alpha = alpha), palette.body.copy(alpha = alpha)),
        startY = p(cx, topY).y,
        endY = p(cx, botY).y,
    )
    drawPath(path, brush, style = Fill)
    drawPath(path, palette.outline.copy(alpha = alpha), style = outlineStroke(d))
    // 하단 반사광
    drawOval(palette.bodyHighlight.copy(alpha = alpha * 0.4f), topLeft = p(cx - halfW * 0.5f, botY - 0.14f), size = Size(d(halfW), d(0.06f)))
    // 상단 하이라이트
    drawOval(Color.White.copy(alpha = alpha * 0.35f), topLeft = p(cx - halfW * 0.4f, topY + 0.06f), size = Size(d(halfW * 0.5f), d(0.08f)))

    // 스페클
    val speckle = palette.marking.copy(alpha = alpha * 0.35f)
    listOf(
        0.42f to 0.36f, 0.58f to 0.40f, 0.47f to 0.50f, 0.62f to 0.58f,
        0.38f to 0.60f, 0.54f to 0.68f, 0.45f to 0.44f, 0.60f to 0.48f,
    ).forEach { (sx, sy) ->
        drawCircle(speckle, radius = d(0.012f), center = p(sx, sy))
    }

    if (crackProgress <= 0f) return

    // 1단계: 헤어라인 크랙
    val crack = scratchPath()
    crack.moveTo(p(0.50f, 0.28f).x, p(0.50f, 0.28f).y)
    crack.lineTo(p(0.45f, 0.37f).x, p(0.45f, 0.37f).y)
    crack.lineTo(p(0.55f, 0.45f).x, p(0.55f, 0.45f).y)
    crack.lineTo(p(0.46f, 0.53f).x, p(0.46f, 0.53f).y)
    crack.lineTo(p(0.53f, 0.62f).x, p(0.53f, 0.62f).y)
    drawPath(crack, CRACK_DARK.copy(alpha = alpha), style = Stroke(d(0.009f)))

    // 2단계: 분기 크랙 + 파편
    if (crackProgress > 0.34f) {
        val br = scratchPath()
        br.moveTo(p(0.55f, 0.45f).x, p(0.55f, 0.45f).y)
        br.lineTo(p(0.64f, 0.43f).x, p(0.64f, 0.43f).y)
        br.moveTo(p(0.46f, 0.53f).x, p(0.46f, 0.53f).y)
        br.lineTo(p(0.38f, 0.55f).x, p(0.38f, 0.55f).y)
        drawPath(br, CRACK_DARK.copy(alpha = alpha), style = Stroke(d(0.007f)))
        drawCircle(CRACK_DARK.copy(alpha = alpha * 0.6f), radius = d(0.008f), center = p(0.60f, 0.50f))
    }

    // 3단계: 셸 캡 들림 + 빛샘
    if (crackProgress > 0.7f) {
        val lift = (crackProgress - 0.7f) / 0.3f
        // 벌어진 틈 글로우
        drawLine(GLOW.copy(alpha = alpha * lift), p(0.42f, 0.335f), p(0.58f, 0.335f), strokeWidth = d(0.02f * lift + 0.004f))
        // 들린 상단 캡
        drawArc(
            palette.bodyHighlight.copy(alpha = alpha),
            180f, 180f, true,
            topLeft = p(cx - halfW * 0.7f, topY - 0.03f * lift),
            size = Size(d(halfW * 1.4f), d(0.14f)),
        )
    }
}
