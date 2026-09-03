package com.example.petling.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {

    private val today = LocalDate.of(2026, 9, 3)

    @Test
    fun `이마트형 - 한 줄에 단가 수량 금액, 할인은 직전 상품에 반영, 합계 이후는 버린다`() {
        val text = """
            이마트 성수점
            (주)이마트 123-45-67890
            2026-09-02 18:22
            상품명          단가   수량   금액
            서울우유1L       2,980    1   2,980
            *신라면5입       4,150    2   8,300
            CJ깨끗한계란15구  6,480    1   6,480
            행사할인                      -1,000
            봉투              100    1     100
            합  계                     16,860
            받은금액                    20,000
            거스름돈                     3,140
            카드승인 1234-****-5678
        """.trimIndent()
        val d = ReceiptParser.parse(text, today)
        assertEquals("이마트 성수점", d.storeName)
        assertEquals(LocalDate.of(2026, 9, 2).toEpochDay(), d.dateEpochDay)
        assertEquals(listOf("서울우유1L", "신라면5입", "CJ깨끗한계란15구"), d.items.map { it.name })
        assertEquals(2_980, d.items[0].unitPriceWon)
        assertEquals(2, d.items[1].quantity)
        assertEquals(4_150, d.items[1].unitPriceWon)
        assertEquals(8_300, d.items[1].totalWon)
        // 할인 1,000원은 계란에 적용
        assertEquals(5_480, d.items[2].totalWon)
        assertEquals(5_480, d.items[2].unitPriceWon)
        assertEquals(16_860, d.totalWon)
        // 결제 정보는 상품으로 잡히지 않는다
        assertTrue(d.items.none { it.name.contains("카드") || it.name.contains("거스름") })
    }

    @Test
    fun `편의점형 - 상품명 뒤에 바로 금액, 날짜는 점 구분`() {
        val text = """
            GS25 역삼점
            2026.09.01 21:05
            포카칩오리지널66G      1,700  1   1,700
            코카콜라500ML         2,000  2   4,000
            합계                             5,700
        """.trimIndent()
        val d = ReceiptParser.parse(text, today)
        assertEquals("GS25 역삼점", d.storeName)
        assertEquals(LocalDate.of(2026, 9, 1).toEpochDay(), d.dateEpochDay)
        assertEquals(2, d.items.size)
        assertEquals(2_000, d.items[1].unitPriceWon)
        assertEquals(4_000, d.items[1].totalWon)
    }

    @Test
    fun `두 줄형 - 상품명 줄 다음에 숫자 줄`() {
        val text = """
            홈플러스 월드컵점
            26/09/02
            농심 신라면 5입
                    4,150    1    4,150
            8801043012345
            서울우유 흰우유 1L
                    2,980
            합 계             7,130
        """.trimIndent()
        val d = ReceiptParser.parse(text, today)
        assertEquals("홈플러스 월드컵점", d.storeName)
        assertEquals(LocalDate.of(2026, 9, 2).toEpochDay(), d.dateEpochDay)
        assertEquals(listOf("농심 신라면 5입", "서울우유 흰우유 1L"), d.items.map { it.name })
        assertEquals(2_980, d.items[1].unitPriceWon)
        assertEquals(7_130, d.totalWon)
    }

    @Test
    fun `미래 날짜와 체인명 없는 영수증`() {
        val text = """
            우리동네마트
            2027-01-01
            두부 1,500
        """.trimIndent()
        val d = ReceiptParser.parse(text, today)
        assertEquals("우리동네마트", d.storeName)
        assertNull(d.dateEpochDay)
        assertEquals(1, d.items.size)
        assertEquals(1_500, d.items[0].unitPriceWon)
    }

    @Test
    fun `상품이 없으면 비어 있다`() {
        val d = ReceiptParser.parse("안녕하세요\n감사합니다", today)
        assertTrue(d.isEmpty)
    }
}
