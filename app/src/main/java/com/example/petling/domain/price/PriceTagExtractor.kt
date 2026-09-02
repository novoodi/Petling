package com.example.petling.domain.price

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 가격표 사진(OCR 텍스트 + 이미지)에서 상품·가격 정보를 추출한다. */
interface PriceTagExtractor {
    suspend fun extract(ocrText: String, imagePath: String? = null): PriceTagInfo
}

/**
 * 규칙 기반 가격표 추출기(순수 Kotlin).
 *
 * 마트 가격표는 구조가 일정하다(상품명+용량 / 단위가 "100g당 936원" / 큰 최종가 /
 * 할인 표기 / 행사기간 / 바코드). 숫자는 전부 OCR 텍스트에서 규칙으로만 뽑는다.
 *
 * 최종가 판정 우선순위:
 *  1) 할인 삼각형: 후보 중 a - 할인액 = c 가 성립하면 최종가 c, 정가 a
 *  2) 단위가 교차검증: 단위가 × (용량/기준량) ≈ 후보면 그 후보(정가 표기 가격)
 *  3) 후보가 하나뿐이면 그 값
 *  4) 그 외(이웃 가격표 혼입 등)는 미확정 — 후보 목록만 채워 Nano/사용자가 고른다
 */
class RuleBasedPriceTagExtractor : PriceTagExtractor {

    override suspend fun extract(ocrText: String, imagePath: String?): PriceTagInfo {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 단위가: "10g당 176원" / "100g당 1,645원" — 첫 매치(중앙 가격표가 보통 먼저 잡힘)
        val unitMatch = lines.firstNotNullOfOrNull { UNIT_PRICE.find(it) }
        val unitBaseAmount = unitMatch?.groupValues?.get(1)?.toDoubleOrNull()
        val unitBaseUnit = unitMatch?.groupValues?.get(2)?.lowercase()
        val unitPriceWon = unitMatch?.groupValues?.get(3)?.replace(",", "")?.toIntOrNull()

        // 가격 후보: 콤마 구분 숫자만(바코드 13자리·날짜 8자리 자동 배제).
        // 단위가 줄의 "1,645원" 같은 값은 후보에서 제외한다.
        val candidates = mutableListOf<Int>()
        val discounts = mutableListOf<Int>()
        for (line in lines) {
            if (UNIT_PRICE.containsMatchIn(line)) continue
            for (m in COMMA_NUMBER.findAll(line)) {
                val value = m.value.replace(",", "").toIntOrNull() ?: continue
                if (value !in 100..9_999_999) continue
                val isDiscount = m.range.first > 0 &&
                    line.substring(0, m.range.first).trimEnd().endsWith("-")
                if (isDiscount) discounts += value else candidates += value
            }
        }

        // 상품명 + 용량
        val nameLine = lines.firstOrNull { isNameLine(it) }
        val volumeMatch = nameLine?.let { VOLUME.find(it) }
        val volumeAmount = volumeMatch?.groupValues?.get(1)?.toDoubleOrNull()
        val volumeUnit = volumeMatch?.groupValues?.get(2)?.lowercase()
        val name = nameLine
            ?.let { if (volumeMatch != null) it.removeRange(volumeMatch.range) else it }
            ?.replace(PARENTHETICAL, "")
            ?.trim(' ', '-', ':', '·')
            .orEmpty()

        val (priceWon, originalPriceWon) = resolvePrice(
            candidates, discounts, unitPriceWon, unitBaseAmount, unitBaseUnit, volumeAmount, volumeUnit,
        )

        return PriceTagInfo(
            name = name,
            priceWon = priceWon,
            originalPriceWon = originalPriceWon,
            volumeAmount = volumeAmount,
            volumeUnit = volumeUnit,
            unitBaseAmount = unitBaseAmount,
            unitBaseUnit = unitBaseUnit,
            unitPriceWon = unitPriceWon,
            saleEndEpochDay = extractSaleEnd(ocrText),
            barcode = BARCODE.find(ocrText)?.groupValues?.get(1),
            priceCandidatesWon = candidates.distinct().sortedDescending(),
        )
    }

