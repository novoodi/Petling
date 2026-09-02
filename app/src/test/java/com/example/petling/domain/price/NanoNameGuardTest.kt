package com.example.petling.domain.price

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NanoNameGuardTest {

    private val ocr = """
        존쿡 델리미트 캠핑파티 840g
        신세계포인트
        10g당 176원
        14,780
        11,980
    """.trimIndent()

    @Test
    fun `용량 토큰만 겹치는 깨진 이름은 거부한다 - 폴드6 QA 사례`() {
        assertFalse(NanoNameGuard.isSupportedByOcr("존국리트핑파다 840g", ocr))
    }

    @Test
    fun `한글 토큰이 OCR에 있으면 채택한다`() {
        assertTrue(NanoNameGuard.isSupportedByOcr("존쿡 델리미트 캠핑파티", ocr))
        assertTrue(NanoNameGuard.isSupportedByOcr("존쿡 캠핑파티 840g", ocr))
    }

    @Test
    fun `한 글자 우연 일치는 증거가 아니다`() {
        assertFalse(NanoNameGuard.isSupportedByOcr("존 파 티", ocr))
    }

    @Test
    fun `용량 토큰 제거와 포함 판정`() {
        assertEquals("존쿡 캠핑파티", NanoNameGuard.stripVolumeTokens("존쿡 캠핑파티 840g"))
        assertEquals("신라면", NanoNameGuard.stripVolumeTokens("신라면 5입"))
        assertTrue(NanoNameGuard.containsVolume("맥심 원두커피 2kg"))
        assertFalse(NanoNameGuard.containsVolume("맥심 원두커피"))
    }
}
