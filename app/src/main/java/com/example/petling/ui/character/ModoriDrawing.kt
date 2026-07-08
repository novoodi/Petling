package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Mood
import com.example.petling.domain.model.Species
import kotlin.math.cos
import kotlin.math.sin

// 종 공통 액센트 색
private val PINK = Color(0xFFF6AFC0)
private val BEAK = Color(0xFFF2A03D)
private val NOSE = Color(0xFF3A2E28)
private val WHITE = Color(0xFFFBF7F0)

/**
 * 스타팅 동물 벡터 드로잉(도토리/여우/고양이/토끼/병아리).
 * 좌표는 캔버스 정사각 기준 정규화 비율(0~1)이라 크기와 무관하게 동일하게 그려진다.
 * 몸통·귀·마킹만 종별로 다르고 얼굴·기분·성장 액세서리는 공유한다.
 */
fun DrawScope.drawCreature(
    species: Species,
    stage: GrowthStage,
    branch: Branch?,
    mood: Mood,
    expression: Expression,
    palette: ModoriPalette,
    eyeStyle: Int,
    blink: Float = 0f,
    crackProgress: Float = 0f,
    hatchAlpha: Float = 1f,
) {
    val s = size.minDimension
    val origin = Offset((size.width - s) / 2f, (size.height - s) / 2f)

    fun p(x: Float, y: Float) = Offset(origin.x + x * s, origin.y + y * s)
    fun d(v: Float) = v * s

    // 바닥 그림자
    drawOval(
        color = Color(0x22000000),
        topLeft = p(0.30f, 0.86f),
        size = Size(d(0.40f), d(0.08f)),
    )

    if (stage == GrowthStage.EGG) {
        drawEgg(::p, ::d, palette, crackProgress, hatchAlpha)
        return
    }

    val scale = when (stage) {
        GrowthStage.JUVENILE -> 0.72f
        GrowthStage.GROWTH1 -> 0.84f
        GrowthStage.GROWTH2 -> 0.94f
        GrowthStage.MATURE -> 1.0f
        else -> 0.8f
    }

    when (species) {
        Species.ACORN -> drawAcornBody(::p, ::d, palette, scale)
        Species.FOX -> drawFoxBody(::p, ::d, palette, scale)
        Species.CAT -> drawCatBody(::p, ::d, palette, scale)
        Species.RABBIT -> drawRabbitBody(::p, ::d, palette, scale)
        Species.CHICK -> drawChickBody(::p, ::d, palette, scale)
    }
    drawStageFeatures(::p, ::d, palette, species, stage, branch, scale)
    drawFace(::p, ::d, palette, species, expression, eyeStyle, blink, scale)
    drawMoodAccent(::p, ::d, mood)
}

/** 하위 호환: 기존 호출부(도토리). */
fun DrawScope.drawModori(
    stage: GrowthStage,
    branch: Branch?,
    mood: Mood,
    expression: Expression,
    palette: ModoriPalette,
    eyeStyle: Int,
    blink: Float = 0f,
    crackProgress: Float = 0f,
    hatchAlpha: Float = 1f,
) = drawCreature(Species.ACORN, stage, branch, mood, expression, palette, eyeStyle, blink, crackProgress, hatchAlpha)

// ─────────────────────────── 공통 요소 ───────────────────────────

private fun DrawScope.drawEgg(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    crackProgress: Float,
    alpha: Float,
) {
    drawOval(
        color = palette.bodyHighlight.copy(alpha = alpha),
        topLeft = p(0.30f, 0.24f),
        size = Size(d(0.40f), d(0.54f)),
    )
    drawOval(
        color = palette.body.copy(alpha = alpha * 0.35f),
        topLeft = p(0.30f, 0.50f),
        size = Size(d(0.40f), d(0.28f)),
    )
    listOf(0.40f to 0.36f, 0.56f to 0.44f, 0.46f to 0.58f).forEach { (x, y) ->
        drawCircle(palette.body.copy(alpha = alpha * 0.4f), radius = d(0.02f), center = p(x, y))
    }
    if (crackProgress > 0f) {
        val crack = Path().apply {
            moveTo(p(0.50f, 0.26f).x, p(0.50f, 0.26f).y)
            lineTo(p(0.44f, 0.36f).x, p(0.44f, 0.36f).y)
            lineTo(p(0.54f, 0.44f).x, p(0.54f, 0.44f).y)
            lineTo(p(0.46f, 0.54f).x, p(0.46f, 0.54f).y)
            lineTo(p(0.52f, 0.64f).x, p(0.52f, 0.64f).y)
        }
        drawPath(crack, color = Color(0xFF5C4326).copy(alpha = alpha * crackProgress), style = Stroke(width = d(0.012f)))
    }
}

