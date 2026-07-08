package com.example.petling.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Petling 테마 — 라이트 고정 (1단계 MVP), 다이나믹 컬러 미사용
 * (브랜드 아이덴티티 일관성 우선, 디자인 시스템 준수).
 */
private val LightColorScheme = lightColorScheme(
    primary = Brand500,
    onPrimary = TextOnBrand,
    primaryContainer = Brand50,
    onPrimaryContainer = Brand700,
    secondary = Neutral600,
    onSecondary = SurfaceBase,
    secondaryContainer = Neutral100,
    onSecondaryContainer = Neutral700,
    tertiary = Green600,
    onTertiary = SurfaceBase,
    error = Red500,
    onError = SurfaceBase,
    errorContainer = Red50,
    onErrorContainer = Red500,
    background = SurfaceSubtle,
    onBackground = TextPrimary,
    surface = SurfaceBase,
    onSurface = TextPrimary,
    surfaceVariant = Neutral100,
    onSurfaceVariant = TextSecondary,
    outline = BorderDefault,
    outlineVariant = Neutral100,
)

@Composable
fun PetlingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
