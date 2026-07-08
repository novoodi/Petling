package com.example.petling.domain.model

/**
 * 동물 종류. 온보딩 부화(랜덤)로 하나를 만나 키운다.
 * [defaultHue]는 몸 색상 슬라이더의 기본값(사용자가 바꿀 수 있음).
 * [pickable]=false인 종(도토리)은 부화 풀에서 제외 — 기존 사용자 렌더 호환용 레거시/마스코트.
 */
enum class Species(val displayName: String, val emoji: String, val defaultHue: Float, val pickable: Boolean = true) {
    ACORN("도토리", "🌰", 30f, pickable = false),
    FOX("여우", "🦊", 24f),
    CAT("고양이", "🐱", 265f),
    RABBIT("토끼", "🐰", 336f),
    CHICK("병아리", "🐤", 46f),
    DOG("강아지", "🐶", 30f),
    HAMSTER("햄스터", "🐹", 38f),
    PENGUIN("펭귄", "🐧", 210f),
    PANDA("판다", "🐼", 45f),
}
