package com.example.petling.domain.market

import com.example.petling.data.local.entity.MarketProductEntity

/** 참가격 상품 후보 하나와 유사도(0~1). */
data class MarketCandidate(val product: MarketProductEntity, val score: Double)

/**
 * 매칭 결과. [confident]면 자동 채택, 아니면 [candidates]를 사용자에게 보여 고르게 한다.
 * 후보가 하나도 없으면 참가격에 없는 상품(PB·정육·채소 등)으로 보고 카드 자체를 숨긴다.
 */
data class MarketMatch(
    val candidates: List<MarketCandidate>,
    val confident: Boolean,
) {
    val best: MarketCandidate? get() = candidates.firstOrNull()

    companion object {
        val NONE = MarketMatch(emptyList(), confident = false)
    }
}

/**
 * OCR 상품명 ↔ 참가격 상품명 매칭(순수 Kotlin).
 *
 * 규칙: 두 이름을 정규화(괄호 규격 제거, 공백·기호 제거, 소문자)한 뒤 **글자 2-gram 자카드 유사도**로 비교한다.
 * 한국어 상품명은 띄어쓰기가 제각각이라 토큰보다 2-gram이 안정적이다. 용량이 둘 다 있으면 일치 시 가산, 불일치 시 감산.
 * 임계값은 골든 케이스(신라면·서울우유·계란 + 참가격에 없는 존쿡·파채)로 맞췄다.
 */
object MarketMatcher {

    private const val CONFIDENT_SCORE = 0.6
    private const val CONFIDENT_GAP = 0.15
    private const val CANDIDATE_SCORE = 0.3
    private const val MAX_CANDIDATES = 3
    private const val VOLUME_BONUS = 0.15
    private const val VOLUME_PENALTY = 0.2

    /** 참가격 이름에서 괄호 규격을 뺀 본체. "서울우유 흰우유(1L)" → "서울우유 흰우유". */
    fun coreName(marketName: String): String =
        marketName.replace(Regex("\\([^)]*\\)"), " ").trim()

    /** OCR 이름에서 용량·수량 토큰 제거. "신라면 5입 120g" → "신라면". */
    fun stripVolume(name: String): String =
        name.replace(Regex("(?i)\\d+(?:[.,]\\d+)?\\s*(?:kg|g|ml|l|리터|개입|개|입|구|매|p|팩|봉|캔|병|장|롤|매입)\\b"), " ")
            .replace(Regex("(?i)\\d+(?:[.,]\\d+)?\\s*(?:kg|g|ml|l)(?=\\S|$)"), " ")
            .trim()

    fun normalize(s: String): String = s.filter { it.isLetterOrDigit() }.lowercase()

    fun bigrams(normalized: String): Set<String> =
        if (normalized.length < 2) setOf(normalized) else (0 until normalized.length - 1).map { normalized.substring(it, it + 2) }.toSet()

    /** 2-gram 자카드. 한쪽이 다른 쪽을 통째로 포함하면(짧은 이름) 포함 비율도 고려한다. */
    fun similarity(a: String, b: String): Double {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) return 0.0
        if (na == nb) return 1.0
        val ga = bigrams(na)
        val gb = bigrams(nb)
        val inter = ga.intersect(gb).size.toDouble()
        val jaccard = inter / (ga.size + gb.size - inter)
        // 짧은 OCR 이름이 긴 참가격 이름에 통째로 들어 있는 경우("신라면" ⊂ "농심 신라면 큰사발면")
        val containment = if (na.length >= 3 && nb.contains(na)) inter / ga.size * 0.8 else 0.0
        return maxOf(jaccard, containment)
    }

    /** g/ml/개 기준량으로 환산. kg→g, l→ml. 모르는 단위는 null. */
    fun baseAmount(amount: Double?, unit: String?): Pair<Double, String>? {
        if (amount == null || unit == null) return null
        return when (unit.trim().lowercase()) {
            "g" -> amount to "g"
            "kg" -> amount * 1000 to "g"
            "ml" -> amount to "ml"
            "l", "리터" -> amount * 1000 to "ml"
            "ea", "개", "입", "개입", "구", "매", "p", "팩" -> amount to "ea"
            else -> null
        }
    }

    /** 용량 비교: 둘 다 알 때만 true/false, 아니면 null. 5% 오차 허용. */
    fun volumeMatches(tagAmount: Double?, tagUnit: String?, product: MarketProductEntity): Boolean? {
        val a = baseAmount(tagAmount, tagUnit) ?: return null
        val b = baseAmount(product.totalAmount, product.totalUnit) ?: return null
        if (a.second != b.second) return false
        return kotlin.math.abs(a.first - b.first) <= b.first * 0.05
    }

    fun match(
        tagName: String,
        tagAmount: Double?,
        tagUnit: String?,
        products: List<MarketProductEntity>,
    ): MarketMatch {
        val query = stripVolume(tagName)
        if (normalize(query).length < 2) return MarketMatch.NONE

        val scored = products.mapNotNull { p ->
            var score = similarity(query, p.normalizedName)
            if (score < CANDIDATE_SCORE) return@mapNotNull null
            val volume = volumeMatches(tagAmount, tagUnit, p)
            when (volume) {
                true -> score += VOLUME_BONUS
                false -> score -= VOLUME_PENALTY
                null -> Unit
            }
            if (score < CANDIDATE_SCORE) null else Triple(p, score.coerceAtMost(1.0), volume)
        }.sortedByDescending { it.second }.take(MAX_CANDIDATES)

        if (scored.isEmpty()) return MarketMatch.NONE
        val best = scored[0].second
        val second = scored.getOrNull(1)?.second ?: 0.0
        // 용량이 확실히 다르면(계란 30구 vs 15개) 이름이 같아도 자동 확정하지 않고 사용자에게 묻는다
        val confident = best >= CONFIDENT_SCORE && best - second >= CONFIDENT_GAP && scored[0].third != false
        return MarketMatch(scored.map { MarketCandidate(it.first, it.second) }, confident)
    }
}
