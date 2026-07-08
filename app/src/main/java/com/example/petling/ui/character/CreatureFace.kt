package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.example.petling.domain.model.Species

/**
 * 얼굴 = 눈(홍채/동공/이중 하이라이트) + 눈썹 + 입 + 볼. 모든 종이 공유한다.
 * 표정은 [FacePose]가, 크기 비율은 [Proportions]가 결정한다. eyeStyle(0~2)로 눈 모양을 커스터마이즈.
 */

private val EYE_DARK = Color(0xFF2A2420)
private val PUPIL = Color(0xFF17130F)
private val WHITE_HL = Color(0xFFFFFFFF)
private val BROW = Color(0xFF3A2E24)
private val MOUTH = Color(0xFF5A4232)
private val TONGUE = Color(0xFFEE8C9A)
private val TEAR = Color(0xCC9FD8F2)

fun DrawScope.drawFace(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    species: Species,
    prop: Proportions,
    pose: FacePose,
    irisColor: Color,
    eyeStyle: Int,
    blink: Float,
    lod: Lod,
) {
    val cx = 0.5f
    val headR = prop.headR
    val eyeCy = prop.headCy + prop.eyeY * headR
    val gap = prop.eyeGap * headR
    val eyeR = headR * 0.30f * prop.eyeScale

    // 볼터치
    val cheekR = headR * 0.20f
    drawCircle(palette.cheek.copy(alpha = 0.55f), radius = d(cheekR), center = p(cx - gap - eyeR * 0.4f, eyeCy + eyeR * 1.2f))
    drawCircle(palette.cheek.copy(alpha = 0.55f), radius = d(cheekR), center = p(cx + gap + eyeR * 0.4f, eyeCy + eyeR * 1.2f))

    drawEye(p, d, p(cx - gap, eyeCy), eyeR, pose, irisColor, eyeStyle, blink, lod, mirror = false)
    drawEye(p, d, p(cx + gap, eyeCy), eyeR, pose, irisColor, eyeStyle, blink, lod, mirror = true)

    // 눈썹
    pose.browAngle?.let { angle ->
        drawBrow(p, d, p(cx - gap, eyeCy - eyeR * 1.5f), eyeR, angle, mirror = false)
        drawBrow(p, d, p(cx + gap, eyeCy - eyeR * 1.5f), eyeR, angle, mirror = true)
    }

    // 눈물 글썽(슬픔)
    if (pose.teary) {
        drawCircle(TEAR, radius = d(eyeR * 0.28f), center = p(cx - gap - eyeR * 0.5f, eyeCy + eyeR * 0.9f))
        drawCircle(TEAR, radius = d(eyeR * 0.28f), center = p(cx + gap + eyeR * 0.5f, eyeCy + eyeR * 0.9f))
    }

    // 입 (병아리는 부리로 대체 → 생략)
    if (species != Species.CHICK) {
        val mouthCy = prop.headCy + headR * (0.72f + prop.muzzle * 0.32f)
        drawMouth(p, d, cx, mouthCy, headR, pose.mouth)
    }
}

