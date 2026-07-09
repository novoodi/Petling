package com.example.petling.domain

import androidx.compose.ui.geometry.Rect
import com.example.petling.ui.overlay.PerchRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * perch 레지스트리 검증: 등록/조회/해제 + 서브픽셀 변화 dedup.
 */
class PerchRegistryTest {

    @Test
    fun report_and_query() {
        val reg = PerchRegistry()
        reg.report("a", Rect(0f, 0f, 100f, 50f), weight = 0.5f)
        assertEquals(Rect(0f, 0f, 100f, 50f), reg.rectOf("a"))
        assertEquals(1, reg.candidates().size)
        assertEquals(0.5f, reg.candidates().first().weight, 0f)
    }

    @Test
    fun remove_clears() {
        val reg = PerchRegistry()
        reg.report("a", Rect(0f, 0f, 10f, 10f), 0f)
        reg.remove("a")
        assertNull(reg.rectOf("a"))
        assertTrue(reg.candidates().isEmpty())
    }

    @Test
    fun subpixel_change_is_deduped() {
        val reg = PerchRegistry()
        reg.report("a", Rect(0f, 0f, 100f, 50f), 0f)
        // EPS(0.5px) 미만 변화 → 저장값 유지(갱신 스킵)
        reg.report("a", Rect(0.2f, 0.1f, 100.2f, 50.1f), 0f)
        assertEquals(Rect(0f, 0f, 100f, 50f), reg.rectOf("a"))
    }

    @Test
    fun meaningful_change_updates() {
        val reg = PerchRegistry()
        reg.report("a", Rect(0f, 0f, 100f, 50f), 0f)
        // 스크롤로 크게 이동 → 갱신
        reg.report("a", Rect(0f, 40f, 100f, 90f), 0f)
        assertEquals(Rect(0f, 40f, 100f, 90f), reg.rectOf("a"))
    }

    @Test
    fun weight_change_updates() {
        val reg = PerchRegistry()
        reg.report("a", Rect(0f, 0f, 100f, 50f), 0f)
        reg.report("a", Rect(0f, 0f, 100f, 50f), 0.9f)
        assertEquals(0.9f, reg.candidates().first().weight, 0f)
    }
}
