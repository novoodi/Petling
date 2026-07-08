package com.example.petling.domain

import com.example.petling.domain.model.Personality
import com.example.petling.domain.personality.PhraseArgs
import com.example.petling.domain.personality.PhraseBank
import com.example.petling.domain.personality.PhraseContext
import com.example.petling.domain.personality.PhraseSelector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhraseSelectorTest {

    @Test
    fun every_personality_context_combination_has_phrases() {
        Personality.entries.forEach { personality ->
            PhraseContext.entries.forEach { context ->
                val phrases = PhraseBank.bank[personality]?.get(context)
                assertTrue(
                    "누락: $personality / $context",
                    phrases != null && phrases.isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun placeholders_are_all_substituted() {
        val selector = PhraseSelector(Random(42))
        val result = selector.pick(
            Personality.SINCERE,
            PhraseContext.REMINDER,
            PhraseArgs(name = "도토리", title = "학원 상담", time = "오후 3시", location = "OO학원"),
        )
        assertFalse(result.contains("{name}"))
        assertFalse(result.contains("{title}"))
        assertFalse(result.contains("{time}"))
        assertFalse(result.contains("{location}"))
    }

    @Test
    fun fixed_random_is_deterministic() {
        val a = PhraseSelector(Random(7)).pick(Personality.DREAMER, PhraseContext.COMPLETED)
        val b = PhraseSelector(Random(7)).pick(Personality.DREAMER, PhraseContext.COMPLETED)
        assertTrue(a == b)
    }

    @Test
    fun no_phrase_leaks_guilt_words() {
        // 죄책감 유발 금지 원칙 회귀 방지 — 대표 금지어가 문구에 없어야 함
        val forbidden = listOf("굶", "죽", "미안해서", "네 탓")
        PhraseBank.bank.values.forEach { byContext ->
            byContext.values.forEach { phrases ->
                phrases.forEach { phrase ->
                    forbidden.forEach { word ->
                        assertFalse("금지어 '$word' 발견: $phrase", phrase.contains(word))
                    }
                }
            }
        }
    }
}
