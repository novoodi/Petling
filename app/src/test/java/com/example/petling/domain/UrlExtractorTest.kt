package com.example.petling.domain

import com.example.petling.domain.capture.UrlExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test fun full_url() {
        assertEquals("https://youtube.com/watch?v=abc", UrlExtractor.firstUrl("이거 봐 https://youtube.com/watch?v=abc 웃김"))
    }

    @Test fun bare_domain_gets_https() {
        assertEquals("https://naver.me/xyz", UrlExtractor.firstUrl("공지 naver.me/xyz 확인"))
    }

    @Test fun www_domain() {
        assertEquals("https://www.example.com", UrlExtractor.firstUrl("www.example.com 참고"))
    }

    @Test fun trailing_punctuation_trimmed() {
        assertEquals("https://example.com/notes", UrlExtractor.firstUrl("자료(https://example.com/notes)."))
    }

    @Test fun no_url() {
        assertNull(UrlExtractor.firstUrl("그냥 평범한 텍스트입니다"))
    }
}
