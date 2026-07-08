package com.example.petling.domain

import com.example.petling.domain.engine.MoodCalculator
import com.example.petling.domain.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodCalculatorTest {

    @Test
    fun no_due_schedules_is_calm() {
        assertEquals(Mood.CALM, MoodCalculator.calculate(dueSoFarToday = 0, completedToday = 0))
    }

    @Test
    fun all_completed_is_happy() {
        assertEquals(Mood.HAPPY, MoodCalculator.calculate(dueSoFarToday = 4, completedToday = 4))
    }

    @Test
    fun happy_threshold_at_seventy_percent() {
        // 7/10 = 0.7 -> HAPPY
        assertEquals(Mood.HAPPY, MoodCalculator.calculate(10, 7))
        // 6/10 = 0.6 -> CALM
        assertEquals(Mood.CALM, MoodCalculator.calculate(10, 6))
    }

    @Test
    fun calm_threshold_at_thirty_percent() {
        // 3/10 = 0.3 -> CALM
        assertEquals(Mood.CALM, MoodCalculator.calculate(10, 3))
        // 2/10 = 0.2 -> TIRED
        assertEquals(Mood.TIRED, MoodCalculator.calculate(10, 2))
    }

    @Test
    fun nothing_done_is_tired() {
        assertEquals(Mood.TIRED, MoodCalculator.calculate(dueSoFarToday = 5, completedToday = 0))
    }
}
