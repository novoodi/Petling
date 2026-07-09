package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

private val SEED = Color(0xFF6B5138)
private val SEED_STRIPE = Color(0xFFF3E3C6)

/**
 * 햄스터 "먹보 수집가".
 * 실루엣: 세로보다 가로로 퍼진 유일한 땅딸보 한 덩어리 / 컬러: 골든+크림 배 /
 * 시그니처: 빵빵한 볼주머니 + 해바라기씨 들고 있기.
 */
internal fun DrawScope.drawHamster(
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
    val top = hc - hr * 1.0f
    val bot = prop.bodyCy + prop.bodyRy * 0.95f * breathe
    val halfW = prop.bodyRx * 1.38f

    // 작은 귀(뒤): 아웃라인 원 + 핑크 속귀, 쫑긋 반영
    listOf(-1f, 1f).forEach { s ->
        val ex = cx + s * hr * 0.58f
        val ey = top + 0.012f - (pose.earPerk - 1f) * 0.02f + pose.earDroop * 0.015f
        outlinedCircle(p, d, ex, ey, prop.earLen * (1f + motion.earTwitch * 0.12f), palette.body, palette.outline, OUT_W_S)
        drawCircle(palette.earInner, radius = d(prop.earLen * 0.52f), center = p(ex, ey + prop.earLen * 0.08f))
    }

    // 한 덩어리 블롭 몸통(가로로 퍼짐)
    drawEggBody(p, d, top, bot, halfW, vGrad(p, cx, (top + bot) / 2f, (bot - top) / 2f, palette.bodyHighlight, palette.body), palette.outline)

    // 볼주머니(양옆 불룩, 성장할수록 빵빵 — 면 마킹)
    val cheekR = hr * (0.34f + prop.fluff * 0.14f)
    listOf(-1f, 1f).forEach { s ->
        drawOval(
            palette.bodyHighlight.copy(alpha = 0.85f),
            topLeft = p(cx + s * halfW * 0.68f - cheekR, hc + hr * 0.42f - cheekR * 0.85f),
            size = Size(d(cheekR * 2f), d(cheekR * 1.7f)),
        )
    }

    // 배 크림(면 마킹)
    drawOval(
        palette.belly,
        topLeft = p(cx - halfW * 0.52f, bot - (bot - top) * 0.40f),
        size = Size(d(halfW * 1.04f), d((bot - top) * 0.33f)),
    )

    // 코(분홍) + 앞니
    val noseCy = hc + hr * 0.44f
    triangle(
        p(cx - hr * 0.07f, noseCy - hr * 0.03f),
        p(cx + hr * 0.07f, noseCy - hr * 0.03f),
        p(cx, noseCy + hr * 0.05f),
        palette.nose,
    )
    val toothW = hr * 0.13f
    val toothTop = noseCy + hr * 0.13f
    drawRoundRect(
        color = Color.White,
        topLeft = p(cx - toothW / 2f, toothTop),
        size = Size(d(toothW), d(hr * 0.11f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(d(toothW * 0.3f)),
    )
    drawLine(palette.outline.copy(alpha = 0.7f), p(cx, toothTop), p(cx, toothTop + hr * 0.10f), strokeWidth = d(0.004f))

    // 해바라기씨(시그니처) + 두 손 모으기
    val handY = prop.bodyCy - prop.bodyRy * 0.02f
    rotate(18f, pivot = p(cx, handY)) {
        outlinedOval(p, d, cx, handY, 0.023f, 0.036f, SEED, palette.outline, OUT_W_S * 0.8f)
        drawLine(SEED_STRIPE, p(cx, handY - 0.026f), p(cx, handY + 0.026f), strokeWidth = d(0.005f))
    }
    listOf(-1f, 1f).forEach { s ->
        outlinedOval(p, d, cx + s * 0.042f, handY + 0.010f, 0.026f, 0.020f, palette.body, palette.outline, OUT_W_S * 0.8f)
    }

    // 아기 발(몸 아래 빼꼼)
    listOf(-1f, 1f).forEach { s ->
        outlinedOval(p, d, cx + s * halfW * 0.34f, bot - 0.008f, 0.032f, 0.018f, palette.bodyHighlight, palette.outline, OUT_W_S * 0.8f)
    }
}
