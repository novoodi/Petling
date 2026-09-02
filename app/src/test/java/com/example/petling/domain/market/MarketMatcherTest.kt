package com.example.petling.domain.market

import com.example.petling.data.local.entity.MarketProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 참가격 실제 상품명(2026-08-28 게시본)으로 만든 골든 케이스. */
class MarketMatcherTest {

    private fun p(id: Long, name: String, total: Double? = null, unit: String? = null) = MarketProductEntity(
        id = id,
        name = name,
        normalizedName = MarketMatcher.normalize(MarketMatcher.coreName(name)),
        totalAmount = total,
        totalUnit = unit,
        cls = null,
    )

    private val products = listOf(
        p(1, "신라면(5개입)", 5.0, "EA"),
        p(2, "농심 신라면 큰사발면(114g)", 114.0, "G"),
        p(3, "진라면 매운맛(5개입)", 5.0, "EA"),
        p(4, "서울우유 흰우유(1L)", 1000.0, "ML"),
        p(5, "매일우유 오리지널(900ml)", 900.0, "ML"),
        p(6, "CJ 1등급 깨끗한 계란(15개)", 15.0, "EA"),
        p(7, "청정원 순창 양념듬뿍 쌈장(500g)", 500.0, "G"),
        p(8, "해표 꽃소금(1kg)", 1000.0, "G"),
        p(9, "임금님표 이천쌀(4kg)", 4000.0, "G"),
    )

    @Test
    fun `괄호 규격을 뺀 본체로 정규화한다`() {
        assertEquals("서울우유흰우유", MarketMatcher.normalize(MarketMatcher.coreName("서울우유 흰우유(1L)")))
        assertEquals("신라면", MarketMatcher.stripVolume("신라면 5입"))
        assertEquals("서울우유", MarketMatcher.stripVolume("서울우유 1L"))
    }

    @Test
    fun `신라면 5입은 신라면 5개입으로 확정된다`() {
        val m = MarketMatcher.match("신라면 5입", 5.0, "입", products)
        assertEquals(1L, m.best!!.product.id)
        assertTrue(m.confident)
    }

    @Test
    fun `용량이 없으면 신라면은 두 후보를 보여준다`() {
        val m = MarketMatcher.match("신라면", null, null, products)
        assertEquals(1L, m.best!!.product.id)
        assertTrue(m.candidates.any { it.product.id == 2L })
    }

    @Test
    fun `서울우유 1L는 용량 가산으로 확정된다`() {
        val m = MarketMatcher.match("서울우유", 1.0, "L", products)
        assertEquals(4L, m.best!!.product.id)
        assertTrue(m.confident)
    }

    @Test
    fun `계란 30구는 15개 상품과 용량이 달라 확정하지 않는다`() {
        val m = MarketMatcher.match("깨끗한 계란", 30.0, "구", products)
        assertEquals(6L, m.best!!.product.id)
        assertFalse(m.confident)
    }

    @Test
    fun `참가격에 없는 상품은 후보가 없다`() {
        assertNull(MarketMatcher.match("존쿡 델리미트 캠핑파티", 840.0, "g", products).best)
        assertNull(MarketMatcher.match("파채", null, null, products).best)
        assertNull(MarketMatcher.match("영리프 샐러드", 200.0, "g", products).best)
    }

    @Test
    fun `단위 환산 - kg L 개입`() {
        assertEquals(1000.0 to "g", MarketMatcher.baseAmount(1.0, "kg"))
        assertEquals(1000.0 to "ml", MarketMatcher.baseAmount(1.0, "L"))
        assertEquals(5.0 to "ea", MarketMatcher.baseAmount(5.0, "입"))
        assertNull(MarketMatcher.baseAmount(1.0, "묶음"))
        assertEquals(true, MarketMatcher.volumeMatches(1.0, "kg", products[7]))
        assertEquals(false, MarketMatcher.volumeMatches(500.0, "g", products[7]))
    }
}
