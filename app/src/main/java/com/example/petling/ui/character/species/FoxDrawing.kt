package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.petling.domain.model.Species
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

/** 여우 옆모습(보행): 풍성한 수평 꼬리(흰 끝) + 삼각귀 + 쐐기 주둥이 + 다크 삭스. */
internal fun DrawScope.drawFoxSide(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    sp: SideProportions,
    pose: FacePose,
    motion: CreatureMotion,
    eyeStyle: Int,
    blink: Float,
    lod: Lod,
) {
    val phi = motion.walkCycle
    val gait = gaitFor(Species.FOX)

    // 꼬리(맨 뒤): 몸 뒤로 수평, 끝이 살짝 들리고 walkCycle에 맞춰 출렁
    val rearX = sp.bodyCx - sp.bodyHalfLen * 0.9f
    val tailLift = sin(motion.tailWag * 2 * PI).toFloat() * 0.02f + pose.tailLift * 0.02f
    val n = 9
    for (i in 0..n) {
        val t = i / n.toFloat()
        val tx = rearX - t * 0.20f
        val ty = sp.bodyCy - t * (0.05f + tailLift)
        val r = 0.045f * (0.7f + 0.6f * sin((t * Math.PI).toFloat()))
        val col = if (t > 0.75f) palette.belly else palette.body
        drawCircle(col, radius = d(r), center = p(tx, ty))
    }

    drawQuadrupedCore(p, d, palette, sp, phi, gait, lod)

    // 다크 삭스(근측 발 강조는 core의 footColor 대신 marking으로 이미 어두움 — 발끝 위 살짝)
    // 귀(머리 위 삼각)
    val earBase = sp.headCy - sp.headR * 0.72f
    triangle(
        p(sp.headCx - sp.headR * 0.15f, earBase + 0.02f),
        p(sp.headCx + sp.headR * 0.45f, earBase + 0.02f),
        p(sp.headCx + sp.headR * (0.15f - pose.earBack * 0.3f), earBase - sp.headR * (0.85f * (1f - pose.earDroop * 0.6f))),
        palette.body,
    )
    triangle(
        p(sp.headCx - sp.headR * 0.02f, earBase + 0.01f),
        p(sp.headCx + sp.headR * 0.32f, earBase + 0.01f),
        p(sp.headCx + sp.headR * 0.13f, earBase - sp.headR * 0.55f),
        palette.marking,
    )

    // 주둥이(머리 앞 쐐기 + 코)
    val mzX = sp.headCx + sp.headR * 0.9f
    val mzY = sp.headCy + sp.headR * 0.18f
    drawOval(palette.belly, topLeft = p(sp.headCx + sp.headR * 0.35f, mzY - sp.headR * 0.28f), size = Size(d(sp.headR * 0.75f), d(sp.headR * 0.5f)))
    drawCircle(palette.nose, radius = d(sp.headR * 0.10f), center = p(mzX + sp.headR * 0.08f, mzY - sp.headR * 0.06f))

    drawSideFace(p, d, palette, sp.headCx, sp.headCy, sp.headR, pose, irisFor(Species.FOX, palette), eyeStyle, blink, lod)
}