/** 동물 공통: 둥근 몸통(머리+몸 한 덩어리 치비 스타일). */
private fun DrawScope.drawRoundBody(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    scale: Float,
) {
    val cx = 0.5f
    val cy = 0.58f
    val rx = 0.25f * scale
    val ry = 0.26f * scale
    drawOval(palette.body, topLeft = p(cx - rx, cy - ry), size = Size(d(rx * 2), d(ry * 2)))
    drawOval(palette.bodyShadow.copy(alpha = 0.45f), topLeft = p(cx - rx, cy + ry * 0.05f), size = Size(d(rx * 2), d(ry * 0.95f)))
    drawOval(palette.bodyHighlight.copy(alpha = 0.55f), topLeft = p(cx - rx * 0.55f, cy - ry * 0.8f), size = Size(d(rx * 0.7f), d(ry * 0.5f)))
}

private fun DrawScope.triangle(a: Offset, b: Offset, c: Offset, color: Color) {
    val path = Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }
    drawPath(path, color, style = Fill)
}

// ─────────────────────────── 종별 몸통 ───────────────────────────

private fun DrawScope.drawAcornBody(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    scale: Float,
) {
    val cx = 0.5f; val cy = 0.58f; val rx = 0.24f * scale; val ry = 0.26f * scale
    drawOval(palette.body, topLeft = p(cx - rx, cy - ry), size = Size(d(rx * 2), d(ry * 2)))
    drawOval(palette.bodyShadow.copy(alpha = 0.5f), topLeft = p(cx - rx, cy - ry * 0.1f), size = Size(d(rx * 2), d(ry * 1.1f)))
    drawOval(palette.bodyHighlight.copy(alpha = 0.6f), topLeft = p(cx - rx * 0.6f, cy - ry * 0.8f), size = Size(d(rx * 0.7f), d(ry * 0.5f)))
    // 깍정이(도토리 모자)
    val capRx = 0.26f * scale
    val capTop = cy - ry - 0.10f * scale
    drawArc(palette.cap, 180f, 180f, true, topLeft = p(cx - capRx, capTop), size = Size(d(capRx * 2), d(capRx * 1.5f)))
    drawOval(palette.capShadow, topLeft = p(cx - capRx, capTop + capRx * 0.6f), size = Size(d(capRx * 2), d(0.06f * scale)))
    drawOval(palette.capShadow, topLeft = p(cx - 0.02f, capTop - 0.05f * scale), size = Size(d(0.04f), d(0.06f * scale)))
    listOf(-0.10f, 0f, 0.10f).forEach { dx ->
        drawCircle(palette.capShadow.copy(alpha = 0.5f), radius = d(0.012f), center = p(cx + dx * scale, capTop + capRx * 0.5f))
    }
}

private fun DrawScope.drawFoxBody(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    scale: Float,
) {
    // 뾰족 삼각 귀(먼저 그려 몸통 뒤로)
    triangle(p(0.30f, 0.46f), p(0.32f, 0.24f), p(0.46f, 0.40f), palette.bodyShadow)
    triangle(p(0.70f, 0.46f), p(0.68f, 0.24f), p(0.54f, 0.40f), palette.bodyShadow)
    triangle(p(0.33f, 0.44f), p(0.345f, 0.30f), p(0.44f, 0.40f), WHITE)
    triangle(p(0.67f, 0.44f), p(0.655f, 0.30f), p(0.56f, 0.40f), WHITE)
    drawRoundBody(p, d, palette, scale)
    // 흰 볼/주둥이
    drawOval(WHITE, topLeft = p(0.40f, 0.60f), size = Size(d(0.20f), d(0.16f)))
    // 코
    drawCircle(NOSE, radius = d(0.018f), center = p(0.5f, 0.635f))
}

