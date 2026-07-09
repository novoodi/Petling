package com.example.petling.ui.character

import com.example.petling.domain.model.Species
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 정면 홉 보행 스타일. 옛 옆모습(SIDE) 보행을 대체한다 — 캐릭터는 정면을 유지한 채
 * 진행 방향으로 살짝 기울여 통통 튀며 이동한다(발 슬라이딩·기괴한 4족 골격 문제 소멸).
 *
 * 종별 개성은 [hopStyleFor]가 결정한다: 토끼=높은 홉, 펭귄=홉 없이 좌우 롤 뒤뚱,
 * 햄스터=빠른 잔홉, 판다=느리고 묵직. 옛 GaitSpec을 "홉 스타일"로 재해석한 것.
 */
data class HopStyle(
    val hopHeight: Float, // 정규화 홉 높이(base 배수). 0=홉 없음(펭귄 뒤뚱)
    val periodMs: Int,    // 홉 1사이클 길이(ms). walkPhase 트랙 duration으로 사용
    val rollDeg: Float,   // 좌우 롤(펭귄 뒤뚱)
    val leanDeg: Float,   // 진행 방향 상시 기울임(5~7°)
    val squashAmp: Float, // 착지 스쿼시 강도
    val dustAmp: Float,   // 착지 먼지 배율(0=없음)
)

fun hopStyleFor(species: Species): HopStyle = when (species) {
    Species.RABBIT -> HopStyle(hopHeight = 0.060f, periodMs = 520, rollDeg = 0f, leanDeg = 5f, squashAmp = 0.12f, dustAmp = 0.8f)
    Species.HAMSTER -> HopStyle(hopHeight = 0.022f, periodMs = 300, rollDeg = 0f, leanDeg = 6f, squashAmp = 0.06f, dustAmp = 0.4f)
    Species.PANDA -> HopStyle(hopHeight = 0.028f, periodMs = 800, rollDeg = 3f, leanDeg = 4f, squashAmp = 0.10f, dustAmp = 1.0f)
    Species.PENGUIN -> HopStyle(hopHeight = 0f, periodMs = 560, rollDeg = 7f, leanDeg = 3f, squashAmp = 0.02f, dustAmp = 0f)
    Species.CHICK -> HopStyle(hopHeight = 0.030f, periodMs = 380, rollDeg = 4f, leanDeg = 6f, squashAmp = 0.05f, dustAmp = 0.4f)
    Species.CAT -> HopStyle(hopHeight = 0.034f, periodMs = 500, rollDeg = 0f, leanDeg = 6f, squashAmp = 0.06f, dustAmp = 0.6f)
    Species.FOX -> HopStyle(hopHeight = 0.038f, periodMs = 480, rollDeg = 0f, leanDeg = 6f, squashAmp = 0.06f, dustAmp = 0.6f)
    Species.DOG -> HopStyle(hopHeight = 0.040f, periodMs = 460, rollDeg = 0f, leanDeg = 6f, squashAmp = 0.06f, dustAmp = 0.6f)
    Species.ACORN -> HopStyle(hopHeight = 0.020f, periodMs = 520, rollDeg = 4f, leanDeg = 4f, squashAmp = 0.03f, dustAmp = 0.3f)
}

/**
 * 한 프레임의 홉 포즈. 렌더러가 이 값으로 translate/rotate/scale + 먼지를 적용한다.
 * 진행 방향(좌/우)은 호출부 graphicsLayer scaleX 반전이 tilt까지 함께 뒤집으므로
 * 여기서는 항상 "오른쪽 진행" 기준으로 계산한다.
 */
data class HopPose(
    val dyNorm: Float,  // 세로 오프셋(base 배수, 음수=위로)
    val tiltDeg: Float, // 기울임(도)
    val scaleX: Float,
    val scaleY: Float,
    val dust: Float,    // 0..1 착지 먼지 알파(motion.dust로 전달)
)

/** phi(0..1 보행 위상) → 홉 포즈. 순수 함수 — 렌더러·디버그 갤러리가 공유해 동일 포즈 보장. */
fun hopPose(style: HopStyle, phi: Float): HopPose {
    val twoPi = (2.0 * PI).toFloat()
    val roll = sin(phi * twoPi) * style.rollDeg

    if (style.hopHeight <= 0f) {
        // 홉 없음(펭귄): 좌우 롤 위주 + 발걸음마다 미세 상하 반동
        val bob = abs(sin(phi * twoPi))
        return HopPose(
            dyNorm = -bob * 0.006f,
            tiltDeg = style.leanDeg + roll,
            scaleX = 1f,
            scaleY = 1f,
            dust = 0f,
        )
    }

    // sin(phi·π): phi 0→0.5→1 에서 0→1→0 아치(사이클당 한 번의 홉)
    val h = sin(phi * PI.toFloat()).coerceAtLeast(0f)
    // 착지 예비 스쿼시: 하강 후반(phi 0.7~1.0)에 램프
    val squash = ((phi - 0.7f) / 0.3f).coerceIn(0f, 1f)

    val stretch = h // 정점에서 늘어남
    val scaleY = 1f + stretch * 0.06f - squash * style.squashAmp
    val scaleX = 1f - stretch * 0.04f + squash * style.squashAmp * 0.8f
    val dy = -h * style.hopHeight
    val tilt = style.leanDeg + roll

    return HopPose(
        dyNorm = dy,
        tiltDeg = tilt,
        scaleX = scaleX,
        scaleY = scaleY,
        dust = squash * style.dustAmp,
    )
}
