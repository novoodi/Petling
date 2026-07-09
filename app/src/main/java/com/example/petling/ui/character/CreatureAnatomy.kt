package com.example.petling.ui.character

import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Species

/**
 * 캐릭터 드로잉의 3계층 파라미터.
 *
 * - [Proportions] : 종×성장단계별 "해부학 비율"(머리/몸/귀/꼬리/발 크기). 성장이 크기만 커지던 것을
 *   비율 변화로 바꿔 유생기(큰 머리·짧은 꼬리) → 성숙기(제 실루엣)로 눈에 띄게 자라게 한다.
 * - [FacePose]    : 표정 → 눈/눈썹/입 + 동물다운 신체언어(귀 처짐·꼬리 자세)로 매핑.
 * - [CreatureMotion] : 렌더러가 프레임마다 계산해 넘기는 미세 모션(숨쉬기·귀 쫑긋·꼬리 살랑·먼지).
 */

/** 상세도. 작은 크기(72dp 등)에선 세부(수염·홍채·발가락·마킹)를 생략해 뭉개짐을 막는다. */
enum class Lod { SIMPLE, FULL }

fun lodFor(minDimensionPx: Float): Lod = if (minDimensionPx >= 360f) Lod.FULL else Lod.SIMPLE

/** 정규화 좌표(0~1) 기준 비율 묶음. 각 종 드로잉이 필요한 값만 소비한다. */
data class Proportions(
    val scale: Float,      // 전체 크기 계수(액세서리 배치용)
    val headR: Float,      // 머리 반경
    val headCy: Float,     // 머리 중심 y
    val bodyRx: Float,     // 몸 가로 반경
    val bodyRy: Float,     // 몸 세로 반경
    val bodyCy: Float,     // 몸 중심 y
    val earLen: Float,     // 귀 길이
    val earW: Float,       // 귀 폭
    val earFold: Float,    // 귀 접힘(0=직립, 1=완전 접힘) — 아기 토끼 특징
    val muzzle: Float,     // 주둥이 발달도 0..1
    val tailLen: Float,    // 꼬리 길이
    val tailThick: Float,  // 꼬리 굵기
    val limb: Float,       // 앞발 크기(0=없음)
    val eyeScale: Float,   // 눈 크기 배율
    val eyeY: Float,       // 눈 y(머리중심 기준 headR 배수 offset)
    val eyeGap: Float,     // 두 눈 간격(headR 배수)
    val fluff: Float,      // 가슴/볼 털 강도 0..1
)

private fun stageIndex(stage: GrowthStage): Int = when (stage) {
    GrowthStage.JUVENILE -> 0
    GrowthStage.GROWTH1 -> 1
    GrowthStage.GROWTH2 -> 2
    GrowthStage.MATURE -> 3
    GrowthStage.EGG -> 0
}

// 단계별 몸통 기본치(포유류 공통). idx 0=유생 … 3=성숙.
private val SCALE = floatArrayOf(0.72f, 0.84f, 0.94f, 1.0f)
private val HEAD_R = floatArrayOf(0.190f, 0.178f, 0.168f, 0.158f)
private val HEAD_CY = floatArrayOf(0.440f, 0.425f, 0.415f, 0.405f)
private val BODY_RX = floatArrayOf(0.160f, 0.180f, 0.200f, 0.215f)
private val BODY_RY = floatArrayOf(0.150f, 0.175f, 0.198f, 0.220f)
private val BODY_CY = floatArrayOf(0.660f, 0.655f, 0.648f, 0.640f)
private val EYE_SCALE = floatArrayOf(1.18f, 1.06f, 1.00f, 0.94f)
private val LIMB = floatArrayOf(0.018f, 0.045f, 0.060f, 0.072f)
private val FLUFF = floatArrayOf(0.0f, 0.25f, 0.55f, 0.85f)

/** 종×단계 비율 테이블. */
fun proportionsFor(species: Species, stage: GrowthStage): Proportions {
    val i = stageIndex(stage)
    // 종별로 변하는 귀/주둥이/꼬리
    val (earLen, earW, earFold) = earSpec(species, i)
    val muzzle = muzzleSpec(species, i)
    val (tailLen, tailThick) = tailSpec(species, i)
    val bodyRxMul = when (species) {
        Species.CHICK, Species.PENGUIN -> 0.92f
        Species.HAMSTER -> 1.10f
        Species.PANDA -> 1.12f
        else -> 1f
    }
    val bodyRyMul = when (species) {
        Species.CHICK, Species.PENGUIN -> 1.05f
        Species.HAMSTER -> 0.98f
        Species.PANDA -> 1.05f
        else -> 1f
    }
    return Proportions(
        scale = SCALE[i],
        headR = HEAD_R[i] * if (species == Species.CHICK || species == Species.PENGUIN) 0.95f else 1f,
        headCy = HEAD_CY[i],
        bodyRx = BODY_RX[i] * bodyRxMul,
        bodyRy = BODY_RY[i] * bodyRyMul,
        bodyCy = BODY_CY[i],
        earLen = earLen,
        earW = earW,
        earFold = earFold,
        muzzle = muzzle,
        tailLen = tailLen,
        tailThick = tailThick,
        limb = LIMB[i],
        eyeScale = EYE_SCALE[i] * if (species == Species.CAT) 1.06f else 1f,
        eyeY = 0.16f,
        eyeGap = 0.52f,
        fluff = FLUFF[i],
    )
}

private data class EarSpec(val len: Float, val w: Float, val fold: Float)

