package com.example.petling.domain.model

/**
 * 스타팅 동물 종류. 온보딩에서 하나를 골라 그 친구를 키운다.
 * [defaultHue]는 선택 시 몸 색상 슬라이더의 기본값(사용자가 바꿀 수 있음).
 */
enum class Species(val displayName: String, val emoji: String, val defaultHue: Float) {
    ACORN("도토리", "🌰", 30f),
    FOX("여우", "🦊", 24f),
    CAT("고양이", "🐱", 265f),
    RABBIT("토끼", "🐰", 336f),
    CHICK("병아리", "🐤", 46f),
}
