package com.example.petling.domain.model

/**
 * 성장 단계. 전이 기준은 XP가 아니라 누적 "완료" 건수.
 * EGG는 온보딩 전용 상태로, 부화(온보딩 완료)로만 벗어난다.
 */
enum class GrowthStage(val requiredCompletions: Int, val displayName: String) {
    EGG(-1, "알"),
    JUVENILE(0, "유생기"),
    GROWTH1(10, "성장기 1"),
    GROWTH2(30, "성장기 2"),
    MATURE(60, "성숙기"),
}

enum class Personality(val displayName: String) {
    SINCERE("성실형"),
    FREE_SPIRIT("자유분방형"),
    WORRIER("걱정형"),
    DREAMER("몽상형"),
}

enum class Mood(val displayName: String) {
    HAPPY("기쁨"),
    CALM("평온"),
    TIRED("피곤"),
}

enum class Branch(val displayName: String) {
    STUDY("학업형"),
    HOBBY("취미형"),
    BALANCED("균형형"),
}

enum class ScheduleStatus {
    PENDING,
    COMPLETED,
    MISSED,
}

enum class ScheduleCategory(val displayName: String) {
    STUDY("학업"),
    APPOINTMENT("약속"),
    HOBBY("취미"),
    REST("휴식"),
}

enum class ScheduleSource {
    MANUAL,
    CAPTURE,
    VOICE,
    TEXT,
}

enum class XpReason {
    REGISTER,
    COMPLETE,
    COMPLETE_IMPORTANT,
    REVERT,
    CAPTURE,
    CAPTURE_VARIETY,
}

/** 캡처(스크린샷) 종류. 정리함 자동 분류의 기준. */
enum class CaptureType(val displayName: String) {
    SCHEDULE("일정"),
    STUDY("공부"),
    CHAT("대화"),
    LINK("링크·정보"),
    PLACE("장소"),
    SHOPPING("쇼핑"),
    MEMORY("추억"),
}
