package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Species

private val GLASS = Color(0xFF6B5A44)
private val BANDANA = Color(0xFF4FA9C4)
private val BANDANA_DK = Color(0xFF3C8AA3)
private val LEAF = Color(0xFF6BBF59)
private val LEAF_DK = Color(0xFF4E9E42)
private val FLOWER = Color(0xFFF4A7C0)
private val FLOWER_CTR = Color(0xFFFFD25E)
private val CROWN_LEAF = Color(0xFF57A84A)

/** 진화 분기 표식(성장기2+) + 성숙기 화관. 실사풍에 맞춰 몸에 붙는 소품으로 표현. */
internal fun DrawScope.drawStageAccessories(
    p: (Float, Float) -> Offset,
    d: (Float) -> Float,
    palette: ModoriPalette,
    species: Species,
    stage: GrowthStage,
    branch: Branch?,
    prop: Proportions,
    lod: Lod,
) {
    val fp = if (species == Species.ACORN) acornFaceProp(prop) else prop
    val branchVisible = stage == GrowthStage.GROWTH2 || stage == GrowthStage.MATURE
    if (branchVisible && branch != null) {
        when (branch) {
            Branch.STUDY -> drawGlasses(p, d, fp, lod)
            Branch.HOBBY -> drawBandana(p, d, prop)
            Branch.BALANCED -> drawLeafSprig(p, d, prop)
        }
    }
    if (stage == GrowthStage.MATURE) {
        if (species == Species.ACORN) drawLeafCrown(p, d, prop) else drawFlowerCrown(p, d, prop)
    }
}

private fun DrawScope.drawGlasses(
    p: (Float, Float) -> Offset, d: (Float) -> Float, fp: Proportions, lod: Lod,
) {
    val cx = 0.5f
    val eyeCy = fp.headCy + fp.eyeY * fp.headR
    val gap = fp.eyeGap * fp.headR
    val r = fp.headR * 0.34f * fp.eyeScale
    listOf(-1f, 1f).forEach { s ->
        val c = p(cx + s * gap, eyeCy)
        drawCircle(GLASS, radius = d(r), center = c, style = Stroke(d(0.009f)))
        if (lod == Lod.FULL) {
            drawLine(Color.White.copy(alpha = 0.5f), p(cx + s * gap - r * 0.4f, eyeCy - r * 0.4f), p(cx + s * gap - r * 0.1f, eyeCy - r * 0.1f), strokeWidth = d(0.006f))
        }
    }
    drawLine(GLASS, p(cx - gap + r * 0.7f, eyeCy), p(cx + gap - r * 0.7f, eyeCy), strokeWidth = d(0.009f))
}

private fun DrawScope.drawBandana(p: (Float, Float) -> Offset, d: (Float) -> Float, prop: Proportions) {
    val cx = 0.5f
    val neckY = prop.headCy + prop.headR * 0.95f
    // 목을 감싸는 밴드
    drawArc(BANDANA, 20f, 140f, false, topLeft = p(cx - prop.headR * 0.75f, neckY - prop.headR * 0.35f), size = Size(d(prop.headR * 1.5f), d(prop.headR * 0.7f)), style = Stroke(d(0.05f)))
    // 매듭
    drawCircle(BANDANA_DK, radius = d(prop.headR * 0.14f), center = p(cx - prop.headR * 0.5f, neckY))
    triangle(p(cx - prop.headR * 0.6f, neckY + prop.headR * 0.05f), p(cx - prop.headR * 0.4f, neckY + prop.headR * 0.05f), p(cx - prop.headR * 0.55f, neckY + prop.headR * 0.35f), BANDANA_DK)
}

private fun DrawScope.drawLeafSprig(p: (Float, Float) -> Offset, d: (Float) -> Float, prop: Proportions) {
    val x = 0.5f + prop.headR * 0.75f
    val y = prop.headCy - prop.headR * 0.55f
    rotate(30f, pivot = p(x, y)) {
        drawOval(LEAF, topLeft = p(x, y - 0.06f), size = Size(d(0.055f), d(0.04f)))
        drawOval(LEAF_DK, topLeft = p(x - 0.04f, y - 0.02f), size = Size(d(0.05f), d(0.035f)))
    }
}

private fun DrawScope.drawFlowerCrown(p: (Float, Float) -> Offset, d: (Float) -> Float, prop: Proportions) {
    val cx = 0.5f
    val crownY = prop.headCy - prop.headR * 0.9f
    listOf(-0.55f, 0f, 0.55f).forEach { t ->
        val fx = cx + t * prop.headR
        val fy = crownY + t * t * prop.headR * 0.3f
        // 5꽃잎
        for (i in 0 until 5) {
            val a = i * (2.0 * Math.PI / 5.0)
            drawCircle(FLOWER, radius = d(prop.headR * 0.10f), center = p(fx + prop.headR * 0.11f * kotlin.math.cos(a).toFloat(), fy + prop.headR * 0.11f * kotlin.math.sin(a).toFloat()))
        }
        drawCircle(FLOWER_CTR, radius = d(prop.headR * 0.07f), center = p(fx, fy))
    }
    // 사이 잎
    listOf(-0.28f, 0.28f).forEach { t ->
        drawOval(CROWN_LEAF, topLeft = p(cx + t * prop.headR - 0.02f, crownY + prop.headR * 0.02f), size = Size(d(0.05f), d(0.03f)))
    }
}

private fun DrawScope.drawLeafCrown(p: (Float, Float) -> Offset, d: (Float) -> Float, prop: Proportions) {
    val cx = 0.5f
    // 도토리 나뭇잎 왕관(꼭지 위)
    val cy = 0.58f - prop.bodyRy * 1.12f - 0.05f
    for (i in -2..2) {
        rotate(i * 22f, pivot = p(cx, cy)) {
            drawOval(CROWN_LEAF, topLeft = p(cx - 0.02f, cy - 0.11f), size = Size(d(0.045f), d(0.10f)))
        }
    }
}