private fun DrawScope.drawEye(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    center: Offset,
    eyeR: Float,
    pose: FacePose,
    irisColor: Color,
    eyeStyle: Int,
    blink: Float,
    lod: Lod,
    mirror: Boolean,
) {
    // eyeStyle 세로 비율: 0 동그란, 1 아몬드, 2 처진
    val styleAspect = when (eyeStyle) {
        1 -> 0.82f
        2 -> 0.86f
        else -> 1.0f
    }
    val open = ((1f - blink) * (1f - pose.lidTop) * (1f - pose.lidBottom * 0.45f)).coerceIn(0f, 1f)
    val rx = eyeR
    val ryFull = eyeR * styleAspect

    if (open < 0.16f) {
        // 감은 눈: 신남/기쁨이면 위로 볼록한 호(웃는 눈), 아니면 잔잔한 아래 곡선
        val happy = pose.mouth == MouthShape.SMILE || pose.mouth == MouthShape.OPEN
        if (happy) {
            drawArc(
                EYE_DARK, 200f, 140f, false,
                topLeft = Offset(center.x - d(rx), center.y - d(rx * 0.5f)),
                size = Size(d(rx * 2), d(rx)), style = Stroke(d(0.012f)),
            )
        } else {
            drawArc(
                EYE_DARK, 20f, 140f, false,
                topLeft = Offset(center.x - d(rx), center.y - d(rx * 0.5f)),
                size = Size(d(rx * 2), d(rx)), style = Stroke(d(0.011f)),
            )
        }
        return
    }

    val ry = ryFull * open
    if (lod == Lod.FULL) {
        // 홍채 → 동공 → 큰 하이라이트 → 작은 하이라이트
        val irisDark = lerp(irisColor, Color.Black, 0.18f)
        drawOval(irisDark, topLeft = Offset(center.x - d(rx), center.y - d(ry)), size = Size(d(rx * 2), d(ry * 2)))
        val pr = rx * pose.pupilScale
        val prY = ry * pose.pupilScale
        drawOval(PUPIL, topLeft = Offset(center.x - d(pr), center.y - d(prY)), size = Size(d(pr * 2), d(prY * 2)))
        drawCircle(WHITE_HL, radius = d(rx * 0.36f), center = Offset(center.x - d(rx * 0.30f), center.y - d(ry * 0.34f)))
        drawCircle(WHITE_HL.copy(alpha = 0.9f), radius = d(rx * 0.16f), center = Offset(center.x + d(rx * 0.32f), center.y + d(ry * 0.30f)))
        if (pose.extraHighlight) {
            drawCircle(WHITE_HL.copy(alpha = 0.85f), radius = d(rx * 0.12f), center = Offset(center.x, center.y - d(ry * 0.55f)))
        }
    } else {
        drawOval(EYE_DARK, topLeft = Offset(center.x - d(rx), center.y - d(ry)), size = Size(d(rx * 2), d(ry * 2)))
        drawCircle(WHITE_HL, radius = d(rx * 0.34f), center = Offset(center.x - d(rx * 0.28f), center.y - d(ry * 0.32f)))
    }

    // 눈꺼풀 라인: 1=아몬드(바깥 끝 살짝 위로 치켜), 2=처진(바깥 끝 아래로)
    if (eyeStyle != 0) {
        val dir = if (mirror) -1f else 1f // 바깥 방향
        val flick = if (eyeStyle == 1) -0.5f else 0.55f // 위/아래
        drawLine(
            EYE_DARK.copy(alpha = 0.7f),
            Offset(center.x - d(rx * 0.85f) * dir, center.y - d(ry * 0.78f)),
            Offset(center.x + d(rx * 1.0f) * dir, center.y - d(ry * 0.78f) + d(rx * flick)),
            strokeWidth = d(0.009f),
        )
    }
}

private fun DrawScope.drawBrow(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    center: Offset,
    eyeR: Float,
    angleDeg: Float,
    mirror: Boolean,
) {
    // 양수 angle=팔자(안쪽 끝이 위). mirror면 좌우 반전.
    val dir = if (mirror) -1f else 1f
    val innerUp = angleDeg / 90f * eyeR * 0.7f
    val inner = Offset(center.x + d(eyeR * 0.5f) * dir, center.y - d(innerUp))
    val outer = Offset(center.x - d(eyeR * 0.6f) * dir, center.y + d(innerUp * 0.3f))
    drawLine(BROW, inner, outer, strokeWidth = d(0.012f))
}

