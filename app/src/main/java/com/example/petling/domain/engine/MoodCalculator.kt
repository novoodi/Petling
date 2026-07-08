package com.example.petling.domain.engine

import com.example.petling.domain.model.Mood

/**
 * 일일 컨디션(기분) — 장기 성장(Stage)과 별개인 단기 지표.
 * 오늘 "지금까지 마감이 지난 일정" 대비 완료율로 계산한다.
 * 죄책감 유발 장치가 아니므로 최저 상태도 TIRED(피곤)에서 멈춘다.
 */
object MoodCalculator {

    fun calculate(dueSoFarToday: Int, completedToday: Int): Mood {
        if (dueSoFarToday <= 0) return Mood.CALM
        val rate = completedToday.toDouble() / dueSoFarToday
        return when {
            rate >= 0.7 -> Mood.HAPPY
            rate >= 0.3 -> Mood.CALM
            else -> Mood.TIRED
        }
    }
}
