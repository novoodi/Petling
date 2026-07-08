package com.example.petling.domain.engine

/**
 * 호감도(0..100) 단계. 단계명만 UI에 노출하고 기질·원값 규칙은 숨긴다(키우며 발견).
 */
enum class AffectionLevel(val displayName: String, val min: Int) {
    STRANGER("낯섦", 0),
    FAMILIAR("익숙", 25),
    CLOSE("친함", 50),
    BONDED("단짝", 80),
}

/**
 * 호감도 규칙 — 순수 함수.
 *
 * 획득: 하루 첫 방문 +3 / 일정 완료 +2 / 캡처 정리 +1 / 간식 +5(하루 3개).
 * 일일 총 상한 20(농사 방지). 감쇠: 3일+ 미접속 복귀 시 -min(일수,7),
 * 단 **현재 단계의 min을 바닥**으로 — 단계는 절대 떨어지지 않는다(죄책감 금지 원칙).
 */
object AffectionRules {
    const val MAX = 100
    const val DAILY_GAIN_CAP = 20
    const val FIRST_VISIT = 3
    const val COMPLETE = 2
    const val CAPTURE = 1
    const val SNACK = 5
    const val SNACKS_PER_DAY = 3

    /** 마이그레이션 시 기존 사용자 시작값(이미 키워온 관계 — 익숙 단계). */
    const val LEGACY_START = 30

    fun levelFor(affection: Int): AffectionLevel =
        AffectionLevel.entries.last { affection.coerceIn(0, MAX) >= it.min }

    data class Applied(val newValue: Int, val gained: Int, val levelUp: AffectionLevel?)

    /** 가산 적용: 일일 상한·0..100 클램프. [gainedToday]는 오늘 이미 얻은 총량. */
    fun apply(current: Int, gainedToday: Int, gain: Int): Applied {
        val room = (DAILY_GAIN_CAP - gainedToday).coerceAtLeast(0)
        val actual = gain.coerceAtMost(room).coerceAtLeast(0)
        val before = current.coerceIn(0, MAX)
        val after = (before + actual).coerceAtMost(MAX)
        val levelUp = levelFor(after).takeIf { it != levelFor(before) && after > before }
        return Applied(after, after - before, levelUp)
    }

    /** 미접속 복귀 감쇠. 단계 min이 바닥(단계 비하락). */
    fun decayOnReturn(current: Int, daysAway: Long): Int {
        if (daysAway < 3) return current
        val floor = levelFor(current).min
        return (current - daysAway.coerceAtMost(7L).toInt()).coerceAtLeast(floor)
    }
}