private fun DrawScope.drawMouth(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    cx: Float,
    cy: Float,
    headR: Float,
    shape: MouthShape,
) {
    val w = headR * 0.5f
    when (shape) {
        MouthShape.SMALL_V -> {
            // 잔잔한 ‿ (작게)
            drawArc(
                MOUTH, 20f, 140f, false,
                topLeft = p(cx - w * 0.4f, cy - headR * 0.12f),
                size = Size(d(w * 0.8f), d(headR * 0.28f)), style = Stroke(d(0.010f)),
            )
        }
        MouthShape.SMILE -> {
            drawArc(
                MOUTH, 20f, 140f, false,
                topLeft = p(cx - w * 0.6f, cy - headR * 0.15f),
                size = Size(d(w * 1.2f), d(headR * 0.5f)), style = Stroke(d(0.013f)),
            )
        }
        MouthShape.OPEN -> {
            drawOval(MOUTH, topLeft = p(cx - w * 0.4f, cy - headR * 0.05f), size = Size(d(w * 0.8f), d(headR * 0.42f)))
            drawArc(
                TONGUE, 0f, 180f, true,
                topLeft = p(cx - w * 0.28f, cy + headR * 0.13f),
                size = Size(d(w * 0.56f), d(headR * 0.22f)),
            )
        }
        MouthShape.YAWN -> {
            drawOval(MOUTH, topLeft = p(cx - w * 0.22f, cy - headR * 0.02f), size = Size(d(w * 0.44f), d(headR * 0.3f)))
        }
        MouthShape.WAVY -> {
            drawArc(
                MOUTH, 200f, 120f, false,
                topLeft = p(cx - w * 0.5f, cy - headR * 0.02f),
                size = Size(d(w * 0.5f), d(headR * 0.22f)), style = Stroke(d(0.010f)),
            )
            drawArc(
                MOUTH, 20f, 120f, false,
                topLeft = p(cx, cy - headR * 0.02f),
                size = Size(d(w * 0.5f), d(headR * 0.22f)), style = Stroke(d(0.010f)),
            )
        }
        MouthShape.FROWN -> {
            drawArc(
                MOUTH, 200f, 140f, false,
                topLeft = p(cx - w * 0.5f, cy + headR * 0.02f),
                size = Size(d(w * 1.0f), d(headR * 0.4f)), style = Stroke(d(0.012f)),
            )
        }
    }
}

/**
 * 옆모습 얼굴(오른쪽 진행 기준): 앞쪽 눈 1개 + 볼터치 1개.
 * 주둥이·코·귀는 종별 side 드로잉이 그린다. 눈썹·눈물은 보행 중 생략.
 */
fun DrawScope.drawSideFace(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    headCx: Float,
    headCy: Float,
    headR: Float,
    pose: FacePose,
    irisColor: Color,
    eyeStyle: Int,
    blink: Float,
    lod: Lod,
) {
    val eyeR = headR * 0.26f
    val center = p(headCx + headR * 0.35f, headCy - headR * 0.08f)
    drawCircle(
        palette.cheek.copy(alpha = 0.45f),
        radius = d(headR * 0.18f),
        center = p(headCx + headR * 0.3f, headCy + headR * 0.32f),
    )
    drawEye(p, d, center, eyeR, pose, irisColor, eyeStyle, blink, lod, mirror = false)
}

/** 종별 홍채 색. */
fun irisFor(species: Species, palette: ModoriPalette): Color = when (species) {
    Species.FOX -> Color.hsl(38f, 0.62f, 0.42f)
    Species.CAT -> Color.hsl(78f, 0.42f, 0.44f)
    Species.RABBIT -> Color.hsl(18f, 0.45f, 0.30f)
    Species.CHICK -> Color(0xFF1A1512)
    Species.DOG -> Color.hsl(25f, 0.5f, 0.28f)
    Species.HAMSTER -> Color(0xFF1E1611)
    Species.PENGUIN -> Color(0xFF14100D)
    Species.PANDA -> Color(0xFF241C16)
    Species.ACORN -> Color(0xFF5A3A1E)
}
