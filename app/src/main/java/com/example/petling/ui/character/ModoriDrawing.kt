package com.example.petling.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Mood
import com.example.petling.domain.model.Species

/** 캐릭터가 서는 바닥선(정규화 y). 종별 체급 스케일과 바닥 그림자의 앵커. */
const val GROUND_Y = 0.87f

/**
 * 스타팅 동물 벡터 드로잉의 진입점(디스패처).
 *
 * 좌표는 정사각 캔버스 기준 정규화 비율(0~1)이라 크기와 무관하게 동일하게 그려진다.
 * 실제 그림은 종별 파일(species 폴더) + 공통 얼굴([CreatureFace])·액세서리([CreatureAccessories])
 * ·이펙트([CreatureEffects])로 분리돼 있고, 성장은 [proportionsFor] 비율 테이블이,
 * 표정은 [poseFor] 신체언어 테이블이, 미세 모션은 [CreatureMotion]이 담당한다.
 *
 * 시그니처는 하위호환을 위해 기존 파라미터 순서를 유지하고, [motion]만 기본값으로 뒤에 추가했다.
 */
fun DrawScope.drawCreature(
    species: Species,
    stage: GrowthStage,
    branch: Branch?,
    mood: Mood,
    expression: Expression,
    palette: ModoriPalette,
    eyeStyle: Int,
    blink: Float = 0f,
    crackProgress: Float = 0f,
    hatchAlpha: Float = 1f,
    motion: CreatureMotion = CreatureMotion.STATIC,
    traits: IndividualTraits = IndividualTraits.NEUTRAL,
) {
    val s = size.minDimension
    val origin = Offset((size.width - s) / 2f, (size.height - s) / 2f)
    // 종별 체급: 캔버스 중앙이 아니라 바닥선(GROUND_Y) 기준으로 스케일해 같은 지면에 서게 한다.
    // 개체 시드의 sizeJitter를 체급에 곱해 미세한 크기차를 준다(코트·실루엣 불변).
    val k = bulkFor(species, stage) * traits.sizeJitter
    fun p(x: Float, y: Float) = Offset(
        origin.x + (0.5f + (x - 0.5f) * k) * s,
        origin.y + (GROUND_Y - (GROUND_Y - y) * k) * s,
    )
    fun d(v: Float) = v * k * s
    val lod = lodFor(s)

    // 바닥 그림자(체급 비례 — p()/d()가 바닥선 기준으로 자동 정합)
    drawOval(
        color = Color(0x22000000),
        topLeft = p(0.30f, GROUND_Y),
        size = Size(d(0.40f), d(0.07f)),
    )

    if (stage == GrowthStage.EGG) {
        drawEgg(::p, ::d, palette, crackProgress, hatchAlpha)
        return
    }

    val pose = poseFor(expression)

    val prop = proportionsFor(species, stage, traits)
    val iris = irisFor(species, palette)

    when (species) {
        Species.FOX -> drawFox(::p, ::d, palette, prop, pose, motion, lod)
        Species.CAT -> drawCat(::p, ::d, palette, prop, pose, motion, lod)
        Species.RABBIT -> drawRabbit(::p, ::d, palette, prop, pose, motion, lod)
        Species.CHICK -> drawChick(::p, ::d, palette, prop, pose, motion, lod, stage)
        Species.DOG -> drawDog(::p, ::d, palette, prop, pose, motion, lod)
        Species.HAMSTER -> drawHamster(::p, ::d, palette, prop, pose, motion, lod)
        Species.PENGUIN -> drawPenguin(::p, ::d, palette, prop, pose, motion, lod)
        Species.PANDA -> drawPanda(::p, ::d, palette, prop, pose, motion, lod)
        Species.ACORN -> drawAcorn(::p, ::d, palette, prop, motion, lod, stage)
    }

    val faceProp = if (species == Species.ACORN) acornFaceProp(prop) else prop
    drawFace(::p, ::d, palette, species, faceProp, pose, iris, eyeStyle, blink, lod)

    drawStageAccessories(::p, ::d, palette, species, stage, branch, prop, lod)
    drawMoodEffects(::p, ::d, mood, expression, prop, motion, lod)
}
