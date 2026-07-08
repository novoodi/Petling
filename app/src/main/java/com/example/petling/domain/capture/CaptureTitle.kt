package com.example.petling.domain.capture

/** 캡처 자동 제목 생성(규칙·Nano 분류기 공용). 첫 의미 있는 라인을 20자 내외로. */
object CaptureTitle {
    fun autoTitle(text: String): String {
        val firstLine = text.split("\n")
            .map { it.trim() }
            .firstOrNull { it.length >= 2 }
            ?: text.trim()
        val oneLine = firstLine.replace(Regex("\\s+"), " ").trim()
        return if (oneLine.length <= 20) oneLine else oneLine.take(20) + "…"
    }
}
