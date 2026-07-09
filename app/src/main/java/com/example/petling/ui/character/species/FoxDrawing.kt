package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/**
 * 여우 "새침한 탐험가".
 * 실루엣: 뾰족한 볼·큰 삼각귀·몸집만 한 플룸 꼬리 / 컬러: 진주황+크림 가슴+다크 삭스 /
 * 시그니처: 끝이 하얀 풍성한 꼬리.
 */
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
    val hr = prop.headR * 1.12f // 여우는 볼이 옆으로 넓은 머리
    val hc = prop.headCy

    // 꼬리(맨 뒤): 몸 오른쪽에서 옆으로 크게 뻗는 플룸, 흰 끝(실루엣 시그니처)
    val rootX = cx + prop.bodyRx * 0.78f
    val rootY = prop.bodyCy + prop.bodyRy * 0.46f
    val wag = sin(motion.tailWag * 2 * PI).toFloat() * pose.tailWagAmp * 6f - pose.tailLift * 5f
    drawPlumeTail(
        p, d, rootX, rootY,
        len = prop.tailLen * 1.5f + 0.03f, thick = prop.tailThick * 1.1f,
        color = palette.body, tipColor = palette.belly, outline = palette.outline,
        angleDeg = -44f + wag,
    )

    // 큰 삼각귀(뒤): 다크 팁 + 크림 속귀 + 아웃라인
    val earCy = hc - hr * 0.58f
    val earLen = prop.earLen * 1.25f
    drawTriEar(p, d, cx - hr * 0.58f, earCy, earLen, prop.earW, palette.body, palette.earInner, palette.marking, -1f, pose.earDroop, pose.earPerk, motion.earTwitch, palette.outline)
    drawTriEar(p, d, cx + hr * 0.58f, earCy, earLen, prop.earW, palette.body, palette.earInner, palette.marking, 1f, pose.earDroop, pose.earPerk, motion.earTwitch, palette.outline)

    // 앉은 몸통 + 다크 삭스 앞발
    drawSittingBody(p, d, palette, prop, motion, widthMul = 0.88f)
    drawSitPaws(p, d, palette, prop, color = palette.marking)

    // 머리: 볼이 뾰족한 쐐기형(위 둥긂 → 볼 최광 → 턱 끝점)
    val head = scratchPath()
    fun m(x: Float, y: Float) = p(cx + x * hr, hc + y * hr)
    head.moveTo(m(-1.02f, 0.10f).x, m(-1.02f, 0.10f).y)                       // 왼볼 끝
    head.cubicTo(m(-0.98f, -0.52f).x, m(-0.98f, -0.52f).y, m(-0.55f, -0.90f).x, m(-0.55f, -0.90f).y, m(0f, -0.90f).x, m(0f, -0.90f).y)
    head.cubicTo(m(0.55f, -0.90f).x, m(0.55f, -0.90f).y, m(0.98f, -0.52f).x, m(0.98f, -0.52f).y, m(1.02f, 0.10f).x, m(1.02f, 0.10f).y) // 오른볼 끝
    head.cubicTo(m(0.92f, 0.62f).x, m(0.92f, 0.62f).y, m(0.45f, 1.02f).x, m(0.45f, 1.02f).y, m(0f, 1.10f).x, m(0f, 1.10f).y)           // 턱 끝
    head.cubicTo(m(-0.45f, 1.02f).x, m(-0.45f, 1.02f).y, m(-0.92f, 0.62f).x, m(-0.92f, 0.62f).y, m(-1.02f, 0.10f).x, m(-1.02f, 0.10f).y)
    head.close()
    outlinedPath(d, head, vGrad(p, cx, hc, hr, palette.bodyHighlight, palette.body), palette.outline)

    // 주둥이 크림 패치(면 마킹)
    drawOval(
        palette.belly,
        topLeft = p(cx - hr * 0.50f, hc + hr * 0.26f),
        size = Size(d(hr * 1.00f), d(hr * 0.74f)),
    )

    // 코: 다크 라운드 삼각
    val noseCy = hc + hr * 0.48f
    triangle(
        p(cx - hr * 0.10f, noseCy - hr * 0.04f),
        p(cx + hr * 0.10f, noseCy - hr * 0.04f),
        p(cx, noseCy + hr * 0.09f),
        palette.nose,
    )
}
