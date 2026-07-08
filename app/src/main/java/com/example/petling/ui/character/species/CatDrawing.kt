package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin

private val CAT_NOSE = Color(0xFFE58AA0)

/** 고양이: 둥근 삼각귀(속귀 핑크) + 분홍 삼각코 + 이마 M 줄무늬 + 가늘고 긴 꼬리. */
internal fun DrawScope.drawCat(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
) {
    val cx = 0.5f
    // 가늘고 긴 꼬리(뒤, 몸 앞으로 감김)
    val rootX = cx + prop.bodyRx * 0.72f
    val rootY = prop.bodyCy + prop.bodyRy * 0.35f
    val wag = (sin(motion.tailWag * 2 * PI).toFloat() * pose.tailWagAmp * 11f) - pose.tailLift * 10f
    rotate(wag, pivot = p(rootX, rootY)) {
        drawFluffyTail(p, d, rootX, rootY, prop.tailLen + 0.04f, prop.tailThick, palette.body, null, dir = 1f, bushy = false)
        // 꼬리 링(FULL·성숙)
        if (lod == Lod.FULL && prop.fluff > 0.5f) {
            listOf(0.4f, 0.7f).forEach { t ->
                val ty = rootY - (prop.tailLen + 0.06f) * t
                drawLine(palette.marking.copy(alpha = 0.6f), p(rootX + prop.tailThick * 0.6f, ty), p(rootX + prop.tailThick * 1.6f, ty - 0.01f), strokeWidth = d(prop.tailThick * 0.6f))
            }
        }
    }

    // 귀(둥근 삼각)
    val earCy = prop.headCy - prop.headR * 0.6f
    drawTriEar(p, d, cx - prop.headR * 0.52f, earCy, prop.earLen, prop.earW, palette.body, palette.earInner, null, -1f, pose.earDroop, pose.earPerk, motion.earTwitch)
    drawTriEar(p, d, cx + prop.headR * 0.52f, earCy, prop.earLen, prop.earW, palette.body, palette.earInner, null, 1f, pose.earDroop, pose.earPerk, motion.earTwitch)

    drawMammalBody(p, d, palette, prop, motion)
    drawPaws(p, d, palette, prop, lod)

    // 이마 M 줄무늬(FULL)
    if (lod == Lod.FULL) {
        val topY = prop.headCy - prop.headR * 0.55f
        listOf(-0.16f, 0f, 0.16f).forEach { dx ->
            drawLine(palette.marking.copy(alpha = 0.55f), p(cx + dx, topY), p(cx + dx * 0.7f, topY + prop.headR * 0.28f), strokeWidth = d(0.010f))
        }
    }

    // 코(분홍 삼각)
    val noseCy = prop.headCy + prop.headR * (0.5f + prop.muzzle * 0.2f)
    triangle(p(cx - prop.headR * 0.09f, noseCy - prop.headR * 0.05f), p(cx + prop.headR * 0.09f, noseCy - prop.headR * 0.05f), p(cx, noseCy + prop.headR * 0.06f), CAT_NOSE)

    // 수염(FULL)
    if (lod == Lod.FULL) {
        val w = palette.marking.copy(alpha = 0.4f)
        val wy = noseCy + prop.headR * 0.02f
        listOf(-0.03f, 0f, 0.03f).forEach { dy ->
            drawLine(w, p(cx - prop.headR * 0.18f, wy + dy), p(cx - prop.headR * 0.9f, wy + dy * 1.8f - 0.005f), strokeWidth = d(0.004f))
            drawLine(w, p(cx + prop.headR * 0.18f, wy + dy), p(cx + prop.headR * 0.9f, wy + dy * 1.8f - 0.005f), strokeWidth = d(0.004f))
        }
    }
}
