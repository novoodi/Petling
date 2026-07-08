package com.example.petling.domain.engine

import com.example.petling.domain.model.GrowthStage

/**
 * 성장 단계 상태 머신. 전이 기준은 누적 완료 건수(XP 아님).
 * 퇴화는 없다 — evaluate()는 상향 전이만 반환한다.
 */
object GrowthStateMachine {

    /** 완료 건수에 해당하는 단계(EGG 제외 — EGG는 부화로만 벗어남). */
    fun stageFor(completedCount: Int): GrowthStage = when {
        completedCount >= GrowthStage.MATURE.requiredCompletions -> GrowthStage.MATURE
        completedCount >= GrowthStage.GROWTH2.requiredCompletions -> GrowthStage.GROWTH2
        completedCount >= GrowthStage.GROWTH1.requiredCompletions -> GrowthStage.GROWTH1
        else -> GrowthStage.JUVENILE
    }

    sealed interface Transition {
        data object None : Transition
        data class Advanced(
            val from: GrowthStage,
            val to: GrowthStage,
            /** GROWTH2 진입 시 진화 갈래(분기) 판정이 필요하다. */
            val requiresBranchChoice: Boolean,
        ) : Transition
    }

    fun evaluate(current: GrowthStage, completedCount: Int): Transition {
        if (current == GrowthStage.EGG) return Transition.None // 부화 전에는 성장하지 않음
        val target = stageFor(completedCount)
        if (target.ordinal <= current.ordinal) return Transition.None
        return Transition.Advanced(
            from = current,
            to = target,
            // 다단계 점프여도 GROWTH2를 "통과"하면 분기 판정이 필요
            requiresBranchChoice = current.ordinal < GrowthStage.GROWTH2.ordinal &&
                target.ordinal >= GrowthStage.GROWTH2.ordinal,
        )
    }
}
