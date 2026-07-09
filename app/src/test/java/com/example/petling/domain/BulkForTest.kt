package com.example.petling.domain

import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Species
import com.example.petling.ui.character.bulkFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 종별 체급(바닥선 기준 크기) 검증.
 * 성숙기에 요구 스케일과 정확히 일치하고, 유생기는 격차가 수렴해야 한다.
 */
class BulkForTest {

    @Test
    fun mature_bulk_matches_spec() {
        // 성숙기(BULK_RAMP=1.0)에서는 종 스케일이 그대로 나온다.
        assertEquals(0.60f, bulkFor(Species.CHICK, GrowthStage.MATURE), 1e-4f)
        assertEquals(0.65f, bulkFor(Species.HAMSTER, GrowthStage.MATURE), 1e-4f)
        assertEquals(0.85f, bulkFor(Species.RABBIT, GrowthStage.MATURE), 1e-4f)
        assertEquals(0.90f, bulkFor(Species.CAT, GrowthStage.MATURE), 1e-4f)
        assertEquals(0.90f, bulkFor(Species.PENGUIN, GrowthStage.MATURE), 1e-4f)
        assertEquals(0.95f, bulkFor(Species.DOG, GrowthStage.MATURE), 1e-4f)
        assertEquals(1.00f, bulkFor(Species.FOX, GrowthStage.MATURE), 1e-4f)
        assertEquals(1.25f, bulkFor(Species.PANDA, GrowthStage.MATURE), 1e-4f)
        assertEquals(1.00f, bulkFor(Species.ACORN, GrowthStage.MATURE), 1e-4f)
    }

    @Test
    fun bulk_range_is_about_two_fold() {
        val min = bulkFor(Species.CHICK, GrowthStage.MATURE)
        val max = bulkFor(Species.PANDA, GrowthStage.MATURE)
        assertTrue("max/min should be roughly 2x", max / min > 1.9f)
    }

    @Test
    fun juvenile_converges_toward_one() {
        // 유생기는 성숙기보다 1(기준)에 가까워야 한다 — 소형 종 과소축소 방지.
        val chickJuv = bulkFor(Species.CHICK, GrowthStage.JUVENILE)
        val chickMat = bulkFor(Species.CHICK, GrowthStage.MATURE)
        assertTrue("juvenile chick closer to 1 than mature", abs(1f - chickJuv) < abs(1f - chickMat))
        // 정확값: 1 + (0.60-1)*0.55 = 0.78
        assertEquals(0.78f, chickJuv, 1e-4f)

        val pandaJuv = bulkFor(Species.PANDA, GrowthStage.JUVENILE)
        val pandaMat = bulkFor(Species.PANDA, GrowthStage.MATURE)
        assertTrue("juvenile panda closer to 1 than mature", abs(1f - pandaJuv) < abs(1f - pandaMat))
    }

    @Test
    fun egg_is_neutral() {
        // 알은 종 미공개 — 항상 1.
        for (sp in Species.entries) {
            assertEquals("egg bulk 1 for $sp", 1f, bulkFor(sp, GrowthStage.EGG), 0f)
        }
    }
}
