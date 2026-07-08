package com.example.petling.domain.personality

import com.example.petling.domain.model.Personality
import kotlin.random.Random

/** 문구 치환에 쓰이는 인자. null 값은 빈 문자열로 대체된다. */
data class PhraseArgs(
    val name: String? = null,
    val title: String? = null,
    val time: String? = null,
    val location: String? = null,
)

/**
 * 성격/컨텍스트에 맞는 문구를 골라 플레이스홀더를 치환한다.
 * Random을 주입받아 테스트에서 결정적으로 동작하게 한다.
 */
class PhraseSelector(private val random: Random = Random.Default) {

    fun pick(personality: Personality, context: PhraseContext, args: PhraseArgs = PhraseArgs()): String {
        val candidates = PhraseBank.bank[personality]?.get(context)
        if (candidates.isNullOrEmpty()) return ""
        val template = candidates[random.nextInt(candidates.size)]
        return template
            .replace("{name}", args.name ?: "")
            .replace("{title}", args.title ?: "")
            .replace("{time}", args.time ?: "")
            .replace("{location}", args.location ?: "")
    }
}
