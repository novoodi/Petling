package com.example.petling.ui.character

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.petling.domain.model.Species
import kotlin.math.abs

/**
 * 캐릭터 팔레트.
 *
 * 리디자인(포켓몬식) 원칙: 몸 색은 슬라이더가 아니라 "종 고유 코트 컬러"가 기본이다.
 * - 종마다 실제 동물의 코트 컬러를 고정([coatFor]) — 여우=진주황, 고양이=블루그레이, 판다=흑백 …
 * - 사용자 colorHue는 [accent]/[accentDark]로 파생돼 목걸이·밴다나·화관 등 "소품"에만 입힌다.
 *   (개성(코트)과 커스텀(액센트)을 양립시키는 구조)
 * - [outline]은 전 종 공통 웜 다크 아웃라인 — 스타일의 뼈대.
 * - ACORN(유아 마스코트)만 기존 hue 파생 파스텔을 유지한다.
 *
 * [body]/[bodyShadow]/[bodyHighlight]는 몸통 볼륨, [belly]는 가슴·배·주둥이 크림,
 * [earInner] 속귀, [marking] 다크 포인트(귀끝·줄무늬·패치·삭스), [nose] 코.
 * [cap]/[capShadow]는 도토리 깍정이 전용(hue 무관 고정 브라운).
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
    val outline: Color = OUTLINE,
    val accent: Color = Color(0xFFD94F3D),
    val accentDark: Color = Color(0xFFB03A2C),
) {
    /** 몸통 볼륨 표현용 세로 그라디언트 색(위=하이라이트 → 아래=본색). */
    val bodyGradient: List<Color> get() = listOf(bodyHighlight, body)

    companion object {
        /** 전 종 공통 웜 다크 아웃라인. */
        val OUTLINE = Color(0xFF3F2E22)

        /**
         * 종별 팔레트. 코트는 [coatFor] 고정, colorHue는 액센트 소품 색으로만 파생된다.
         * species가 null이거나 ACORN이면 기존 hue 파생 파스텔(마스코트 톤).
         */
        fun from(hue: Float, species: Species?): ModoriPalette {
            val base = coatFor(species) ?: from(hue)
            val h = ((hue % 360f) + 360f) % 360f
            return base.copy(
                accent = Color.hsl(h, 0.60f, 0.52f),
                accentDark = Color.hsl(h, 0.60f, 0.40f),
            )
        }

        /** 종 고유 코트 컬러(리디자인 목업 기준). ACORN·null은 hue 파생으로 폴백. */
        private fun coatFor(species: Species?): ModoriPalette? = when (species) {
            Species.FOX -> ModoriPalette(
                body = Color(0xFFF08A24),
                bodyShadow = Color(0xFFD67415),
                bodyHighlight = Color(0xFFF8A854),
                cap = CAP, capShadow = CAP_SHADOW, cheek = Color(0xFFF6A96B),
                belly = Color(0xFFFFEFD9),
                earInner = Color(0xFFF7B98B),
                marking = Color(0xFF4A3527), // 귀끝·발끝 삭스
                nose = Color(0xFF3F2E22),
            )
            Species.CAT -> ModoriPalette(
                body = Color(0xFF97A5BC),
                bodyShadow = Color(0xFF7C8AA3),
                bodyHighlight = Color(0xFFAFBCCD),
                cap = CAP, capShadow = CAP_SHADOW, cheek = CHEEK_PINK,
                belly = Color(0xFFF2EEE6),
                earInner = Color(0xFFE8A6B5),
                marking = Color(0xFF5B6478), // M 무늬·꼬리 링
                nose = Color(0xFFE58AA0),
            )
            Species.RABBIT -> ModoriPalette(
                body = Color(0xFFF6EFE3),
                bodyShadow = Color(0xFFE2D6C2),
                bodyHighlight = Color(0xFFFFFDF8),
                cap = CAP, capShadow = CAP_SHADOW, cheek = Color(0xFFF5B8C4),
                belly = Color(0xFFFFFDF8),
                earInner = Color(0xFFF5B8C4),
                marking = Color(0xFFC4B39E),
                nose = Color(0xFFE8899E),
            )
            Species.CHICK -> ModoriPalette(
                body = Color(0xFFFFD84D),
                bodyShadow = Color(0xFFF0BE2E),
                bodyHighlight = Color(0xFFFFE68A),
                cap = CAP, capShadow = CAP_SHADOW, cheek = Color(0xFFFBAE5C),
                belly = Color(0xFFFFE68A),
                earInner = Color(0xFFFBAE5C),
                marking = Color(0xFFDA8420), // 깃 라인
                nose = Color(0xFFDA8420),
            )
            Species.DOG -> ModoriPalette(
                body = Color(0xFFD69A5B),
                bodyShadow = Color(0xFFC08544),
                bodyHighlight = Color(0xFFE5B075),
                cap = CAP, capShadow = CAP_SHADOW, cheek = CHEEK_PINK,
                belly = Color(0xFFF7E9D2),
                earInner = Color(0xFFE3AF8E),
                marking = Color(0xFF8A5A33), // 초콜릿 귀·눈 패치
                nose = Color(0xFF3F2E22),
            )
            Species.HAMSTER -> ModoriPalette(
                body = Color(0xFFEFB54F),
                bodyShadow = Color(0xFFDE9F38),
                bodyHighlight = Color(0xFFF7CE7E),
                cap = CAP, capShadow = CAP_SHADOW, cheek = Color(0xFFF3B9A0),
                belly = Color(0xFFFBEBCB),
                earInner = Color(0xFFE39BA8),
                marking = Color(0xFF8A6B3E),
                nose = Color(0xFFD98A96),
            )
            Species.PENGUIN -> ModoriPalette(
                body = Color(0xFF33415C),
                bodyShadow = Color(0xFF283349),
                bodyHighlight = Color(0xFF43536F),
                cap = CAP, capShadow = CAP_SHADOW, cheek = Color(0xFFE8A0A8),
                belly = Color(0xFFFDFBF6),
                earInner = Color(0xFFE8A0A8),
                marking = Color(0xFF1F2A3E),
                nose = Color(0xFF14100D),
            )
            Species.PANDA -> ModoriPalette(
                body = Color(0xFFF7F4EE),
                bodyShadow = Color(0xFFE7E1D4),
                bodyHighlight = Color(0xFFFFFFFF),
                cap = CAP, capShadow = CAP_SHADOW, cheek = CHEEK_PINK,
                belly = Color(0xFFF7F4EE),
                earInner = Color(0xFFE8B7C0),
                marking = Color(0xFF3A3633), // 귀·팔다리·눈 패치
                nose = Color(0xFF2B2724),
            )
            Species.ACORN, null -> null
        }

        private val CAP = Color.hsl(28f, 0.45f, 0.38f)
        private val CAP_SHADOW = Color.hsl(28f, 0.45f, 0.30f)
        private val CHEEK_PINK = Color.hsl(8f, 0.55f, 0.78f)

        /**
         * hue 파생 파스텔(도토리 마스코트·하위호환).
         * 브라운·러스트 계열(웜)은 채도를 살리고, 쿨 계열은 그레이시 파스텔로 눌러 네온을 막는다.
         */
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
                cap = CAP,
                capShadow = CAP_SHADOW,
                cheek = CHEEK_PINK, // 볼터치는 hue 무관 웜 핑크 고정
                belly = hsl(h, 0.08f + 0.08f * warmth, 0.93f),
                earInner = lerp(hsl(8f, 0.42f, 0.80f), bodyC, 0.25f),
                marking = hsl(h, s * 0.7f, 0.28f),
                iris = hsl(38f, 0.55f, 0.42f), // 앰버 기본(종별로 드로잉에서 오버라이드)
                nose = hsl(h, 0.25f, 0.24f),
                accent = hsl(h, 0.60f, 0.52f),
                accentDark = hsl(h, 0.60f, 0.40f),
            )
        }

        private fun hsl(h: Float, s: Float, l: Float): Color = Color.hsl(h, s, l)
    }
}
