package com.example.petling.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Petling 컬러 토큰.
 * .design-import 디자인 시스템(Toss 스타일)의 토큰 구조를 유지하되,
 * 브랜드 컬러만 도토리 오렌지 계열로 치환. 순수 검정(#000000) 사용 금지.
 */

// ── Brand (도토리 오렌지) ──
val Brand50 = Color(0xFFFDF3EC)
val Brand100 = Color(0xFFFAE3D3)
val Brand200 = Color(0xFFF5C7A8)
val Brand300 = Color(0xFFEFA97C)
val Brand400 = Color(0xFFE8905A)
val Brand500 = Color(0xFFE07A3F) // Brand main
val Brand600 = Color(0xFFC4632D)
val Brand700 = Color(0xFF9C4E24)
val Brand800 = Color(0xFF773B1B)

// ── Neutral (웜 틴트 그레이 — 순검정 금지) ──
val Neutral50 = Color(0xFFFAF8F4)
val Neutral100 = Color(0xFFF4F1EB)
val Neutral200 = Color(0xFFE8E4DB)
val Neutral300 = Color(0xFFCFC9BD)
val Neutral400 = Color(0xFFA49E92)
val Neutral500 = Color(0xFF7A756B)
val Neutral600 = Color(0xFF615C53)
val Neutral700 = Color(0xFF453F37)
val Neutral800 = Color(0xFF322E28)
val Neutral900 = Color(0xFF262320)
val Neutral950 = Color(0xFF1E1B18) // Primary text

// ── Warning / Error (파스텔 — 위협적이지 않게) ──
val Red50 = Color(0xFFFFF0F0)
val Red100 = Color(0xFFFFD6D6)
val Red200 = Color(0xFFFFA8A8)
val Red400 = Color(0xFFF87171)
val Red500 = Color(0xFFEF4444)

// ── 카테고리 컬러 (solid fg + pastel bg 페어) ──
val CategoryStudy = Color(0xFF1F4EF5)
val CategoryStudyBg = Color(0xFFEBF0FF)
val CategoryAppointment = Color(0xFF8B5CF6)
val CategoryAppointmentBg = Color(0xFFF5F3FF)
val CategoryHobby = Color(0xFFF59E0B)
val CategoryHobbyBg = Color(0xFFFFFBEB)
val CategoryRest = Color(0xFF22C55E)
val CategoryRestBg = Color(0xFFF0FFF4)

// ── Success ──
val Green500 = Color(0xFF22C55E)
val Green600 = Color(0xFF16A34A)

// ── Semantic ──
val TextPrimary = Neutral950
val TextSecondary = Neutral600
val TextTertiary = Neutral400
val TextDisabled = Neutral300
val TextOnBrand = Color.White

val SurfaceBase = Color.White
val SurfaceSubtle = Neutral50
val SurfaceCard = Color.White
val SurfaceWarning = Red50

val BorderDefault = Neutral200
val BorderStrong = Neutral300
