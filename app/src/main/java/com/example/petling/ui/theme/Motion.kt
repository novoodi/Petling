package com.example.petling.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/**
 * 모션 토큰 — 디자인 시스템 수치 준수.
 * press scale(0.97) 120ms / 시트·드로어 280ms decelerate / 일반 전환 150~200ms.
 */
object Motion {
    // ── Durations (ms) ──
    const val Instant = 80
    const val Press = 120
    const val Fast = 150
    const val Normal = 200
    const val Slow = 280 // 시트/드로어
    const val Deliberate = 350

    // ── Easing ──
    val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val Decelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)   // 등장
    val Accelerate = CubicBezierEasing(0.4f, 0f, 1f, 1f)   // 퇴장
    val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f) // 장난스러운 스프링

    // ── Press ──
    const val PressScale = 0.97f
    const val DisabledAlpha = 0.38f
}
