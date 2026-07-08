package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 옆모습(보행) 공통 골격. 오른쪽 진행 기준으로 그린다(좌향은 호출부 scaleX 반전).
 *
 * 걸음질: walkCycle(0..1)로 대각 쌍 트롯 — 근측 앞다리+원측 뒷다리가 함께 앞으로.
 * 원측 다리는 어둡게 칠해 깊이를 표현한다. 다리는 1피벗 스윙 캡슐(무릎 없음).
 */

internal const val SIDE_GROUND = 0.84f

/** 다리 하나: 피벗에서 지면으로 내려오는 캡슐 + 발끝 타원. angleDeg는 진행 방향(+x) 스윙. */
internal fun DrawScope.drawSideLeg(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    pivotX: Float,
    pivotY: Float,
    len: Float,
    thick: Float,
    angleDeg: Float,
    color: Color,
    footColor: Color?,
    lod: Lod,
    outline: Color? = null,
) {
    rotate(angleDeg, pivot = p(pivotX, pivotY)) {
        // 캡슐(둥근 끝 직사각형 근사: 타원)
        drawOval(
            color,
            topLeft = p(pivotX - thick / 2f, pivotY - thick * 0.3f),
            size = Size(d(thick), d(len + thick * 0.5f)),
        )
        outline?.let {
            drawOval(
                it,
                topLeft = p(pivotX - thick / 2f, pivotY - thick * 0.3f),
                size = Size(d(thick), d(len + thick * 0.5f)),
                style = outlineStroke(d, OUT_W_S),
            )
        }
        // 발끝
        if (lod == Lod.FULL && footColor != null) {
            drawOval(
                footColor,
                topLeft = p(pivotX - thick * 0.55f, pivotY + len - thick * 0.35f),
                size = Size(d(thick * 1.25f), d(thick * 0.7f)),
            )
        }
    }
}

/** 대각 트롯 다리 4개 스윙 각도(도). [phi]=0..1. 근측 앞/뒤, 원측 앞/뒤 순. */
internal fun trotAngles(phi: Float, swingDeg: Float): FloatArray {
    val s = sin(phi * 2f * PI).toFloat() * swingDeg
    return floatArrayOf(s, -s, -s, s)
}

/**
 * 4족 공통 코어: 원측 다리 2 → 몸통 캡슐(등선 베지어) → 근측 다리 2 → 머리.
 * 종별 함수가 이 앞뒤로 꼬리/귀/마킹/얼굴을 얹는다.
 */
