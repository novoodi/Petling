package com.example.petling.domain.model

/** 정리함에 보관되는 캡처 한 건. [categoryKey]는 [Category.key]. */
data class CaptureItem(
    val id: Long = 0L,
    val imagePath: String,
    val ocrText: String,
    val categoryKey: String,
    val title: String,
    /** 온디바이스 AI 한줄 요약(탭 시 생성·캐시). */
    val summary: String? = null,
    val note: String? = null,
    /** 화면 속에서 뽑아낸 링크(있으면 '열기' 제공). */
    val linkUrl: String? = null,
    val sourceScheduleId: Long? = null,
    val pinned: Boolean = false,
    val createdAt: Long = 0L,
)
