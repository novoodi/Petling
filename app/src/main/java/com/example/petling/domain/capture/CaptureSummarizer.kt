package com.example.petling.domain.capture

/** 캡처 내용을 한 문장으로 요약한다(온디바이스). 불가하면 null. */
interface CaptureSummarizer {
    suspend fun summarize(text: String): String?
}

/** 요약기 없음(비지원 기기). 항상 null. */
object NoOpCaptureSummarizer : CaptureSummarizer {
    override suspend fun summarize(text: String): String? = null
}
