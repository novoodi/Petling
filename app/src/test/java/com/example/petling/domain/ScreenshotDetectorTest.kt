package com.example.petling.domain

import com.example.petling.notifications.ScreenshotDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotDetectorTest {

    @Test
    fun screenshots_folder_is_screenshot() {
        assertTrue(ScreenshotDetector.isScreenshot("Pictures/Screenshots/", "Screenshot_20240814.jpg"))
        assertTrue(ScreenshotDetector.isScreenshot("DCIM/Screenshots/", "img.png"))
    }

    @Test
    fun screenshot_filename_prefix_is_screenshot() {
        assertTrue(ScreenshotDetector.isScreenshot("Pictures/", "Screenshot_20240814_105746_KakaoTalk.jpg"))
        assertTrue(ScreenshotDetector.isScreenshot(null, "screenshot-1.png"))
    }

    @Test
    fun normal_photo_is_not_screenshot() {
        assertFalse(ScreenshotDetector.isScreenshot("DCIM/Camera/", "IMG_20240814.jpg"))
        assertFalse(ScreenshotDetector.isScreenshot("Pictures/Instagram/", "insta.jpg"))
    }

    @Test
    fun nulls_are_not_screenshot() {
        assertFalse(ScreenshotDetector.isScreenshot(null, null))
        assertFalse(ScreenshotDetector.isScreenshot("", ""))
    }

    @Test
    fun case_insensitive() {
        assertTrue(ScreenshotDetector.isScreenshot("PICTURES/SCREENSHOTS/", "FOO.JPG"))
    }
}
