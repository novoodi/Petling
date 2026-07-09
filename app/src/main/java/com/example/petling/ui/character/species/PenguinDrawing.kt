package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin

private val P_BEAK = Color(0xFFF2A03D)
private val P_FEET = Color(0xFFE8933A)
private val BROW_GOLD = Color(0xFFF2C14E)

/**
 * 펭귄 "진지한 뒤뚱이".
 * 실루엣: 유일한 직립 물방울 + 양옆 플리퍼 / 컬러: 네이비+화이트+주황 부리·발 /
 * 시그니처: 노란 눈썹 깃털(로크호퍼).
 */
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
    val hr = prop.headR
    val hc = prop.headCy
    val breathe = 1f + motion.breathe * 0.012f
    val top = hc - hr * 1.05f
    val bot = prop.bodyCy + prop.bodyRy * 0.92f * breathe
    val halfW = prop.bodyRx * 1.12f

    // 주황 발(몸 아래 빼꼼)
    listOf(-1f, 1f).forEach { s ->
        outlinedOval(p, d, cx + s * halfW * 0.34f, bot + 0.012f, 0.042f, 0.017f, P_FEET, palette.outline, OUT_W_S)
    }

    // 플리퍼(몸 옆, 뒤): 아웃라인 타원, tailWag 위상으로 파닥
    val flap = sin(motion.tailWag * 2 * PI).toFloat() * pose.tailWagAmp * 5f
    val flipY = bot - (bot - top) * 0.42f
    listOf(-1f, 1f).forEach { s ->
        rotate(s * (24f + flap), pivot = p(cx + s * halfW * 0.88f, flipY - prop.bodyRy * 0.35f)) {
            outlinedOval(p, d, cx + s * halfW * 0.88f, flipY, 0.033f, prop.bodyRy * 0.62f, palette.body, palette.outline, OUT_W_S)
        }
    }

    // 직립 물방울 몸통(네이비)
    drawEggBody(p, d, top, bot, halfW, vGrad(p, cx, (top + bot) / 2f, (bot - top) / 2f, palette.bodyHighlight, palette.body), palette.outline)

    // 큰 흰 배(면 마킹)
    val bellyTop = hc + hr * 0.38f
    fillOval(p, d, cx, (bellyTop + bot - 0.015f) / 2f, halfW * 0.64f, (bot - 0.015f - bellyTop) / 2f, palette.belly)

    // 눈 흰자(다크 몸 위에서 눈이 보이도록 — drawFace가 이 위에 홍채를 얹음)
    val eyeCy = hc + prop.eyeY * hr
    val gap = prop.eyeGap * hr
    val eyeR = hr * 0.30f * prop.eyeScale
    listOf(-1f, 1f).forEach { s ->
        fillOval(p, d, cx + s * gap, eyeCy, eyeR * 1.30f, eyeR * 1.45f, palette.belly)
    }

    // 노란 눈썹 깃털(시그니처)
    listOf(-1f, 1f).forEach { s ->
        drawLine(
            BROW_GOLD,
            p(cx + s * gap * 0.75f, eyeCy - eyeR * 1.8f),
            p(cx + s * (gap + eyeR * 1.6f), eyeCy - eyeR * 2.6f),
            strokeWidth = d(0.014f),
        )
        drawLine(
            BROW_GOLD.copy(alpha = 0.8f),
            p(cx + s * gap * 0.85f, eyeCy - eyeR * 1.55f),
            p(cx + s * (gap + eyeR * 1.75f), eyeCy - eyeR * 1.9f),
            strokeWidth = d(0.010f),
        )
    }

    // 부리(주황 다이아몬드 + 아웃라인)
    val beakCy = hc + hr * 0.44f
    val bw = hr * 0.17f
    val beak = scratchPath()
    val l = p(cx - bw, beakCy); val r = p(cx + bw, beakCy)
    val t = p(cx, beakCy - hr * 0.13f); val b = p(cx, beakCy + hr * 0.14f)
    beak.moveTo(l.x, l.y); beak.lineTo(t.x, t.y); beak.lineTo(r.x, r.y); beak.lineTo(b.x, b.y); beak.close()
    drawPath(beak, P_BEAK)
    drawPath(beak, palette.outline, style = outlineStroke(d, OUT_W_S))
}
