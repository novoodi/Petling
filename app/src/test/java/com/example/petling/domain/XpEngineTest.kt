package com.example.petling.domain

import com.example.petling.domain.engine.XpEngine
import com.example.petling.domain.engine.XpRules
import com.example.petling.domain.model.XpReason
import org.junit.Assert.assertEquals
import org.junit.Test

class XpEngineTest {

    @Test
    fun registration_is_flat_one_and_ignores_streak() {
        val gain = XpEngine.registrationGain()
        assertEquals(1, gain.total)
        assertEquals(1.0, gain.multiplier, 0.0)
        assertEquals(XpReason.REGISTER, gain.reason)
    }

    @Test
    fun completion_base_values() {
        assertEquals(5, XpEngine.completionGain(isImportant = false, streakDays = 0).total)
        assertEquals(8, XpEngine.completionGain(isImportant = true, streakDays = 0).total)
    }

    @Test
    fun completion_reason_reflects_importance() {
        assertEquals(XpReason.COMPLETE, XpEngine.completionGain(false, 1).reason)
        assertEquals(XpReason.COMPLETE_IMPORTANT, XpEngine.completionGain(true, 1).reason)
    }

    @Test
    fun streak_multiplier_thresholds() {
        assertEquals(1.0, XpRules.streakMultiplier(0), 0.0)
        assertEquals(1.0, XpRules.streakMultiplier(2), 0.0)
        assertEquals(1.2, XpRules.streakMultiplier(3), 0.0)
        assertEquals(1.2, XpRules.streakMultiplier(6), 0.0)
        assertEquals(1.5, XpRules.streakMultiplier(7), 0.0)
        assertEquals(1.5, XpRules.streakMultiplier(30), 0.0)
    }

    @Test
    fun completion_rounding_is_fixed() {
        // 5 * 1.2 = 6.0 -> 6
        assertEquals(6, XpEngine.completionGain(false, 3).total)
        // 8 * 1.2 = 9.6 -> 10
        assertEquals(10, XpEngine.completionGain(true, 3).total)
        // 5 * 1.5 = 7.5 -> 8 (half-up)
        assertEquals(8, XpEngine.completionGain(false, 7).total)
        // 8 * 1.5 = 12.0 -> 12
        assertEquals(12, XpEngine.completionGain(true, 7).total)
    }
}