private fun DrawScope.drawCatBody(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    scale: Float,
) {
    triangle(p(0.32f, 0.46f), p(0.34f, 0.26f), p(0.47f, 0.40f), palette.body)
    triangle(p(0.68f, 0.46f), p(0.66f, 0.26f), p(0.53f, 0.40f), palette.body)
    triangle(p(0.35f, 0.44f), p(0.36f, 0.32f), p(0.45f, 0.40f), PINK)
    triangle(p(0.65f, 0.44f), p(0.64f, 0.32f), p(0.55f, 0.40f), PINK)
    drawRoundBody(p, d, palette, scale)
    // 코(작은 분홍 삼각)
    triangle(p(0.485f, 0.62f), p(0.515f, 0.62f), p(0.5f, 0.645f), PINK)
    // 수염
    val w = NOSE.copy(alpha = 0.55f)
    listOf(-0.02f, 0f, 0.02f).forEach { dy ->
        drawLine(w, p(0.42f, 0.63f + dy), p(0.30f, 0.61f + dy * 1.5f), strokeWidth = d(0.006f))
        drawLine(w, p(0.58f, 0.63f + dy), p(0.70f, 0.61f + dy * 1.5f), strokeWidth = d(0.006f))
    }
}

private fun DrawScope.drawRabbitBody(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    scale: Float,
) {
    // 길쭉한 귀
    rotate(-10f, pivot = p(0.45f, 0.42f)) {
        drawOval(palette.body, topLeft = p(0.41f, 0.16f), size = Size(d(0.08f), d(0.26f)))
        drawOval(PINK, topLeft = p(0.43f, 0.20f), size = Size(d(0.04f), d(0.18f)))
    }
    rotate(10f, pivot = p(0.55f, 0.42f)) {
        drawOval(palette.body, topLeft = p(0.51f, 0.16f), size = Size(d(0.08f), d(0.26f)))
        drawOval(PINK, topLeft = p(0.53f, 0.20f), size = Size(d(0.04f), d(0.18f)))
    }
    drawRoundBody(p, d, palette, scale)
    // 코
    triangle(p(0.485f, 0.63f), p(0.515f, 0.63f), p(0.5f, 0.65f), PINK)
}

private fun DrawScope.drawChickBody(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    scale: Float,
) {
    // 머리 깃털
    listOf(-0.04f, 0f, 0.04f).forEach { dx ->
        drawLine(palette.bodyShadow, p(0.5f + dx, 0.36f), p(0.5f + dx * 1.4f, 0.30f), strokeWidth = d(0.014f))
    }
    drawRoundBody(p, d, palette, scale)
    // 작은 날개
    drawOval(palette.bodyShadow.copy(alpha = 0.7f), topLeft = p(0.26f, 0.58f), size = Size(d(0.09f), d(0.13f)))
    drawOval(palette.bodyShadow.copy(alpha = 0.7f), topLeft = p(0.65f, 0.58f), size = Size(d(0.09f), d(0.13f)))
    // 부리(다이아몬드)
    triangle(p(0.47f, 0.63f), p(0.53f, 0.63f), p(0.5f, 0.60f), BEAK)
    triangle(p(0.47f, 0.63f), p(0.53f, 0.63f), p(0.5f, 0.665f), BEAK)
}

// ─────────────────────────── 성장 액세서리 ───────────────────────────

private fun DrawScope.drawStageFeatures(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    species: Species,
    stage: GrowthStage,
    branch: Branch?,
    scale: Float,
) {
    when (stage) {
        GrowthStage.GROWTH1 -> {
            drawArmStubs(p, d, palette, scale)
            if (species == Species.ACORN) drawSprout(p, d, 0.5f, 0.24f, scale)
        }
        GrowthStage.GROWTH2 -> {
            drawArmStubs(p, d, palette, scale)
            drawBranchSignature(p, d, branch, scale)
        }
        GrowthStage.MATURE -> {
            drawArmStubs(p, d, palette, scale)
            if (species == Species.ACORN) drawLeafCrown(p, d, scale) else drawStarCrown(p, d, scale)
            drawBranchSignature(p, d, branch, scale)
        }
        else -> Unit
    }
}

