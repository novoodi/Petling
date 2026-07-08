package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin

/** 여우: 큰 삼각귀(끝 다크) + 쐐기 주둥이(크림) + 흰끝 풍성 꼬리. */
internal fun DrawScope.drawFox(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
) {
    val cx = 0.5f
    // 꼬리(뒤) — 몸 오른쪽 뒤에서 풍성하게 위로 말림
    val rootX = cx + prop.bodyRx * 0.52f
    val rootY = prop.bodyCy + prop.bodyRy * 0.15f
    val wag = (sin(motion.tailWag * 2 * PI).toFloat() * pose.tailWagAmp * 7f) - pose.tailLift * 6f
    rotate(wag, pivot = p(rootX, rootY)) {
        drawFluffyTail(p, d, rootX, rootY, prop.tailLen * 0.95f + 0.05f, prop.tailThick * 1.15f, palette.body, palette.belly, dir = 1f, bushy = true)
    }

    // 귀(뒤)
    val earCy = prop.headCy - prop.headR * 0.62f
    drawTriEar(p, d, cx - prop.headR * 0.55f, earCy, prop.earLen, prop.earW, palette.body, palette.earInner, palette.marking, -1f, pose.earDroop, pose.earPerk, motion.earTwitch)
    drawTriEar(p, d, cx + prop.headR * 0.55f, earCy, prop.earLen, prop.earW, palette.body, palette.earInner, palette.marking, 1f, pose.earDroop, pose.earPerk, motion.earTwitch)

    drawMammalBody(p, d, palette, prop, motion)
    drawPaws(p, d, palette, prop, lod)

    // 앞발 끝 다크 삭스(FULL)
    if (lod == Lod.FULL && prop.limb > 0f) {
        val cyf = prop.bodyCy + prop.bodyRy * 0.78f
        listOf(-1f, 1f).forEach { s ->
            drawOval(palette.marking.copy(alpha = 0.5f), topLeft = p(cx + s * prop.bodyRx * 0.5f - prop.limb, cyf + prop.limb * 0.2f), size = Size(d(prop.limb * 2), d(prop.limb * 0.7f)))
        }
    }

    // 주둥이 크림 패치
    if (prop.muzzle > 0.12f) {
        val mz = prop.muzzle
        val mcy = prop.headCy + prop.headR * (0.45f + mz * 0.28f)
        drawOval(
            palette.belly,
            topLeft = p(cx - prop.headR * (0.32f + mz * 0.12f), mcy - prop.headR * 0.28f),
            size = Size(d(prop.headR * (0.64f + mz * 0.24f)), d(prop.headR * (0.55f + mz * 0.25f))),
        )
    }
    // 코
    val noseCy = prop.headCy + prop.headR * (0.5f + prop.muzzle * 0.34f)
    drawCircle(palette.nose, radius = d(prop.headR * 0.09f), center = p(cx, noseCy))
    // 콧등 하이라이트
    drawCircle(Color.White.copy(alpha = 0.5f), radius = d(prop.headR * 0.03f), center = p(cx - prop.headR * 0.03f, noseCy - prop.headR * 0.03f))

    // 성숙기 가슴 러프
    if (prop.fluff > 0.6f) {
        listOf(-0.6f, 0f, 0.6f).forEach { t ->
            drawOval(
                palette.belly.copy(alpha = 0.85f),
                topLeft = p(cx + t * prop.bodyRx * 0.4f - prop.bodyRx * 0.22f, prop.bodyCy - prop.bodyRy * 0.35f),
                size = Size(d(prop.bodyRx * 0.44f), d(prop.bodyRy * 0.5f)),
            )
        }
    }
}
