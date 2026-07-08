package com.example.petling.domain.model

import kotlinx.serialization.Serializable

/** 캐릭터 표정. 렌더러가 눈/입 모양으로 표현한다. */
enum class Expression {
    NEUTRAL, HAPPY, EXCITED, SLEEPY, WORRIED, SAD,
}

/**
 * 재생할 애니메이션 종류.
 * WALK/SLEEP/EAT/GROOM/PECK은 홈 마당 로밍용으로, 렌더 시점 spec.copy로만 주입하고 저장 경로엔 넣지 않는다.
 */
enum class CharacterAnimation {
    IDLE, BOUNCE, HATCHING, EVOLVING, WALK, SLEEP, EAT, GROOM, PECK,
}

/**
 * 캐릭터를 그리는 데 필요한 모든 파라미터.
 * @Serializable 이므로 growth_snapshots 저장/다이어리 재렌더에 그대로 재사용된다.
 */
@Serializable
data class CharacterSpec(
    val stage: GrowthStage,
    val branch: Branch? = null,
    val mood: Mood = Mood.CALM,
    val expression: Expression = Expression.NEUTRAL,
    val species: Species = Species.ACORN,
    val colorHue: Float = 30f,
    val eyeStyle: Int = 0,
    val animation: CharacterAnimation = CharacterAnimation.IDLE,
) {
    companion object {
        /** 도메인 캐릭터 상태로부터 기본 표정을 정해 스펙을 만든다. */
        fun from(
            state: CharacterState,
            expression: Expression = expressionFor(state.mood),
            animation: CharacterAnimation = CharacterAnimation.IDLE,
        ): CharacterSpec = CharacterSpec(
            stage = state.stage,
            branch = state.branch,
            mood = state.mood,
            expression = expression,
            species = state.species,
            colorHue = state.colorHue,
            eyeStyle = state.eyeStyle,
            animation = animation,
        )

        fun expressionFor(mood: Mood): Expression = when (mood) {
            Mood.HAPPY -> Expression.HAPPY
            Mood.CALM -> Expression.NEUTRAL
            Mood.TIRED -> Expression.SLEEPY
        }
    }
}
