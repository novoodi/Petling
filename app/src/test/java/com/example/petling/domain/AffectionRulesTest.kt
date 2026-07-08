package com.example.petling.domain

import com.example.petling.domain.engine.AffectionLevel
import com.example.petling.domain.engine.AffectionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AffectionRulesTest {

    @Test
    fun level_boundaries() {
        assertEquals(AffectionLevel.STRANGER, AffectionRules.levelFor(0))
        assertEquals(AffectionLevel.STRANGER, AffectionRules.levelFor(24))
        assertEquals(AffectionLevel.FAMILIAR, AffectionRules.levelFor(25))
        assertEquals(AffectionLevel.FAMILIAR, AffectionRules.levelFor(49))
        assertEquals(AffectionLevel.CLOSE, AffectionRules.levelFor(50))
        assertEquals(AffectionLevel.CLOSE, AffectionRules.levelFor(79))
        assertEquals(AffectionLevel.BONDED, AffectionRules.levelFor(80))
        assertEquals(AffectionLevel.BONDED, AffectionRules.levelFor(100))
    }

    @Test
    fun apply_respects_daily_cap() {
        val r = AffectionRules.apply(current = 10, gainedToday = 18, gain = 5)
        assertEquals(12, r.newValue) // 상한 20 중 2만 남음
        assertEquals(2, r.gained)
    }

    @Test
    fun apply_clamps_at_max() {
        val r = AffectionRules.apply(current = 99, gainedToday = 0, gain = 5)
        assertEquals(100, r.newValue)
        assertEquals(1, r.gained)
    }

    @Test
    fun apply_detects_level_up() {
        val r = AffectionRules.apply(current = 24, gainedToday = 0, gain = 2)
        assertEquals(AffectionLevel.FAMILIAR, r.levelUp)
    }

    @Test
    fun apply_no_level_up_within_level() {
        val r = AffectionRules.apply(current = 30, gainedToday = 0, gain = 2)
        assertNull(r.levelUp)
    }

    @Test
    fun decay_floors_at_level_min() {
        // 친함(50~79)에서 아무리 오래 비워도 50 밑으로 안 떨어짐
        assertEquals(50, AffectionRules.decayOnReturn(current = 52, daysAway = 10))
        assertEquals(53, AffectionRules.decayOnReturn(current = 60, daysAway = 7))
    }

    @Test
    fun decay_skips_short_absence() {
        assertEquals(60, AffectionRules.decayOnReturn(current = 60, daysAway = 2))
    }
}
