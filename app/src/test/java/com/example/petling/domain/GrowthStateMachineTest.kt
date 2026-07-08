package com.example.petling.domain

import com.example.petling.domain.engine.GrowthStateMachine
import com.example.petling.domain.engine.GrowthStateMachine.Transition
import com.example.petling.domain.model.GrowthStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthStateMachineTest {

    @Test
    fun stage_for_completion_boundaries() {
        assertEquals(GrowthStage.JUVENILE, GrowthStateMachine.stageFor(0))
        assertEquals(GrowthStage.JUVENILE, GrowthStateMachine.stageFor(9))
        assertEquals(GrowthStage.GROWTH1, GrowthStateMachine.stageFor(10))
        assertEquals(GrowthStage.GROWTH1, GrowthStateMachine.stageFor(29))
        assertEquals(GrowthStage.GROWTH2, GrowthStateMachine.stageFor(30))
        assertEquals(GrowthStage.GROWTH2, GrowthStateMachine.stageFor(59))
        assertEquals(GrowthStage.MATURE, GrowthStateMachine.stageFor(60))
        assertEquals(GrowthStage.MATURE, GrowthStateMachine.stageFor(1000))
    }

    @Test
    fun no_transition_below_threshold() {
        assertEquals(Transition.None, GrowthStateMachine.evaluate(GrowthStage.JUVENILE, 9))
    }

    @Test
    fun advances_at_ten_completions() {
        val t = GrowthStateMachine.evaluate(GrowthStage.JUVENILE, 10)
        assertTrue(t is Transition.Advanced)
        t as Transition.Advanced
        assertEquals(GrowthStage.JUVENILE, t.from)
        assertEquals(GrowthStage.GROWTH1, t.to)
        assertEquals(false, t.requiresBranchChoice)
    }

    @Test
    fun growth2_transition_requires_branch_choice() {
        val t = GrowthStateMachine.evaluate(GrowthStage.GROWTH1, 30) as Transition.Advanced
        assertEquals(GrowthStage.GROWTH2, t.to)
        assertTrue(t.requiresBranchChoice)
    }

    @Test
    fun mature_transition_does_not_require_branch_choice() {
        val t = GrowthStateMachine.evaluate(GrowthStage.GROWTH2, 60) as Transition.Advanced
        assertEquals(GrowthStage.MATURE, t.to)
        assertEquals(false, t.requiresBranchChoice)
    }

    @Test
    fun multi_stage_jump_through_growth2_still_requires_branch() {
        // JUVENILE에서 한 번에 60건 도달 -> MATURE로 점프하지만 GROWTH2를 통과하므로 분기 필요
        val t = GrowthStateMachine.evaluate(GrowthStage.JUVENILE, 60) as Transition.Advanced
        assertEquals(GrowthStage.MATURE, t.to)
        assertTrue(t.requiresBranchChoice)
    }

    @Test
    fun no_regression() {
        // 완료 건수가 현 단계보다 낮게 들어와도 퇴화 없음
        assertEquals(Transition.None, GrowthStateMachine.evaluate(GrowthStage.MATURE, 5))
        assertEquals(Transition.None, GrowthStateMachine.evaluate(GrowthStage.GROWTH2, 10))
    }

    @Test
    fun egg_never_advances_automatically() {
        assertEquals(Transition.None, GrowthStateMachine.evaluate(GrowthStage.EGG, 100))
    }
}
