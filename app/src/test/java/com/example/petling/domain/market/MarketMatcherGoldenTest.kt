package com.example.petling.domain.market

import com.example.petling.data.local.entity.MarketProductEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 실제 게시본(martmemo-data index.json, 2026-08-28 조사)의 상품 604개 전체를 상대로 매칭한다.
 * 목적: 골든 가격표(트레이더스 4장)가 엉뚱한 참가격 상품에 붙지 않는지(오탐), 흔한 생필품은 붙는지.
 */
class MarketMatcherGoldenTest {

    @Serializable
    private data class Index(val products: List<Product>)

    @Serializable
    private data class Product(
        val id: Long,
        val name: String,
        val total: Double? = null,
        val totalUnit: String? = null,
    )

    private val products: List<MarketProductEntity> by lazy {
        val text = javaClass.getResourceAsStream("/market_index_20260828.json")!!.bufferedReader().readText()
        Json { ignoreUnknownKeys = true }.decodeFromString(Index.serializer(), text).products.map {
            MarketProductEntity(
                id = it.id,
                name = it.name,
                normalizedName = MarketMatcher.normalize(MarketMatcher.coreName(it.name)),
                totalAmount = it.total,
                totalUnit = it.totalUnit,
                cls = null,
            )
        }
    }

    @Test
    fun `게시본 상품 수`() {
        assertEquals(604, products.size)
    }

    @Test
    fun `골든 가격표 4장은 참가격에 없으므로 후보가 없어야 한다`() {
        // 존쿡 캠핑파티 / 파채 / 영리프 샐러드 / 즉석떡볶이 — RuleBasedPriceTagExtractorTest의 골든 케이스
        listOf(
            Triple("존쿡 델리미트 캠핑파티", 840.0, "g"),
            Triple("파채", 200.0, "g"),
            Triple("영리프 샐러드", 200.0, "g"),
            Triple("즉석떡볶이", 400.0, "g"),
        ).forEach { (name, amt, unit) ->
            val m = MarketMatcher.match(name, amt, unit, products)
            assertNull("오탐: $name → ${m.best?.product?.name}", m.best)
        }
    }

    @Test
    fun `흔한 생필품은 확정 또는 후보로 붙는다`() {
        val ramen = MarketMatcher.match("신라면 5입", 5.0, "입", products)
        assertEquals("신라면(5개입)", ramen.best!!.product.name)
        assertTrue(ramen.confident)

        val milk = MarketMatcher.match("서울우유 흰우유", 1.0, "L", products)
        assertEquals("서울우유 흰우유(1L)", milk.best!!.product.name)
        assertTrue(milk.confident)

        val egg = MarketMatcher.match("깨끗한 계란", 15.0, "개", products)
        assertEquals("CJ 1등급 깨끗한 계란(15개)", egg.best!!.product.name)

        // 용량 모르면 후보로만
        val salt = MarketMatcher.match("해표 꽃소금", null, null, products)
        assertEquals("해표 꽃소금(1kg)", salt.best!!.product.name)
    }
}
