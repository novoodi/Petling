package com.example.petling.ui.character

import androidx.compose.ui.graphics.Color

/**
 * colorHue 하나에서 몸통 본색/음영/하이라이트와 깍정이(모자) 색을 파생한다.
 * HSL 기반으로 일관된 팔레트를 만들어 커스터마이징 슬라이더 하나로 전체가 변한다.
 */
data class ModoriPalette(
    val body: Color,
    val bodyShadow: Color,
    val bodyHighlight: Color,
    val cap: Color,
    val capShadow: Color,
    val cheek: Color,
) {
    companion object {
        fun from(hue: Float): ModoriPalette {
            val h = ((hue % 360f) + 360f) % 360f
            return ModoriPalette(
                body = hsl(h, 0.55f, 0.62f),
                bodyShadow = hsl(h, 0.50f, 0.50f),
                bodyHighlight = hsl(h, 0.60f, 0.75f),
                // 깍정이(도토리 모자)는 따뜻한 브라운 고정 계열
                cap = hsl(28f, 0.45f, 0.38f),
                capShadow = hsl(28f, 0.45f, 0.30f),
                cheek = hsl((h + 10f) % 360f, 0.70f, 0.72f),
            )
        }

        private fun hsl(h: Float, s: Float, l: Float): Color = Color.hsl(h, s, l)
    }
}
