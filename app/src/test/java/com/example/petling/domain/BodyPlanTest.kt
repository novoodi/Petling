package com.example.petling.domain

import com.example.petling.domain.model.Species
import com.example.petling.ui.character.BodyPlan
import com.example.petling.ui.character.bodyPlanFor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 체형 아키타입 매핑 고정. 단색 실루엣만으로 종을 구분하려면 종→체형 배정이 안정적이어야 한다.
 */
class BodyPlanTest {

    @Test
    fun species_body_plan_mapping_is_stable() {
        assertEquals(BodyPlan.UPRIGHT_TEARDROP, bodyPlanFor(Species.CHICK))
        assertEquals(BodyPlan.UPRIGHT_TEARDROP, bodyPlanFor(Species.PENGUIN))
        assertEquals(BodyPlan.CROUCHED_BALL, bodyPlanFor(Species.HAMSTER))
        assertEquals(BodyPlan.SEATED_QUADRUPED, bodyPlanFor(Species.FOX))
        assertEquals(BodyPlan.SEATED_QUADRUPED, bodyPlanFor(Species.CAT))
        assertEquals(BodyPlan.SEATED_QUADRUPED, bodyPlanFor(Species.DOG))
        assertEquals(BodyPlan.HEAVY_DOME, bodyPlanFor(Species.PANDA))
        assertEquals(BodyPlan.HAUNCHED, bodyPlanFor(Species.RABBIT))
    }

    @Test
    fun every_species_has_a_plan() {
        // when 망라 — 새 종 추가 시 배정 누락을 컴파일/런타임에서 잡는다.
        for (sp in Species.entries) {
            bodyPlanFor(sp)
        }
    }
}