private fun DrawScope.drawArmStubs(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    scale: Float,
) {
    drawCircle(palette.bodyShadow, radius = d(0.035f * scale), center = p(0.26f, 0.64f))
    drawCircle(palette.bodyShadow, radius = d(0.035f * scale), center = p(0.74f, 0.64f))
}

private fun DrawScope.drawSprout(p: (Float, Float) -> Offset, d: (Float) -> Float, cx: Float, cy: Float, scale: Float) {
    drawOval(Color(0xFF6BBF59), topLeft = p(cx - 0.02f, cy - 0.06f * scale), size = Size(d(0.06f * scale), d(0.10f * scale)))
}

private fun DrawScope.drawBranchSignature(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    branch: Branch?,
    scale: Float,
) {
    when (branch) {
        Branch.STUDY -> {
            val r = 0.055f * scale
            drawCircle(Color(0xFF3D3A34), radius = d(r), center = p(0.42f, 0.55f), style = Stroke(d(0.012f)))
            drawCircle(Color(0xFF3D3A34), radius = d(r), center = p(0.58f, 0.55f), style = Stroke(d(0.012f)))
            drawLine(Color(0xFF3D3A34), p(0.475f, 0.55f), p(0.525f, 0.55f), strokeWidth = d(0.012f))
        }
        Branch.HOBBY -> {
            drawCircle(Color(0xFF7C4DD1), radius = d(0.018f), center = p(0.62f, 0.30f))
            drawLine(Color(0xFF7C4DD1), p(0.635f, 0.30f), p(0.635f, 0.24f), strokeWidth = d(0.01f))
        }
        Branch.BALANCED -> {
            val leaf = Color(0xFF6BBF59)
            listOf(-0.03f to -0.02f, 0.03f to -0.02f, 0f to 0.03f).forEach { (dx, dy) ->
                drawCircle(leaf, radius = d(0.022f * scale), center = p(0.5f + dx, 0.26f + dy))
            }
        }
        null -> Unit
    }
}

private fun DrawScope.drawLeafCrown(p: (Float, Float) -> Offset, d: (Float) -> Float, scale: Float) {
    val leaf = Color(0xFF57A84A)
    for (i in -2..2) {
        rotate(i * 22f, pivot = p(0.5f, 0.28f)) {
            drawOval(leaf, topLeft = p(0.48f, 0.18f), size = Size(d(0.04f * scale), d(0.10f * scale)))
        }
    }
}

private fun DrawScope.drawStarCrown(p: (Float, Float) -> Offset, d: (Float) -> Float, scale: Float) {
    listOf(0.40f to 0.24f, 0.5f to 0.20f, 0.60f to 0.24f).forEach { (x, y) ->
        drawSparkle(p(x, y), d(0.022f * scale), Color(0xFFFFD25E))
    }
}

// ─────────────────────────── 얼굴 ───────────────────────────

