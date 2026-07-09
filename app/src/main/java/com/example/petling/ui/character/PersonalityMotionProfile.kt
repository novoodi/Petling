package com.example.petling.ui.character

import com.example.petling.domain.model.Personality

/**
 * 성격별 렌더 미세 모션 프로파일.
 *
 * 종 기질([com.example.petling.ui.home.TemperamentProfile])이 "마당에서 어떤 행동을 고르는가"라면,
 * 이 프로파일은 "그 행동을 어떤 속도·리듬으로 보여주는가"다. 렌더러의 하드코딩 타이밍 상수를
 * 성격 참조로 치환해 "나만 아는 내 캐릭터 말투"처럼 모션에도 개성을 입힌다.
 *
 * [motionProfileFor] `null`(성격 미상·레거시)은 기존 상수와 정확히 일치하는 [DEFAULT]를 돌려줘
 * 픽셀 회귀가 없다.
 */
data class PersonalityMotionProfile(
    val breatheMs: Int,     // 숨쉬기(부유) 주기
    val blinkCycleMs: Int,  // 깜빡임 주기
    val idleHopCycleMs: Int,// 아이들 폴짝 홉 주기(먼지와 공유)
    val swayDegMul: Float,  // 살랑 기울기 진폭 배율
    val lidBias: Float,     // 기본 눈꺼풀 내림(나른함) 0..1
) {
    companion object {
        /** 기존 렌더러 상수와 동일 — 성실형·성격 미상의 기준값(회귀 없음). */
        val DEFAULT = PersonalityMotionProfile(
            breatheMs = 2600, blinkCycleMs = 4200, idleHopCycleMs = 6400,
            swayDegMul = 1f, lidBias = 0f,
        )

        fun motionProfileFor(personality: Personality?): PersonalityMotionProfile = when (personality) {
            null, Personality.SINCERE -> DEFAULT
            // 자유분방: 빠른 숨·잦은 홉·큰 살랑
            Personality.FREE_SPIRIT -> PersonalityMotionProfile(2300, 4200, 4600, 1.4f, 0f)
            // 걱정: 빠른 숨·잦은 깜빡임·조심스런 작은 흔들
            Personality.WORRIER -> PersonalityMotionProfile(2100, 3200, 6400, 0.7f, 0f)
            // 몽상: 느긋한 숨·느린 깜빡임·드문 홉·나른한 눈꺼풀
            Personality.DREAMER -> PersonalityMotionProfile(3100, 5200, 8200, 1f, 0.12f)
        }
    }
}
