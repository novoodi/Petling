package com.example.petling.domain

import com.example.petling.domain.model.Species
import com.example.petling.ui.character.hopPose
import com.example.petling.ui.character.hopStyleFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * 정면 홉 보행 포즈 검증. 옆모습(SIDE) 제거 후 WALK를 대체한 순수 함수.
 */
class HopPoseTest {

    @Test
    fun penguin_has_no_vertical_hop() {
        val style = hopStyleFor(Species.PENGUIN)
        assertEquals(0f, style.hopHeight, 0f)
        // 펭귄은 홉 대신 좌우 롤 뒤뚱 — 세로 이동이 거의 없어야 한다.
        for (i in 0..20) {
            val phi = i / 20f
            val pose = hopPose(style, phi)
            assertTrue("penguin dy at phi=$phi should be tiny", abs(pose.dyNorm) < 0.01f)
        }
        // 롤은 존재해야 한다(위상에 따라 좌우로 기울어짐).
        val mid = hopPose(style, 0.25f)
        assertTrue("penguin should roll", abs(mid.tiltDeg - style.leanDeg) > 1f)
    }

    @Test
    fun hopping_species_peak_lift_near_mid_cycle() {
        val style = hopStyleFor(Species.RABBIT)
        val apex = hopPose(style, 0.5f)   // sin(π/2)=1 → 최대 상승
        val ground = hopPose(style, 0f)   // sin(0)=0 → 지면
        assertTrue("apex should lift up (negative dy)", apex.dyNorm < -0.01f)
        assertEquals("ground has no lift", 0f, ground.dyNorm, 1e-4f)
        assertTrue("apex should be higher than ground", apex.dyNorm < ground.dyNorm)
        // 정점에서 늘어남(stretch): scaleY > 1
        assertTrue("apex stretches vertically", apex.scaleY > 1f)
    }

    @Test
    fun phase_wraps_continuously() {
        // phi 0 과 1 은 같은 지점 — 위상 팝(점프) 없이 연속이어야 한다.
        for (sp in Species.entries) {
            val style = hopStyleFor(sp)
            val at0 = hopPose(style, 0f)
            val at1 = hopPose(style, 1f)
            assertEquals("dy continuous at wrap for $sp", at0.dyNorm, at1.dyNorm, 1e-3f)
            assertEquals("tilt continuous at wrap for $sp", at0.tiltDeg, at1.tiltDeg, 1e-3f)
        }
    }

    @Test
    fun dust_only_fires_on_landing_window() {
        val style = hopStyleFor(Species.DOG)
        assertEquals("no dust mid-air", 0f, hopPose(style, 0.4f).dust, 1e-4f)
        assertTrue("dust near landing", hopPose(style, 0.95f).dust > 0f)
    }

    @Test
    fun acorn_waddles_gently() {
        // 옛 도토리 정면 뒤뚱을 홉 스타일로 흡수 — 작은 홉 + 롤.
        val style = hopStyleFor(Species.ACORN)
        assertTrue("acorn has a small hop", style.hopHeight in 0.001f..0.03f)
        assertTrue("acorn rolls", style.rollDeg > 0f)
    }
}
