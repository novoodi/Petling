package com.example.petling.domain

import com.example.petling.ui.character.IndividualTraits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 개체 시드 변이 검증. 결정론·범위·0L 폴백.
 */
class IndividualTraitsTest {

    @Test
    fun zero_seed_is_neutral() {
        assertEquals(IndividualTraits.NEUTRAL, IndividualTraits.from(0L))
    }

    @Test
    fun deterministic_for_same_seed() {
        assertEquals(IndividualTraits.from(123456789L), IndividualTraits.from(123456789L))
    }

    @Test
    fun different_seeds_usually_differ() {
        // 서로 다른 seed는 대체로 다른 변이를 낸다(충돌은 극히 드묾).
        assertNotEquals(IndividualTraits.from(11L), IndividualTraits.from(22L))
    }

    @Test
    fun traits_stay_within_range() {
        for (seed in longArrayOf(1L, 42L, 9999L, -7L, Long.MAX_VALUE, 1234567890123L)) {
            val t = IndividualTraits.from(seed)
            assertTrue("size $seed", t.sizeJitter in 0.94f..1.06f)
            assertTrue("cheek $seed", t.cheekStrength in 0.85f..1.15f)
            assertTrue("tail $seed", t.tailCurl in 0.85f..1.15f)
        }
    }
}
