package com.example.petling.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 간격/레이아웃 토큰 — 4dp 그리드, 디자인 시스템 수치 엄격 준수.
 */
object Dimens {
    // ── Base Scale (4dp grid) ──
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp
    val Space8 = 32.dp
    val Space10 = 40.dp
    val Space12 = 48.dp
    val Space16 = 64.dp

    // ── Screen Padding ──
    val ScreenPadding = 15.dp        // 목록형 화면 (홈/캘린더) — 개방감
    val ScreenPaddingFocused = 22.dp // 집중형 화면 (편집/설정)

    // ── Fixed Chrome ──
    val NavBarHeight = 50.dp
    val NavIconSize = 22.dp
    val ToolbarHeight = 42.dp

    // ── Touch Targets & Buttons ──
    val TouchMin = 44.dp
    val ButtonSm = 36.dp
    val ButtonMd = 44.dp
    val ButtonLg = 53.dp // Primary CTA

    // ── Border Radius ──
    val RadiusXs = 4.dp
    val RadiusSm = 6.dp
    val RadiusMd = 10.dp
    val RadiusLg = 14.dp // 카드
    val RadiusXl = 18.dp
    val Radius2Xl = 24.dp
}
