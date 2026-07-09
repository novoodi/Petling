package com.example.petling.data.capture

import android.util.Log

/**
 * Gemini Nano 경로의 조용한 실패(폴백 원인)를 디버그 수준으로 남긴다.
 * detail에는 길이·개수·상태코드만 — 사용자 콘텐츠(OCR/STT 원문)는 절대 로그 금지.
 * 확인: adb logcat -s PetlingNano
 */
internal object NanoLog {
    private const val TAG = "PetlingNano"

    fun d(component: String, stage: String, detail: String = "") {
        Log.d(TAG, if (detail.isEmpty()) "$component:$stage" else "$component:$stage $detail")
    }
}
