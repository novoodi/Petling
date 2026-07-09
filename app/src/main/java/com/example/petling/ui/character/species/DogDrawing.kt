package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin

private val TAG_GOLD = Color(0xFFF2C14E)

/**
 * 강아지 "해맑은 단짝".
 * 실루엣: 축 처진 펄럭 귀 + 말린 꼬리 / 컬러: 탄(황갈)+초콜릿 귀·눈 패치 /
 * 시그니처: 목걸이(사용자 colorHue 액센트) + 한쪽 눈 패치.
 */
internal fun DrawScope.drawDog(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    prop: Proportions,
    pose: FacePose,
    motion: CreatureMotion,
    lod: Lod,
) {
    val cx = 0.5f
    val hr = prop.headR * 1.06f
    val hc = prop.headCy

    // 말린 꼬리(뒤): 몸 오른쪽 위에 붙은 작은 나선 아크, 살랑
    val wag = sin(motion.tailWag * 2 * PI).toFloat() * pose.tailWagAmp * 12f
    val tRootX = cx + prop.bodyRx * 0.72f
    val tRootY = prop.bodyCy - prop.bodyRy * 0.28f
    rotate(wag, pivot = p(tRootX, tRootY)) {
        val r = prop.tailThick * 1.0f + 0.006f
        val c = p(tRootX + r * 0.5f, tRootY - r * 0.55f)
        drawStrokeTail(d, prop.tailThick * 1.05f, palette.body, palette.outline) { path ->
            path.arcTo(
                Rect(c.x - d(r), c.y - d(r), c.x + d(r), c.y + d(r)),
                startAngleDegrees = 150f, sweepAngleDegrees = -300f, forceMoveTo = true,
            )
        }
    }

    // 앉은 몸통 + 앞발
    drawSittingBody(p, d, palette, prop, motion, widthMul = 0.92f)
    drawSitPaws(p, d, palette, prop, color = palette.body)

    // 목걸이(액센트) + 금색 태그 — 머리 아래 가슴 위에 보이도록
    val bandTop = prop.headCy + prop.headR * 1.06f * 0.92f
    val bandW = prop.bodyRx * 0.58f
    val sag = prop.bodyRy * 0.24f
    val bandH = prop.bodyRy * 0.18f
    val band = scratchPath()
    band.moveTo(p(cx - bandW, bandTop).x, p(cx - bandW, bandTop).y)
    band.quadraticBezierTo(p(cx, bandTop + sag).x, p(cx, bandTop + sag).y, p(cx + bandW, bandTop).x, p(cx + bandW, bandTop).y)
    band.lineTo(p(cx + bandW, bandTop + bandH).x, p(cx + bandW, bandTop + bandH).y)
    band.quadraticBezierTo(p(cx, bandTop + sag + bandH).x, p(cx, bandTop + sag + bandH).y, p(cx - bandW, bandTop + bandH).x, p(cx - bandW, bandTop + bandH).y)
    band.close()
    outlinedPath(d, band, palette.accent, palette.outline, OUT_W_S)
    outlinedCircle(p, d, cx, bandTop + sag + bandH + 0.012f, 0.020f, TAG_GOLD, palette.outline, OUT_W_S * 0.9f)

    // 펄럭 귀(뒤→머리가 위를 덮음): 초콜릿 아웃라인 타원, 처짐/쫑긋 반영
    val earPivY = hc - hr * 0.44f
    listOf(-1f, 1f).forEach { s ->
        val px = cx + s * hr * 0.76f
        rotate(s * (26f + pose.earDroop * 14f - (pose.earPerk - 1f) * 36f) + motion.earTwitch * s * 5f, pivot = p(px, earPivY)) {
            outlinedOval(p, d, px + s * prop.earLen * 0.14f, earPivY + prop.earLen * 0.80f, prop.earLen * 0.44f, prop.earLen * 1.02f, palette.marking, palette.outline, OUT_W_S)
        }
    }

    // 머리 + 아웃라인
    outlinedOval(p, d, cx, hc, hr * 1.05f, hr * 0.96f, vGrad(p, cx, hc, hr, palette.bodyHighlight, palette.body), palette.outline)

    // 오른눈 초콜릿 패치(면 마킹 — 이후 drawFace가 이 위에 눈을 얹음)
    val eyeCy = prop.headCy + prop.eyeY * prop.headR
    val gap = prop.eyeGap * prop.headR
    drawOval(
        palette.marking.copy(alpha = 0.92f),
        topLeft = p(cx + gap - hr * 0.30f, eyeCy - hr * 0.34f),
        size = Size(d(hr * 0.60f), d(hr * 0.68f)),
    )

    // 주둥이 크림 + 큰 코
    drawOval(
        palette.belly,
        topLeft = p(cx - hr * 0.40f, hc + hr * 0.30f),
        size = Size(d(hr * 0.80f), d(hr * 0.58f)),
    )
    fillOval(p, d, cx, hc + hr * 0.44f, hr * 0.15f, hr * 0.11f, palette.nose)
}
