package com.example.petling.domain

import com.example.petling.domain.model.Personality
import com.example.petling.ui.home.personalityBiasFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 성격별 perch 편향 매핑 검증. (관찰 가능한 차이: 이동 빈도·체류 시간·weight 선호)
 */
class PersonalityBiasTest {

    @Test
    fun free_spirit_perches_most_and_briefly() {
        val free = personalityBiasFor(Personality.FREE_SPIRIT)
        val dream = personalityBiasFor(Personality.DREAMER)
        assertTrue("자유분방이 몽상보다 자주 앉음", free.perchUrge > dream.perchUrge)
        // 자유분방은 짧게, 몽상은 오래
        assertTrue(free.sitDwellMs.last < dream.sitDwellMs.first)
    }

    @Test
    fun dreamer_stays_longest() {
        val dream = personalityBiasFor(Personality.DREAMER)
        Personality.entries.filter { it != Personality.DREAMER }.forEach {
            assertTrue("몽상이 $it 보다 오래 앉음", dream.sitDwellMs.first >= personalityBiasFor(it).sitDwellMs.last)
        }
    }

    @Test
    fun only_worrier_prefers_weight() {
        assertTrue(personalityBiasFor(Personality.WORRIER).prefersWeight)
        assertFalse(personalityBiasFor(Personality.FREE_SPIRIT).prefersWeight)
        assertFalse(personalityBiasFor(Personality.DREAMER).prefersWeight)
        assertFalse(personalityBiasFor(Personality.SINCERE).prefersWeight)
    }

    @Test
    fun null_equals_sincere() {
        assertEquals(personalityBiasFor(Personality.SINCERE), personalityBiasFor(null))
    }
}
