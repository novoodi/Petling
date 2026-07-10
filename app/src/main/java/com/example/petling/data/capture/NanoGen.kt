package com.example.petling.data.capture

import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.GenerateContentResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Nano generateContent 공통 래퍼: 예외는 null(폴백 경로), 매달림은 타임아웃 차단.
 * 실기기 QA에서 재시도 호출이 결과 로그 없이 장시간 매달리는 패턴이 관찰됨 —
 * 무한 대기는 인제스트 UI와 canRegisterSchedule 판단을 함께 멈추게 한다.
 */
internal suspend fun GenerativeModel.generateOrNull(
    prompt: String,
    timeoutMs: Long = NANO_GEN_TIMEOUT_MS,
): GenerateContentResponse? = withTimeoutOrNull(timeoutMs) {
    try {
        generateContent(prompt)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

internal const val NANO_GEN_TIMEOUT_MS = 10_000L
