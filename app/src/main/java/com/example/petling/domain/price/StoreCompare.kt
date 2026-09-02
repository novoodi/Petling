package com.example.petling.domain.price

import com.example.petling.data.local.entity.PriceEntryEntity

/** 재방문 비교 대상 한 건. [sameStore]가 false면 같은 매장 기록이 없어 다른 매장 기록으로 폴백한 것. */
data class PreviousRecord(
    val entry: PriceEntryEntity,
    val sameStore: Boolean,
)

/** 매장명 비교용 정규화(공백 제거·소문자). 빈 문자열은 null과 동일하게 본다. */
fun normalizeStoreName(name: String?): String? =
    name?.filter { !it.isWhitespace() }?.lowercase()?.takeIf { it.isNotEmpty() }

/**
 * 비교 대상 선택 규칙: 같은 매장의 최신 기록 우선, 없으면 전체 최신 기록으로 폴백.
 * @param history 비교 후보(최신순 정렬). 오늘 기록을 제외한 목록을 넘긴다.
 * @param storeName 지금 촬영 중인 매장(없으면 null → 전체 최신 기록, sameStore=true 취급).
 */
fun previousFor(history: List<PriceEntryEntity>, storeName: String?): PreviousRecord? {
    val wanted = normalizeStoreName(storeName)
    val sorted = history.sortedByDescending { it.createdAt }
    if (wanted != null) {
        sorted.firstOrNull { normalizeStoreName(it.storeName) == wanted }
            ?.let { return PreviousRecord(it, sameStore = true) }
    }
    val fallback = sorted.firstOrNull() ?: return null
    return PreviousRecord(
        entry = fallback,
        sameStore = wanted == null || normalizeStoreName(fallback.storeName) == wanted,
    )
}
