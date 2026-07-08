package com.example.petling.domain.engine

import com.example.petling.domain.model.XpReason
import kotlin.math.roundToInt

/**
 * XP 획득 규칙 (character_concept_growth_system.md 3.2).
 * 등록은 작게(+1, 배율 미적용), 이행은 크게(+5/+8, 스트릭 배율 적용).
 */
object XpRules {
    const val REGISTER = 1
    const val COMPLETE = 5
    const val COMPLETE_IMPORTANT = 8

    // 정리함: 캡처 보관 XP. "많이"보다 "다양하게"를 보상.
    const val CAPTURE = 2
    const val CAPTURE_VARIETY_BONUS = 8

    fun streakMultiplier(streakDays: Int): Double = when {
        streakDays >= 7 -> 1.5
        streakDays >= 3 -> 1.2
        else -> 1.0
    }

    /** 캡처 1건 보관 시 지급 XP. 처음 캡처하는 종류면 다양성 보너스. */
    fun captureXp(isNewType: Boolean): Int = CAPTURE + if (isNewType) CAPTURE_VARIETY_BONUS else 0
}

data class XpGain(
    val base: Int,
    val multiplier: Double,
    val total: Int,
    val reason: XpReason,
)

object XpEngine {

    /** 일정 등록 보상. 스트릭 배율은 "이행" 보너스이므로 적용하지 않는다. */
    fun registrationGain(): XpGain =
        XpGain(base = XpRules.REGISTER, multiplier = 1.0, total = XpRules.REGISTER, reason = XpReason.REGISTER)

    /** 일정 완료 보상. 갱신된 스트릭 일수를 넘겨야 한다(3일째 완료 그 건부터 배율 적용). */
    fun completionGain(isImportant: Boolean, streakDays: Int): XpGain {
        val base = if (isImportant) XpRules.COMPLETE_IMPORTANT else XpRules.COMPLETE
        val multiplier = XpRules.streakMultiplier(streakDays)
        return XpGain(
            base = base,
            multiplier = multiplier,
            total = (base * multiplier).roundToInt(),
            reason = if (isImportant) XpReason.COMPLETE_IMPORTANT else XpReason.COMPLETE,
        )
    }
}
