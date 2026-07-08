package com.example.petling.domain.model

import java.time.LocalDate

/**
 * 일정 편집 화면을 프리필하기 위한 씨앗 값.
 * AI 파이프라인(2단계)이 ParsedScheduleDraft를 이 타입으로 변환해
 * 동일한 편집(확인/수정) 화면으로 진입시킨다. 지금은 자리만 잡아둔다.
 */
data class ParsedDraftSeed(
    val title: String? = null,
    val date: LocalDate? = null,
    val startMinuteOfDay: Int? = null,
    val location: String? = null,
    val category: ScheduleCategory? = null,
    val isImportant: Boolean = false,
    /** 0~1. 낮으면 확인 화면에서 해당 항목을 강조해 사용자 검토를 유도한다. */
    val confidence: Float = 0f,
    val source: ScheduleSource = ScheduleSource.MANUAL,
)
