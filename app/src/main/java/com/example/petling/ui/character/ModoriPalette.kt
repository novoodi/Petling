package com.example.petling.ui.character

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs

/**
 * colorHue 하나에서 캐릭터 전체 색을 파생한다.
 *
 * 실사풍(치비 실사) 방향에 맞춰, 슬라이더 hue를 그대로 쓰지 않고 "자연 코트 색"으로 매핑한다:
 * - 브라운·러스트 계열(웜)은 채도를 살리고, 파랑·보라 계열(쿨)은 그레이시 파스텔로 눌러 네온을 막는다.
 * - 명도는 0.60~0.66 좁은 범위로 클램프해 항상 부드러운 털 톤을 유지한다.
 *
 * [body]/[bodyShadow]/[bodyHighlight]는 몸통 볼륨, [belly]는 가슴·배·주둥이 크림,
 * [earInner] 속귀, [marking] 다크 포인트(귀끝·줄무늬·깃), [iris] 기본 홍채(종별 오버라이드 가능),
 * [nose] 코. [cap]/[capShadow]는 도토리 깍정이 전용(hue 무관 고정 브라운).
 */
data class ModoriPalette(
    val body: Color,
    val bodyShadow: Color,
    val bodyHighlight: Color,
    val cap: Color,
    val capShadow: Color,
    val cheek: Color,
    val belly: Color = Color(0xFFFBF5EA),
    val earInner: Color = Color(0xFFEAB4BE),
    val marking: Color = Color(0xFF4A3A2C),
    val iris: Color = Color(0xFF7A4A1E),
    val nose: Color = Color(0xFF3A2E28),
) {
    /** 몸통 볼륨 표현용 세로 그라디언트 색(위=하이라이트 → 아래=본색). */
    val bodyGradient: List<Color> get() = listOf(bodyHighlight, body)

    companion object {
        /**
         * 종별 특례 팔레트. 판다·펭귄은 흑백 마킹 고정에 몸 바탕만 hue 미세 틴트(채도 상한)
         * — 슬라이더 UX는 유지하되 "보라 판다"를 막는다.
         */
        fun from(hue: Float, species: com.example.petling.domain.model.Species?): ModoriPalette {
            val base = from(hue)
            return when (species) {
                com.example.petling.domain.model.Species.PANDA -> base.copy(
                    body = desat(hue, 0.06f, 0.90f),
                    bodyShadow = desat(hue, 0.08f, 0.78f),
                    bodyHighlight = desat(hue, 0.04f, 0.96f),
                    belly = Color(0xFFF7F4EE),
                    marking = Color(0xFF2F2C2A),
                    nose = Color(0xFF241C16),
                )
                com.example.petling.domain.model.Species.PENGUIN -> base.copy(
                    body = desat(hue, 0.10f, 0.30f), // 등·머리 다크 슬레이트
                    bodyShadow = desat(hue, 0.10f, 0.22f),
                    bodyHighlight = desat(hue, 0.10f, 0.40f),
                    belly = Color(0xFFF7F4EE),
                    marking = Color(0xFF1C1A18),
                )
                else -> base
            }
        }

        private fun desat(hue: Float, s: Float, l: Float): Color {
            val h = ((hue % 360f) + 360f) % 360f
            return Color.hsl(h, s, l)
        }

        fun from(hue: Float): ModoriPalette {
            val h = ((hue % 360f) + 360f) % 360f
            // 35° = 자연 코트 중심(브라운·러스트). 여기서 멀수록 쿨→그레이시하게.
            val dist = abs((((h - 35f) + 540f) % 360f) - 180f) // 0(=35°)..180(반대)
            val warmth = 1f - dist / 180f
            val s = 0.20f + 0.35f * warmth
            val l = 0.60f + 0.06f * (1f - warmth * 0.5f)

            val bodyC = hsl(h, s, l)
            return ModoriPalette(
                body = bodyC,
                bodyShadow = hsl(h, (s + 0.06f).coerceAtMost(0.62f), l - 0.13f),
                bodyHighlight = hsl(h, (s - 0.06f).coerceAtLeast(0.10f), l + 0.09f),
                // 깍정이(도토리 모자)는 hue 무관 따뜻한 브라운 고정
                cap = hsl(28f, 0.45f, 0.38f),
                capShadow = hsl(28f, 0.45f, 0.30f),
                cheek = hsl(8f, 0.55f, 0.78f), // 볼터치는 hue 무관 웜 핑크 고정
                belly = hsl(h, 0.08f + 0.08f * warmth, 0.93f),
                earInner = lerp(hsl(8f, 0.42f, 0.80f), bodyC, 0.25f),
                marking = hsl(h, s * 0.7f, 0.28f),
                iris = hsl(38f, 0.55f, 0.42f), // 앰버 기본(종별로 드로잉에서 오버라이드)
                nose = hsl(h, 0.25f, 0.24f),
            )
        }

        private fun hsl(h: Float, s: Float, l: Float): Color = Color.hsl(h, s, l)
    }
}
