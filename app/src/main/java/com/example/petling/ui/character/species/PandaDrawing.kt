package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.petling.domain.model.Species

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

    // 몸통(흰 + 아웃라인) — 코어의 몸통 부분만 직접
    outlinedOval(p, d, sp.bodyCx, sp.bodyCy, sp.bodyHalfLen, sp.bodyHalfHt, vGrad(p, sp.bodyCx, sp.bodyCy, sp.bodyHalfHt, palette.bodyHighlight, palette.body), palette.outline)
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

    // 머리(흰 + 아웃라인) + 흑 귀
    outlinedOval(p, d, sp.headCx, sp.headCy, sp.headR, sp.headR, vGrad(p, sp.headCx, sp.headCy, sp.headR, palette.bodyHighlight, palette.body), palette.outline)
    outlinedCircle(p, d, sp.headCx - sp.headR * 0.35f, sp.headCy - sp.headR * 0.7f, sp.headR * 0.28f, black, palette.outline, OUT_W_S)

    // 흑 눈 패치(눈보다 먼저) + 흰자
    drawOval(
        black,
        topLeft = p(sp.headCx + sp.headR * 0.12f, sp.headCy - sp.headR * 0.32f),
        size = Size(d(sp.headR * 0.45f), d(sp.headR * 0.5f)),
    )
    fillOval(p, d, sp.headCx + sp.headR * 0.35f, sp.headCy - sp.headR * 0.08f, sp.headR * 0.20f, sp.headR * 0.22f, Color.White)

    // 코
    drawCircle(palette.nose, radius = d(sp.headR * 0.09f), center = p(sp.headCx + sp.headR * 0.85f, sp.headCy + sp.headR * 0.15f))

    drawSideFace(p, d, palette, sp.headCx, sp.headCy, sp.headR, pose, irisFor(Species.PANDA, palette), eyeStyle, blink, lod)
}
