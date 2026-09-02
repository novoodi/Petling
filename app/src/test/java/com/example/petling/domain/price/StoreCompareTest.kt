package com.example.petling.domain.price

import com.example.petling.data.local.entity.PriceEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreCompareTest {

    private fun entry(id: Long, price: Int, store: String?, createdAt: Long) = PriceEntryEntity(
        id = id,
        productId = 1L,
        priceWon = price,
        storeName = store,
        dateEpochDay = createdAt,
        createdAt = createdAt,
    )

    private val history = listOf(
        entry(1, 6_980, "이마트", createdAt = 10),
        entry(2, 7_480, "홈플러스", createdAt = 20),
        entry(3, 6_480, "이마트", createdAt = 30),
    )

    @Test
    fun `같은 매장 최신 기록을 우선한다`() {
        val prev = previousFor(history, "홈플러스")!!
        assertEquals(2L, prev.entry.id)
        assertTrue(prev.sameStore)
    }

    @Test
    fun `같은 매장 기록이 없으면 전체 최신으로 폴백하고 표시한다`() {
        val prev = previousFor(history, "코스트코")!!
        assertEquals(3L, prev.entry.id)
        assertFalse(prev.sameStore)
    }

    @Test
    fun `매장을 고르지 않으면 전체 최신 기록이고 폴백 표시가 없다`() {
        val prev = previousFor(history, null)!!
        assertEquals(3L, prev.entry.id)
        assertTrue(prev.sameStore)
    }

    @Test
    fun `매장명은 공백과 대소문자를 무시하고 비교한다`() {
        val prev = previousFor(history, " 홈 플러스 ")!!
        assertEquals(2L, prev.entry.id)
        assertTrue(prev.sameStore)
    }

    @Test
    fun `매장 미기록 과거 데이터는 매장을 골랐을 때 다른 매장으로 본다`() {
        val old = listOf(entry(9, 5_000, null, createdAt = 5))
        val prev = previousFor(old, "이마트")!!
        assertEquals(9L, prev.entry.id)
        assertFalse(prev.sameStore)
    }

    @Test
    fun `기록이 없으면 null`() {
        assertNull(previousFor(emptyList(), "이마트"))
    }
}
