package com.example.petling.domain

import com.example.petling.domain.model.CharacterAnimation
import com.example.petling.domain.model.CharacterSpec
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Species
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 성장 다이어리 스냅샷(CharacterSpec JSON) 하위호환 검증.
 * animation에 WALK/SLEEP을 추가해도 기존 저장본(animation 키 없음/IDLE/BOUNCE)이 안전해야 한다.
 */
class CharacterSpecSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun legacy_json_without_animation_decodes_to_idle() {
        // animation 키가 없던 옛 스냅샷
        val legacy = """{"stage":"GROWTH1","mood":"CALM","expression":"NEUTRAL","species":"ACORN","colorHue":30.0,"eyeStyle":0}"""
        val spec = json.decodeFromString<CharacterSpec>(legacy)
        assertEquals(CharacterAnimation.IDLE, spec.animation)
        assertEquals(GrowthStage.GROWTH1, spec.stage)
        assertEquals(Species.ACORN, spec.species)
    }

    @Test
    fun existing_bounce_json_still_decodes() {
        val body = """{"stage":"MATURE","mood":"HAPPY","expression":"HAPPY","species":"FOX","colorHue":24.0,"eyeStyle":1,"animation":"BOUNCE"}"""
        val spec = json.decodeFromString<CharacterSpec>(body)
        assertEquals(CharacterAnimation.BOUNCE, spec.animation)
        assertEquals(Species.FOX, spec.species)
    }

    @Test
    fun new_species_round_trip() {
        listOf(Species.DOG, Species.HAMSTER, Species.PENGUIN, Species.PANDA).forEach { sp ->
            val spec = CharacterSpec(stage = GrowthStage.JUVENILE, species = sp)
            val decoded = json.decodeFromString<CharacterSpec>(json.encodeToString(spec))
            assertEquals(sp, decoded.species)
        }
    }

    @Test
    fun walk_spec_round_trips() {
        val spec = CharacterSpec(
            stage = GrowthStage.GROWTH2,
            species = Species.RABBIT,
            animation = CharacterAnimation.WALK,
        )
        val encoded = json.encodeToString(spec)
        val decoded = json.decodeFromString<CharacterSpec>(encoded)
        assertEquals(spec, decoded)
        assertEquals(CharacterAnimation.WALK, decoded.animation)
    }
}