private fun DrawScope.drawFace(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    species: Species,
    expression: Expression,
    eyeStyle: Int,
    blink: Float,
    scale: Float,
) {
    val eyeY = 0.56f
    val leftX = 0.43f
    val rightX = 0.57f
    val eyeR = 0.028f * scale

    drawCircle(palette.cheek.copy(alpha = 0.6f), radius = d(0.028f * scale), center = p(0.37f, 0.62f))
    drawCircle(palette.cheek.copy(alpha = 0.6f), radius = d(0.028f * scale), center = p(0.63f, 0.62f))

    val eyeColor = Color(0xFF2E2A24)
    val open = (1f - blink).coerceIn(0f, 1f)

    fun eye(x: Float) {
        when (expression) {
            Expression.HAPPY, Expression.EXCITED -> drawArc(
                eyeColor, 200f, 140f, false,
                topLeft = p(x - 0.03f * scale, eyeY - 0.03f * scale),
                size = Size(d(0.06f * scale), d(0.06f * scale)), style = Stroke(d(0.014f)),
            )
            Expression.SLEEPY -> drawLine(eyeColor, p(x - 0.028f * scale, eyeY), p(x + 0.028f * scale, eyeY + 0.01f), strokeWidth = d(0.012f))
            Expression.WORRIED -> {
                drawCircle(eyeColor, radius = d(eyeR), center = p(x, eyeY))
                drawLine(eyeColor, p(x - 0.03f * scale, eyeY - 0.05f * scale), p(x + 0.02f * scale, eyeY - 0.035f * scale), strokeWidth = d(0.01f))
            }
            Expression.SAD -> drawArc(
                eyeColor, 20f, 140f, false,
                topLeft = p(x - 0.03f * scale, eyeY - 0.02f * scale),
                size = Size(d(0.06f * scale), d(0.06f * scale)), style = Stroke(d(0.014f)),
            )
            Expression.NEUTRAL -> {
                if (open < 0.3f) {
                    drawLine(eyeColor, p(x - 0.028f * scale, eyeY), p(x + 0.028f * scale, eyeY), strokeWidth = d(0.012f))
                } else {
                    val h = eyeR * 2 * open
                    drawOval(eyeColor, topLeft = p(x - eyeR, eyeY - h / 2), size = Size(d(eyeR * 2), d(h)))
                    // 눈 하이라이트(반짝임) — 항상 살짝 넣어 더 귀엽게
                    drawCircle(Color.White, radius = d(0.008f), center = p(x + 0.008f, eyeY - 0.008f))
                }
            }
        }
    }
    eye(leftX)
    eye(rightX)

    // 병아리는 입 대신 부리(몸통에서 그림) → 입 생략
    if (species == Species.CHICK) return

    val mouthY = 0.68f
    when (expression) {
        Expression.HAPPY, Expression.EXCITED -> drawArc(
            eyeColor, 20f, 140f, false, topLeft = p(0.46f, mouthY - 0.03f),
            size = Size(d(0.08f), d(0.05f)), style = Stroke(d(0.012f)),
        )
        Expression.SLEEPY -> drawOval(eyeColor, topLeft = p(0.485f, mouthY), size = Size(d(0.03f), d(0.02f)))
        Expression.WORRIED, Expression.SAD -> drawArc(
            eyeColor, 200f, 140f, false, topLeft = p(0.46f, mouthY),
            size = Size(d(0.08f), d(0.05f)), style = Stroke(d(0.012f)),
        )
        Expression.NEUTRAL -> drawLine(eyeColor, p(0.475f, mouthY + 0.01f), p(0.525f, mouthY + 0.01f), strokeWidth = d(0.012f))
    }
}

private fun DrawScope.drawMoodAccent(p: (Float, Float) -> Offset, d: (Float) -> Float, mood: Mood) {
    when (mood) {
        Mood.HAPPY -> listOf(0.24f to 0.34f, 0.76f to 0.32f, 0.72f to 0.50f).forEach { (x, y) ->
            drawSparkle(p(x, y), d(0.02f), Color(0xFFFFD25E))
        }
        Mood.TIRED -> drawArc(
            Color(0x66A49E92), 120f, 180f, false,
            topLeft = p(0.62f, 0.30f), size = Size(d(0.05f), d(0.05f)), style = Stroke(d(0.008f)),
        )
        Mood.CALM -> Unit
    }
}

private fun DrawScope.drawSparkle(center: Offset, r: Float, color: Color) {
    val path = Path()
    for (i in 0 until 4) {
        val a = i * (Math.PI / 2)
        val outer = Offset(center.x + (r * cos(a)).toFloat(), center.y + (r * sin(a)).toFloat())
        val innerA = a + Math.PI / 4
        val inner = Offset(center.x + (r * 0.35f * cos(innerA)).toFloat(), center.y + (r * 0.35f * sin(innerA)).toFloat())
        if (i == 0) path.moveTo(outer.x, outer.y) else path.lineTo(outer.x, outer.y)
        path.lineTo(inner.x, inner.y)
    }
    path.close()
    drawPath(path, color, style = Fill)
}
