package com.example.petling.domain.engine

import java.time.LocalDate

/**
 * 스트릭 = "하루 1건 이상 완료한 날"의 연속 수.
 * 완료 시점마다 호출해 갱신하며, 갱신된 값으로 XP 배율을 계산한다.
 */
object StreakCalculator {

    fun onCompletion(
        lastCompletionDay: LocalDate?,
        today: LocalDate,
        currentStreak: Int,
    ): Int = when {
        lastCompletionDay == null -> 1
        lastCompletionDay == today -> maxOf(currentStreak, 1) // 같은 날 추가 완료: 유지
        lastCompletionDay == today.minusDays(1) -> currentStreak + 1 // 어제도 완료: 연장
        else -> 1 // 공백 발생: 리셋 후 오늘부터 다시
    }

    /**
     * 완료 이력으로부터 스트릭을 재계산한다(완료 취소 시 정확 복원용).
     * 가장 최근 완료일(anchorDay)에서 하루씩 거슬러 연속된 완료일 수를 센다.
     */
    fun streakEndingAt(anchorDay: Long?, completedDays: Set<Long>): Int {
        if (anchorDay == null || anchorDay !in completedDays) return 0
        var day = anchorDay
        var streak = 0
        while (day in completedDays) {
            streak++
            day--
        }
        return streak
    }
}