    private fun resolvePrice(
        candidates: List<Int>,
        discounts: List<Int>,
        unitPriceWon: Int?,
        unitBaseAmount: Double?,
        unitBaseUnit: String?,
        volumeAmount: Double?,
        volumeUnit: String?,
    ): Pair<Int?, Int?> {
        val distinct = candidates.distinct()

        // 1) 할인 삼각형: 정가 - 할인액 = 최종가
        for (a in distinct.sortedDescending()) {
            for (d in discounts) {
                val c = a - d
                if (c > 0 && c in distinct) return c to a
            }
        }
        // 1.5) OCR이 '-' 기호를 놓친 경우: 후보끼리 a - b = c 성립 여부로 복원
        for (a in distinct.sortedDescending()) {
            for (b in distinct) {
                val c = a - b
                if (b in 1 until a && c != b && c in distinct) return c to a
            }
        }

        // 2) 단위가 × 용량 교차검증(단위 g/kg, ml/l 환산)
        val expected = expectedFromUnit(unitPriceWon, unitBaseAmount, unitBaseUnit, volumeAmount, volumeUnit)
        if (expected != null) {
            distinct.firstOrNull { withinTolerance(it, expected) }?.let { return it to null }
        }

        // 3) 후보가 하나면 확정
        if (distinct.size == 1) return distinct.single() to null

        // 4) 미확정 — 후보만 노출
        return null to null
    }

    /** 단위가 기반 기대 가격(원). 단위 비호환이면 null. */
    private fun expectedFromUnit(
        unitPriceWon: Int?,
        unitBaseAmount: Double?,
        unitBaseUnit: String?,
        volumeAmount: Double?,
        volumeUnit: String?,
    ): Double? {
        if (unitPriceWon == null || unitBaseAmount == null || unitBaseAmount <= 0.0) return null
        if (volumeAmount == null) return null
        val baseNorm = toBaseUnits(unitBaseAmount, unitBaseUnit) ?: return null
        val volNorm = toBaseUnits(volumeAmount, volumeUnit) ?: return null
        return unitPriceWon * (volNorm / baseNorm)
    }

    /** g/ml 기준으로 환산. kg/l → ×1000. 무게·부피 혼용은 마트 표기상 사실상 없어 구분하지 않는다. */
    private fun toBaseUnits(amount: Double, unit: String?): Double? = when (unit) {
        "g", "ml" -> amount
        "kg", "l" -> amount * 1000
        else -> null
    }

    private fun withinTolerance(candidate: Int, expected: Double): Boolean {
        val diff = kotlin.math.abs(candidate - expected)
        return diff <= maxOf(expected * 0.01, 20.0)
    }

    /** 상품명 줄 판정: 한글 2자 이상, 설명("-시작)·안내 문구·단위가 줄 제외. */
    private fun isNameLine(line: String): Boolean {
        if (line.startsWith("-")) return false
        if (UNIT_PRICE.containsMatchIn(line)) return false
        if (EXCLUDE_KEYWORDS.any { line.contains(it) }) return false
        return line.count { it in '가'..'힣' } >= 2
    }

    private fun extractSaleEnd(text: String): Long? {
        val m = SALE_PERIOD.find(text) ?: return null
        return runCatching {
            LocalDate.parse(m.groupValues[2], DateTimeFormatter.BASIC_ISO_DATE).toEpochDay()
        }.getOrNull()
    }

    private companion object {
        val UNIT_PRICE = Regex("""(\d+(?:\.\d+)?)\s*(g|kg|ml|l)당\s*([\d,]+)\s*원""", RegexOption.IGNORE_CASE)
        val COMMA_NUMBER = Regex("""\d{1,3}(?:,\d{3})+""")
        val VOLUME = Regex("""(\d+(?:\.\d+)?)\s*(g|kg|ml|l|매|입|개|팩|병|캔)(?![a-z0-9가-힣])""", RegexOption.IGNORE_CASE)
        val PARENTHETICAL = Regex("""[(（][^)）]*[)）]""")
        val SALE_PERIOD = Regex("""행사\s*기간\s*:?\s*(\d{8})\s*~\s*(\d{8})""")
        val BARCODE = Regex("""(?<!\d)(\d{13})(?!\d)""")
        val EXCLUDE_KEYWORDS = listOf("행사기간", "포인트", "할인", "원산지", "적립", "카드", "결제")
    }
}