internal fun DrawScope.drawQuadrupedCore(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    sp: SideProportions,
    phi: Float,
    gait: GaitSpec,
    lod: Lod,
) {
    val angles = trotAngles(phi, gait.swingDeg)
    val farColor = lerp(palette.body, palette.bodyShadow, 0.55f)

    // 원측 다리(앞·뒤)
    drawSideLeg(p, d, sp.shoulderX - 0.012f, sp.pivotY, sp.legLen, sp.legThick, angles[2], farColor, null, lod)
    drawSideLeg(p, d, sp.hipX - 0.012f, sp.pivotY, sp.legLen, sp.legThick, angles[3], farColor, null, lod)

    // 몸통(수평 캡슐 + 어깨 쪽이 살짝 높은 등선)
    val bodyBrush = vGrad(p, sp.bodyCx, sp.bodyCy, sp.bodyHalfHt, palette.bodyHighlight, palette.body)
    val path = scratchPath()
    val l = sp.bodyCx - sp.bodyHalfLen
    val r = sp.bodyCx + sp.bodyHalfLen
    val top = sp.bodyCy - sp.bodyHalfHt
    val bot = sp.bodyCy + sp.bodyHalfHt
    path.moveTo(p(l, sp.bodyCy).x, p(l, sp.bodyCy).y)
    // 등선: 엉덩이→어깨로 갈수록 2~3% 상승
    path.cubicTo(
        p(l, top + 0.015f).x, p(l, top + 0.015f).y,
        p(sp.bodyCx, top - 0.008f).x, p(sp.bodyCx, top - 0.008f).y,
        p(r, top).x, p(r, top).y,
    )
    path.cubicTo(
        p(r + sp.bodyHalfHt * 0.9f, sp.bodyCy - sp.bodyHalfHt * 0.3f).x, p(r + sp.bodyHalfHt * 0.9f, sp.bodyCy - sp.bodyHalfHt * 0.3f).y,
        p(r + sp.bodyHalfHt * 0.9f, sp.bodyCy + sp.bodyHalfHt * 0.4f).x, p(r + sp.bodyHalfHt * 0.9f, sp.bodyCy + sp.bodyHalfHt * 0.4f).y,
        p(r, bot).x, p(r, bot).y,
    )
    path.cubicTo(
        p(sp.bodyCx, bot + 0.01f).x, p(sp.bodyCx, bot + 0.01f).y,
        p(l - sp.bodyHalfHt * 0.6f, bot).x, p(l - sp.bodyHalfHt * 0.6f, bot).y,
        p(l - sp.bodyHalfHt * 0.7f, sp.bodyCy).x, p(l - sp.bodyHalfHt * 0.7f, sp.bodyCy).y,
    )
    path.close()
    drawPath(path, bodyBrush, style = Fill)
    drawPath(path, palette.outline, style = outlineStroke(d))
    // 배 크림
    drawOval(
        palette.belly.copy(alpha = 0.85f),
        topLeft = p(sp.bodyCx - sp.bodyHalfLen * 0.55f, sp.bodyCy + sp.bodyHalfHt * 0.15f),
        size = Size(d(sp.bodyHalfLen * 1.1f), d(sp.bodyHalfHt * 0.85f)),
    )

    // 근측 다리(앞·뒤, 아웃라인)
    drawSideLeg(p, d, sp.shoulderX, sp.pivotY, sp.legLen, sp.legThick, angles[0], palette.body, palette.bodyShadow, lod, palette.outline)
    drawSideLeg(p, d, sp.hipX, sp.pivotY, sp.legLen, sp.legThick, angles[1], palette.body, palette.bodyShadow, lod, palette.outline)

    // 머리(몸과 겹쳐 목 생략) + 아웃라인
    val headBob = -abs(sin(phi * 2f * PI)).toFloat() * gait.bobAmp
    outlinedOval(p, d, sp.headCx, sp.headCy + headBob, sp.headR, sp.headR, vGrad(p, sp.headCx, sp.headCy, sp.headR, palette.bodyHighlight, palette.body), palette.outline)
}

/** 2족 직립 코어(병아리·펭귄): 원측 다리 → 세로 몸통 → 근측 다리. 머리는 몸 상단에 붙음. */
internal fun DrawScope.drawBipedCore(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    sp: SideProportions,
    phi: Float,
    gait: GaitSpec,
    legColor: Color,
    lod: Lod,
) {
    val s = sin(phi * 2f * PI).toFloat() * gait.swingDeg
    val farColor = lerp(legColor, Color.Black, 0.25f)
    drawSideLeg(p, d, sp.bodyCx - 0.02f, sp.pivotY, sp.legLen, sp.legThick, -s, farColor, null, lod)
    // 몸통(세로 캡슐 + 아웃라인)
    outlinedOval(
        p, d, sp.bodyCx, sp.bodyCy, sp.bodyHalfLen, sp.bodyHalfHt,
        vGrad(p, sp.bodyCx, sp.bodyCy, sp.bodyHalfHt, palette.bodyHighlight, palette.body), palette.outline,
    )
    drawSideLeg(p, d, sp.bodyCx + 0.015f, sp.pivotY, sp.legLen, sp.legThick, s, legColor, null, lod, palette.outline)
}

/**
 * 토끼 홉 프로파일. [phi] 0..1 중 앞 55%가 체공(아치), 나머지는 착지 스탠스.
 * @return dy(음수=공중), legExtend(-1 웅크림..+1 신전), pitchDeg(이륙+/착지-)
 */
internal fun hopProfile(phi: Float): Triple<Float, Float, Float> {
    return if (phi < 0.55f) {
        val t = phi / 0.55f
        val arc = sin(t * PI).toFloat()
        Triple(-0.055f * arc, (0.5f - abs(t - 0.5f)) * 2f, (0.5f - t) * 16f)
    } else {
        Triple(0f, -0.6f, 0f)
    }
}
