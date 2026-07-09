package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

private val BAMBOO = Color(0xFF7BA05B)
private val BAMBOO_DARK = Color(0xFF5D8443)

/**
 * 판다 "느긋한 대식가".
 * 실루엣: 가장 묵직한 덩치 + 짧은 팔다리 / 컬러: 흑백 대비(유일한 무채색 종) /
 * 시그니처: 기울어진 눈 패치 + 대나무 잎.
 */
internal fun DrawScope.drawPanda(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
) {
    val cx = 0.5f
    val hr = prop.headR * 1.08f
    val hc = prop.headCy
    val black = palette.marking
    val breathe = 1f + motion.breathe * 0.012f

    // 둥근 흑 귀(뒤, 쫑긋 반영)
    val earY = hc - hr * 0.72f
    listOf(-1f, 1f).forEach { s ->
        outlinedCircle(
            p, d, cx + s * hr * 0.62f, earY - (pose.earPerk - 1f) * 0.02f + pose.earDroop * 0.012f,
            hr * 0.32f * (1f + motion.earTwitch * 0.08f), black, palette.outline, OUT_W_S,
        )
    }

    // 흑 뒷발(몸 아래 양옆)
    val bot = prop.bodyCy + prop.bodyRy * 0.92f * breathe
    listOf(-1f, 1f).forEach { s ->
        outlinedOval(p, d, cx + s * prop.bodyRx * 0.62f, bot - 0.012f, 0.075f, 0.048f, black, palette.outline, OUT_W_S)
    }

    // 묵직한 흰 몸통
    outlinedOval(
        p, d, cx, prop.bodyCy, prop.bodyRx * 1.08f, prop.bodyRy * 0.98f * breathe,
        vGrad(p, cx, prop.bodyCy, prop.bodyRy, palette.bodyHighlight, palette.body), palette.outline,
    )

    // 흑 팔(어깨에서 비스듬히)
    val armY = prop.bodyCy - prop.bodyRy * 0.18f
    listOf(-1f, 1f).forEach { s ->
        rotate(s * 26f, pivot = p(cx + s * prop.bodyRx * 0.82f, armY - 0.02f)) {
            outlinedOval(p, d, cx + s * prop.bodyRx * 0.82f, armY + prop.bodyRy * 0.30f, 0.062f, prop.bodyRy * 0.60f, black, palette.outline, OUT_W_S)
        }
    }

    // 대나무(시그니처, 오른팔 근처)
    val bx = cx + prop.bodyRx * 0.96f
    val by2 = armY + prop.bodyRy * 0.12f
    rotate(-22f, pivot = p(bx, by2)) {
        drawLine(BAMBOO_DARK, p(bx, by2 + 0.075f), p(bx, by2 - 0.085f), strokeWidth = d(0.015f))
        drawLine(BAMBOO, p(bx, by2 + 0.07f), p(bx, by2 - 0.08f), strokeWidth = d(0.010f))
        rotate(-38f, pivot = p(bx, by2 - 0.075f)) {
            fillOval(p, d, bx + 0.028f, by2 - 0.075f, 0.030f, 0.011f, BAMBOO)
        }
        rotate(35f, pivot = p(bx, by2 - 0.050f)) {
            fillOval(p, d, bx - 0.028f, by2 - 0.050f, 0.028f, 0.010f, BAMBOO_DARK)
        }
    }

    // 머리(흰) + 아웃라인
    outlinedOval(p, d, cx, hc, hr * 1.08f, hr * 0.94f, vGrad(p, cx, hc, hr, palette.bodyHighlight, palette.body), palette.outline)

    // 기울어진 흑 눈 패치(면 마킹 — drawFace가 이 위에 눈을 얹음)
    val eyeCy = hc + prop.eyeY * prop.headR
    val gap = prop.eyeGap * prop.headR
    val eyeR = prop.headR * 0.30f * prop.eyeScale
    listOf(-1f, 1f).forEach { s ->
        rotate(s * -16f, pivot = p(cx + s * gap, eyeCy)) {
            fillOval(p, d, cx + s * gap, eyeCy + hr * 0.02f, hr * 0.27f, hr * 0.36f, black)
        }
    }
    // 눈 흰자(패치 위에서 눈이 보이도록)
    listOf(-1f, 1f).forEach { s ->
        fillOval(p, d, cx + s * gap, eyeCy, eyeR * 1.18f, eyeR * 1.30f, Color.White)
    }

    // 코 + 짧은 인중
    fillOval(p, d, cx, hc + hr * 0.42f, hr * 0.11f, hr * 0.08f, palette.nose)
}
