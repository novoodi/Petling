package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.petling.domain.model.Species

/** 판다 정면: 흰 몸 + 흑 귀·팔·눈 패치(마킹 고정). 눈 패치는 얼굴보다 먼저 그려 눈이 위에 얹힘. */
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
    val black = palette.marking

    // 둥근 흑 귀
    val earY = prop.headCy - prop.headR * 0.68f
    listOf(-1f, 1f).forEach { s ->
        drawCircle(black, radius = d(prop.headR * 0.30f), center = p(cx + s * prop.headR * 0.58f, earY))
    }

    drawMammalBody(p, d, palette, prop, motion)

    // 어깨띠(가슴 위 흑 밴드, FULL)
    if (lod == Lod.FULL) {
        drawOval(
            black.copy(alpha = 0.85f),
            topLeft = p(cx - prop.bodyRx * 0.85f, prop.bodyCy - prop.bodyRy * 0.75f),
            size = Size(d(prop.bodyRx * 1.7f), d(prop.bodyRy * 0.5f)),
        )
    }
    // 흑 팔(몸 하단 양옆)
    val armY = prop.bodyCy + prop.bodyRy * 0.35f
    listOf(-1f, 1f).forEach { s ->
        drawOval(
            black,
            topLeft = p(cx + s * prop.bodyRx * 0.62f - 0.05f, armY - 0.045f),
            size = Size(d(0.10f), d(0.09f)),
        )
    }

    // 흑 눈 패치(얼굴 눈보다 먼저 — drawFace가 이 위에 눈을 얹음)
    val eyeY = prop.headCy + prop.eyeY * prop.headR
    val gap = prop.eyeGap * prop.headR
    listOf(-1f, 1f).forEach { s ->
        drawOval(
            black,
            topLeft = p(cx + s * gap - prop.headR * 0.24f, eyeY - prop.headR * 0.26f),
            size = Size(d(prop.headR * 0.48f), d(prop.headR * 0.55f)),
        )
    }

    // 코
    drawCircle(palette.nose, radius = d(prop.headR * 0.09f), center = p(cx, prop.headCy + prop.headR * 0.5f))
}

/** 판다 옆모습(느릿 보행): 흰 몸 + 흑 다리·귀·어깨띠, 큰 몸 흔들. */
internal fun DrawScope.drawPandaSide(
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
    val gait = gaitFor(Species.PANDA)
    val black = palette.marking

    // 판다는 다리가 흑색이라 4족 코어를 쓰지 않고 직접 그린다.
    val angles = trotAngles(phi, gait.swingDeg)
    drawSideLeg(p, d, sp.shoulderX - 0.012f, sp.pivotY, sp.legLen, sp.legThick, angles[2], black.copy(alpha = 0.75f), null, lod)
    drawSideLeg(p, d, sp.hipX - 0.012f, sp.pivotY, sp.legLen, sp.legThick, angles[3], black.copy(alpha = 0.75f), null, lod)

    // 몸통(흰) — 코어의 몸통 부분만 직접
    fillOval(p, d, sp.bodyCx, sp.bodyCy, sp.bodyHalfLen, sp.bodyHalfHt, vGrad(p, sp.bodyCx, sp.bodyCy, sp.bodyHalfHt, palette.bodyHighlight, palette.body))
    // 어깨띠(흑)
    if (lod == Lod.FULL) {
        drawOval(
            black.copy(alpha = 0.85f),
            topLeft = p(sp.bodyCx + sp.bodyHalfLen * 0.25f, sp.bodyCy - sp.bodyHalfHt * 1.0f),
            size = Size(d(sp.bodyHalfLen * 0.55f), d(sp.bodyHalfHt * 2.0f)),
        )
    }

    // 근측 다리(흑)
    drawSideLeg(p, d, sp.shoulderX, sp.pivotY, sp.legLen, sp.legThick, angles[0], black, null, lod)
    drawSideLeg(p, d, sp.hipX, sp.pivotY, sp.legLen, sp.legThick, angles[1], black, null, lod)

    // 머리(흰) + 흑 귀
    fillOval(p, d, sp.headCx, sp.headCy, sp.headR, sp.headR, vGrad(p, sp.headCx, sp.headCy, sp.headR, palette.bodyHighlight, palette.body))
    drawCircle(black, radius = d(sp.headR * 0.28f), center = p(sp.headCx - sp.headR * 0.35f, sp.headCy - sp.headR * 0.7f))

    // 흑 눈 패치(눈보다 먼저)
    drawOval(
        black,
        topLeft = p(sp.headCx + sp.headR * 0.12f, sp.headCy - sp.headR * 0.32f),
        size = Size(d(sp.headR * 0.45f), d(sp.headR * 0.5f)),
    )

    // 코
    drawCircle(palette.nose, radius = d(sp.headR * 0.09f), center = p(sp.headCx + sp.headR * 0.85f, sp.headCy + sp.headR * 0.15f))

    drawSideFace(p, d, palette, sp.headCx, sp.headCy, sp.headR, pose, irisFor(Species.PANDA, palette), eyeStyle, blink, lod)
}
