package com.example.petling.notifications

/**
 * MediaStore 항목이 "스크린샷"인지 판별하는 순수 로직(테스트 가능).
 * 갤럭시/안드로이드 공통: 스크린샷은 Pictures/Screenshots 또는 DCIM/Screenshots에 저장되고
 * 파일명이 "Screenshot_..."으로 시작한다.
 */
object ScreenshotDetector {

    fun isScreenshot(relativePath: String?, displayName: String?): Boolean {
        val path = relativePath?.lowercase().orEmpty()
        val name = displayName?.lowercase().orEmpty()
        return path.contains("screenshots") || name.startsWith("screenshot")
    }
}
