package com.example.petling.domain.price

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 골든 케이스: 트레이더스 실물 가격표 4장(2026-08-30 촬영)의 OCR 근사 텍스트.
 * 실제 ML Kit 결과와 줄 순서·공백이 다를 수 있으나 구성 요소는 동일하다.
 */
class RuleBasedPriceTagExtractorTest {

    private val extractor = RuleBasedPriceTagExtractor()

    // ── 1. 할인 가격표: 정가 14,780 - 신세계포인트 2,800 = 11,980, 행사기간·바코드 포함
    private val johncook = """
        존쿡 델리미트 캠핑파티 840g
        신세계포인트
        JOHNCOOK CAMPING PARTY
        10g당 176원
        - 6가지 다양한 소시지를 한 번에 즐길 수 있는 소시지 파티
        - 롤,치즈,김치,그릴,화이트,컨츄리 6가지 맛이 한팩에!
        - 여행, 바베큐, 캠핑 등에 적합한 상품
        14,780
        신세계포인트 적립 할인
        -2,800
        11,980
        8809861815462
        행사기간 : 20260817 ~ 20260823
    """.trimIndent()

    @Test
    fun `할인 가격표 - 최종가와 정가를 구분한다`() = runTest {
        val tag = extractor.extract(johncook)
        assertEquals("존쿡 델리미트 캠핑파티", tag.name)
        assertEquals(11_980, tag.priceWon)
        assertEquals(14_780, tag.originalPriceWon)
        assertEquals(840.0, tag.volumeAmount!!, 0.001)
        assertEquals("g", tag.volumeUnit)
        assertEquals(176, tag.unitPriceWon)
        assertEquals("8809861815462", tag.barcode)
        assertEquals(LocalDate.of(2026, 8, 23).toEpochDay(), tag.saleEndEpochDay)
    }

    // ── 2. 단순 가격표 + 이웃 가격표 일부 혼입(당근라페)
    private val greenOnion = """
        파채 500g(팩)
        JULIENNE GREEN ONION
        100g당 936원
        - 대파를 얇게 썰어 간편하게 포장
        - 육류 요리와 구이에 곁들어 먹음
        4,680
        2500000217081
        당근라페
        Carrot Rapee
        100g당 998원
    """.trimIndent()

    @Test
    fun `단순 가격표 - 이웃 태그가 섞여도 첫 상품을 잡는다`() = runTest {
        val tag = extractor.extract(greenOnion)
        assertEquals("파채", tag.name)
        assertEquals(4_680, tag.priceWon)
        assertNull(tag.originalPriceWon)
        assertEquals(500.0, tag.volumeAmount!!, 0.001)
        assertEquals(936, tag.unitPriceWon)
        assertEquals("2500000217081", tag.barcode)
    }

    // ── 3. 단위가 교차검증: 1,645 × (400/100) = 6,580
    private val salad = """
        스마트팜 영리프 샐러드 400g
        100g당 1,645원
        6,580
        2507281101610
        와일드
        ARUGU
        100g당 3
    """.trimIndent()

    @Test
    fun `단위가 교차검증으로 가격을 확정한다`() = runTest {
        val tag = extractor.extract(salad)
        assertEquals("스마트팜 영리프 샐러드", tag.name)
        assertEquals(6_580, tag.priceWon)
        assertEquals(400.0, tag.volumeAmount!!, 0.001)
        assertEquals(1_645, tag.unitPriceWon)
    }

    // ── 4. 용량·단위가 없는 상품 + 이웃 매대 가격 혼입 → 미확정, 후보만 노출
    private val tteokbokki = """
        19,980
        15,980
        신당동식 즉석떡볶이
        TTEOKBOKKI
        - 매콤달콤한 소스에 약간의 짜장 베이스가 더해진 신당동식 즉석떡볶이
        - 튀김만두,라면사리,쫄면,어묵 등 다양한 구성으로 조화로운 맛이 특징
        14,980
        2507281089253
    """.trimIndent()

    @Test
    fun `애매하면 확정하지 않고 후보를 노출한다`() = runTest {
        val tag = extractor.extract(tteokbokki)
        assertEquals("신당동식 즉석떡볶이", tag.name)
        assertNull(tag.priceWon)
        assertTrue(tag.priceCandidatesWon.containsAll(listOf(19_980, 15_980, 14_980)))
    }

    @Test
    fun `할인액의 마이너스를 OCR이 놓쳐도 삼각형으로 복원한다`() = runTest {
        val text = johncook.replace("-2,800", "2,800")
        val tag = extractor.extract(text)
        assertEquals(11_980, tag.priceWon)
        assertEquals(14_780, tag.originalPriceWon)
    }

    @Test
    fun `빈 텍스트는 빈 결과`() = runTest {
        val tag = extractor.extract("")
        assertTrue(tag.isEmpty)
    }
}
