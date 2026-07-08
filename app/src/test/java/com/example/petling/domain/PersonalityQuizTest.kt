package com.example.petling.domain

import com.example.petling.domain.model.Personality
import com.example.petling.domain.personality.PersonalityQuiz
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalityQuizTest {

    @Test
    fun has_four_questions_each_with_four_options() {
        assertEquals(4, PersonalityQuiz.questions.size)
        PersonalityQuiz.questions.forEach { assertEquals(4, it.options.size) }
    }

    @Test
    fun all_sincere_answers_yield_sincere() {
        // 각 질문의 0번 옵션이 성실형
        assertEquals(Personality.SINCERE, PersonalityQuiz.determine(listOf(0, 0, 0, 0)))
    }

    @Test
    fun all_free_spirit_answers_yield_free_spirit() {
        assertEquals(Personality.FREE_SPIRIT, PersonalityQuiz.determine(listOf(1, 1, 1, 1)))
    }

    @Test
    fun all_worrier_answers_yield_worrier() {
        assertEquals(Personality.WORRIER, PersonalityQuiz.determine(listOf(2, 2, 2, 2)))
    }

    @Test
    fun all_dreamer_answers_yield_dreamer() {
        assertEquals(Personality.DREAMER, PersonalityQuiz.determine(listOf(3, 3, 3, 3)))
    }

    @Test
    fun tie_break_prefers_sincere_then_worrier() {
        // 성실(0) 2개, 걱정(2) 2개 동점 -> 우선순위상 SINCERE
        assertEquals(Personality.SINCERE, PersonalityQuiz.determine(listOf(0, 0, 2, 2)))
        // 자유분방(1) 2개, 몽상(3) 2개 동점 -> 우선순위상 FREE_SPIRIT
        assertEquals(Personality.FREE_SPIRIT, PersonalityQuiz.determine(listOf(1, 1, 3, 3)))
    }

    @Test
    fun handles_incomplete_answers_gracefully() {
        // 답이 하나뿐이어도 예외 없이 판정
        assertEquals(Personality.WORRIER, PersonalityQuiz.determine(listOf(2)))
    }
}
