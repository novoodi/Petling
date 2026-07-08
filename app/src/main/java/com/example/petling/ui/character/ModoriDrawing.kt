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
    view: CreatureView = CreatureView.FRONT,
) {
    val s = size.minDimension
    val origin = Offset((size.width - s) / 2f, (size.height - s) / 2f)
    fun p(x: Float, y: Float) = Offset(origin.x + x * s, origin.y + y * s)
    fun d(v: Float) = v * s
    val lod = lodFor(s)

    // 바닥 그림자
    drawOval(
        color = Color(0x22000000),
        topLeft = p(0.30f, 0.87f),
        size = Size(d(0.40f), d(0.07f)),
    )

    if (stage == GrowthStage.EGG) {
        drawEgg(::p, ::d, palette, crackProgress, hatchAlpha)
        return
    }

    val pose = poseFor(expression)

    // 옆모습(보행). ACORN은 옆모습이 없어(구르기 이동) FRONT로 폴백.
    if (view == CreatureView.SIDE && species != Species.ACORN) {
        val sp = sideProportionsFor(species, stage)
        when (species) {
            Species.FOX -> drawFoxSide(::p, ::d, palette, sp, pose, motion, eyeStyle, blink, lod)
            Species.CAT -> drawCatSide(::p, ::d, palette, sp, pose, motion, eyeStyle, blink, lod)
            Species.RABBIT -> drawRabbitSide(::p, ::d, palette, sp, pose, motion, eyeStyle, blink, lod)
            Species.CHICK -> drawChickSide(::p, ::d, palette, sp, pose, motion, eyeStyle, blink, lod)
            Species.DOG -> drawDogSide(::p, ::d, palette, sp, pose, motion, eyeStyle, blink, lod)
            Species.HAMSTER -> drawHamsterSide(::p, ::d, palette, sp, pose, motion, eyeStyle, blink, lod)
            Species.PENGUIN -> drawPenguinSide(::p, ::d, palette, sp, pose, motion, eyeStyle, blink, lod)
            Species.PANDA -> drawPandaSide(::p, ::d, palette, sp, pose, motion, eyeStyle, blink, lod)
            Species.ACORN -> Unit
        }
        // 액세서리·기분 이펙트는 정면 전용(보행은 수 초의 과도 상태)
        return
    }

    val prop = proportionsFor(species, stage)
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

/** 하위 호환: 기존 도토리 호출부. */
fun DrawScope.drawModori(
    stage: GrowthStage,
    branch: Branch?,
    mood: Mood,
    expression: Expression,
    palette: ModoriPalette,
    eyeStyle: Int,
    blink: Float = 0f,
    crackProgress: Float = 0f,
    hatchAlpha: Float = 1f,
) = drawCreature(Species.ACORN, stage, branch, mood, expression, palette, eyeStyle, blink, crackProgress, hatchAlpha)