private fun earSpec(species: Species, i: Int): EarSpec = when (species) {
    Species.FOX -> EarSpec(floatArrayOf(0.15f, 0.14f, 0.135f, 0.13f)[i], 0.12f, 0f)
    Species.CAT -> EarSpec(floatArrayOf(0.11f, 0.105f, 0.10f, 0.10f)[i], 0.11f, 0f)
    Species.RABBIT -> EarSpec(
        floatArrayOf(0.16f, 0.22f, 0.28f, 0.32f)[i],
        0.065f,
        floatArrayOf(0.45f, 0.18f, 0f, 0f)[i],
    )
    Species.DOG -> EarSpec(floatArrayOf(0.11f, 0.12f, 0.13f, 0.14f)[i], 0.09f, 0f) // 늘어진 귀 길이
    Species.HAMSTER, Species.PANDA -> EarSpec(0.07f, 0.09f, 0f) // 작은 반원 귀(반지름 근사)
    Species.CHICK, Species.PENGUIN, Species.ACORN -> EarSpec(0f, 0f, 0f)
}

private fun muzzleSpec(species: Species, i: Int): Float = when (species) {
    Species.FOX -> floatArrayOf(0.20f, 0.42f, 0.60f, 0.75f)[i]
    Species.CAT -> floatArrayOf(0.15f, 0.30f, 0.45f, 0.55f)[i]
    Species.RABBIT -> floatArrayOf(0.10f, 0.20f, 0.30f, 0.35f)[i]
    Species.DOG -> floatArrayOf(0.18f, 0.34f, 0.48f, 0.58f)[i]
    Species.HAMSTER -> floatArrayOf(0.08f, 0.14f, 0.20f, 0.24f)[i]
    Species.PANDA -> floatArrayOf(0.10f, 0.18f, 0.26f, 0.32f)[i]
    else -> 0f
}

private data class TailSpec(val len: Float, val thick: Float)

private fun tailSpec(species: Species, i: Int): TailSpec = when (species) {
    Species.FOX -> TailSpec(floatArrayOf(0.06f, 0.14f, 0.22f, 0.28f)[i], floatArrayOf(0.07f, 0.09f, 0.11f, 0.13f)[i])
    Species.CAT -> TailSpec(floatArrayOf(0.10f, 0.18f, 0.24f, 0.30f)[i], floatArrayOf(0.045f, 0.05f, 0.055f, 0.06f)[i])
    Species.RABBIT -> TailSpec(0.055f, 0.06f) // 폼폼 꼬리(길이 고정)
    Species.DOG -> TailSpec(floatArrayOf(0.06f, 0.08f, 0.10f, 0.12f)[i], 0.05f) // 말린 꼬리
    Species.HAMSTER -> TailSpec(0.03f, 0.025f)
    else -> TailSpec(0f, 0f)
}

// ─────────────────────────── 표정 → 신체언어 ───────────────────────────

enum class MouthShape { SMALL_V, SMILE, OPEN, YAWN, WAVY, FROWN }

data class FacePose(
    val lidTop: Float,       // 윗눈꺼풀 감김 0(열림)..1(감김)
    val lidBottom: Float,    // 아랫눈꺼풀 상승(스퀸트) 0..1
    val pupilScale: Float,   // 동공 크기 0.5..0.85
    val browAngle: Float?,   // 눈썹 각도(도). null=없음. 양수=팔자(안쪽 올라감)
    val mouth: MouthShape,
    val earDroop: Float,     // 귀 옆으로 처짐 0..1
    val earBack: Float,      // 귀 뒤로 젖힘 0..1
    val earPerk: Float,      // 귀 앞·위 쫑긋 배율(1=기본)
    val tailLift: Float,     // 꼬리 들림 -1(감김)..1(최대)
    val tailWagAmp: Float,   // 살랑 진폭 배율
    val teary: Boolean,      // 눈물 글썽
    val extraHighlight: Boolean, // 추가 반짝(신남)
)

fun poseFor(expression: Expression): FacePose = when (expression) {
    Expression.NEUTRAL -> FacePose(0f, 0f, 0.65f, null, MouthShape.SMALL_V, 0f, 0f, 1f, 0.1f, 1f, false, false)
    Expression.HAPPY -> FacePose(0f, 0.28f, 0.68f, null, MouthShape.SMILE, 0f, 0f, 1.05f, 0.5f, 1.5f, false, false)
    Expression.EXCITED -> FacePose(0f, 0f, 0.82f, -12f, MouthShape.OPEN, 0f, 0f, 1.15f, 1f, 2.5f, false, true)
    Expression.SLEEPY -> FacePose(0.7f, 0f, 0.6f, null, MouthShape.YAWN, 0.35f, 0f, 0.9f, -0.6f, 0f, false, false)
    Expression.WORRIED -> FacePose(0.2f, 0f, 0.5f, 20f, MouthShape.WAVY, 0f, 0.5f, 0.9f, -0.3f, 0.3f, false, false)
    Expression.SAD -> FacePose(0.45f, 0f, 0.55f, 28f, MouthShape.FROWN, 0.85f, 0.2f, 0.85f, -1f, 0f, true, false)
}

// ─────────────────────────── 모션 ───────────────────────────

/** 렌더러가 매 프레임 계산해 drawCreature로 넘기는 미세 애니메이션 값. */
data class CreatureMotion(
    val earTwitch: Float = 0f,   // 0..1 쫑긋 펄스
    val tailWag: Float = 0f,     // 0..1 연속 위상(사인 입력)
    val effectPhase: Float = 0f, // 0..1 하트/zzz 상승·페이드 위상
    val dust: Float = 0f,        // 0..1 착지 먼지 알파
    val breathe: Float = 0f,     // -1..1 숨쉬기(몸 팽창)
) {
    companion object {
        val STATIC = CreatureMotion()
    }
}
