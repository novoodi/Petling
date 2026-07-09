package com.example.petling.domain

import com.example.petling.domain.model.Personality
import com.example.petling.ui.character.PersonalityMotionProfile
import com.example.petling.ui.character.PersonalityMotionProfile.Companion.motionProfileFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 성격 모션 프로파일 검증. 기본값=기존 상수(회귀 없음) + 성격별 차별화.
 */
class PersonalityMotionProfileTest {

    @Test
    fun null_and_sincere_equal_legacy_defaults() {
        // 레거시(성격 미상)·성실형은 기존 렌더러 상수와 정확히 일치해야 픽셀 회귀가 없다.
        val default = PersonalityMotionProfile.DEFAULT
        assertEquals(2600, default.breatheMs)
        assertEquals(4200, default.blinkCycleMs)
        assertEquals(6400, default.idleHopCycleMs)
        assertEquals(1f, default.swayDegMul, 0f)
        assertEquals(0f, default.lidBias, 0f)
        assertEquals(default, motionProfileFor(null))
        assertEquals(default, motionProfileFor(Personality.SINCERE))
    }

    @Test
    fun personalities_are_differentiated() {
        val free = motionProfileFor(Personality.FREE_SPIRIT)
        val worry = motionProfileFor(Personality.WORRIER)
        val dream = motionProfileFor(Personality.DREAMER)
        // 자유분방: 잦은 홉·큰 살랑
        assertTrue(free.idleHopCycleMs < PersonalityMotionProfile.DEFAULT.idleHopCycleMs)
        assertTrue(free.swayDegMul > 1f)
        // 걱정: 잦은 깜빡임·작은 살랑
        assertTrue(worry.blinkCycleMs < PersonalityMotionProfile.DEFAULT.blinkCycleMs)
        assertTrue(worry.swayDegMul < 1f)
        // 몽상: 느린 숨·나른한 눈꺼풀
        assertTrue(dream.breatheMs > PersonalityMotionProfile.DEFAULT.breatheMs)
        assertTrue(dream.lidBias > 0f)
    }

    @Test
    fun every_personality_has_positive_timings() {
        for (p in Personality.entries) {
            val prof = motionProfileFor(p)
            assertTrue(prof.breatheMs > 0 && prof.blinkCycleMs > 0 && prof.idleHopCycleMs > 0)
        }
    }
}
