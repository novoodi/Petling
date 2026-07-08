package com.example.petling.domain.model

data class CharacterState(
    val name: String,
    val personality: Personality,
    /** 스타팅 동물 종류. 기존 데이터 호환 위해 기본 도토리. */
    val species: Species = Species.ACORN,
    val stage: GrowthStage,
    val branch: Branch? = null,
    val totalXp: Int = 0,
    /** 누적 완료 건수(일정). 통계용. */
    val completedCount: Int = 0,
    /** 누적 캡처 건수 — 성장 단계 전이 기준값(정리함 컨셉). */
    val captureCount: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val lastCompletionEpochDay: Long? = null,
    val mood: Mood = Mood.CALM,
    val moodDateEpochDay: Long = 0L,
    val lastVisitEpochDay: Long = 0L,
    /** 호감도 0..100. 매일 첫 방문·일정 완료·캡처 정리·간식으로 상승. */
    val affection: Int = 0,
    /** 일일 카운터(가산 상한·간식 개수) 리셋 기준일. */
    val affectionDateEpochDay: Long = 0L,
    /** 오늘 얻은 호감도 총량(상한 20). */
    val affectionGainedToday: Int = 0,
    /** 오늘 준 간식 개수(하루 3개). */
    val snacksToday: Int = 0,
    /** 커스터마이징: 몸 색상 hue (0..360) */
    val colorHue: Float = 30f,
    /** 커스터마이징: 눈 모양 변형 인덱스 */
    val eyeStyle: Int = 0,
    val quizAnswersJson: String? = null,
    val hatchedAt: Long = 0L,
)
