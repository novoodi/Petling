package com.example.petling.domain.receipt

import java.time.LocalDate

/** 영수증 한 줄 = 상품 하나. [unitPriceWon]은 낱개 가격(수량으로 나눈 값), [totalWon]은 줄 합계. */
data class ReceiptItem(
    val name: String,
    val unitPriceWon: Int,
    val quantity: Int,
    val totalWon: Int,
)

data class ReceiptDraft(
    val storeName: String?,
    /** 영수증 날짜(epochDay). 못 읽으면 null → 오늘로 기록. */
    val dateEpochDay: Long?,
    val items: List<ReceiptItem>,
    /** 영수증의 합계 금액(검증용). */
    val totalWon: Int?,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

/**
 * 영수증 OCR 텍스트 → 매장·날짜·상품 목록 (순수 Kotlin, 규칙만).
 *
 * 한국 마트·편의점 영수증은 "상품명 단가 수량 금액" 한 줄 또는 상품명 줄 + 숫자 줄 두 줄 구조다.
 * 합계 줄 이후(결제·거스름돈·포인트)는 상품이 아니므로 버린다. 할인 줄(-금액)은 직전 상품에 적용한다.
 * 숫자는 OCR 텍스트에서만 뽑고, 카드번호·승인번호 같은 결제 정보는 아예 읽지 않는다(저장도 안 함).
 */
object ReceiptParser {

    private val CHAINS = listOf(
        "트레이더스", "이마트24", "이마트 에브리데이", "이마트에브리데이", "이마트", "홈플러스", "롯데마트", "롯데슈퍼",
        "코스트코", "하나로마트", "노브랜드", "GS25", "GS더프레시", "GS THE FRESH", "CU", "세븐일레븐", "다이소",
        "메가마트", "킴스클럽", "올리브영", "농협", "탑마트", "식자재마트",
    )
    private val STORE_LINE = Regex("""(${CHAINS.joinToString("|") { Regex.escape(it) }})\s*([가-힣A-Za-z0-9]{1,12}점)?""", RegexOption.IGNORE_CASE)
    private val DATE_FULL = Regex("""(20\d{2})\s*[-./년]\s*(\d{1,2})\s*[-./월]\s*(\d{1,2})""")
    private val DATE_SHORT = Regex("""(?<!\d)(\d{2})[-./](\d{2})[-./](\d{2})(?!\d)""")

    private const val MONEY = """\d{1,3}(?:,\d{3})+|\d{3,7}"""
    /** 상품명 단가 수량 금액 */
    private val LINE_FULL = Regex("""^(.+?)\s+($MONEY)\s+(\d{1,3})\s+($MONEY)\s*$""")
    /** 상품명 금액 (또는 상품명 수량 금액) */
    private val LINE_QTY_TOTAL = Regex("""^(.+?)\s+(\d{1,2})\s+($MONEY)\s*$""")
    private val LINE_TOTAL = Regex("""^(.+?)\s+($MONEY)\s*$""")
    /** 숫자만 있는 줄: 단가 수량 금액 / 수량 금액 / 금액 */
    private val NUMBERS_ONLY = Regex("""^\s*($MONEY)(?:\s+(\d{1,3}))?(?:\s+($MONEY))?\s*$""")
    private val DISCOUNT = Regex("""(?:할인|에누리|쿠폰|행사)?\s*-\s*($MONEY)\s*$""")
    private val TOTAL_LINE = Regex("""^\s*(합\s*계|총\s*액|총\s*구매|총\s*합계|결제\s*금액|판매\s*합계|받을\s*금액|합계\s*금액)""")
    private val BARCODE = Regex("""^\s*\d{8,14}\s*$""")
    private val LEADING_MARK = Regex("""^[\s*#]*(?:\d{1,2}[.)]\s*)?[\s*#]*""")

    /** 이 단어가 들어간 줄은 상품이 아니다(결제·안내·헤더). */
    private val EXCLUDE = listOf(
        "합계", "총액", "부가세", "과세", "면세", "받은", "받을", "거스름", "카드", "승인", "결제", "현금", "신용", "잔액",
        "포인트", "적립", "사업자", "전화", "대표", "주소", "상품명", "단가", "수량", "금액", "소계", "봉투", "환경부담",
        "영수증", "매출", "일시", "고객", "회원", "번호", "할부", "가맹", "취소", "교환", "환불", "감사", "공급가",
        "물품", "서명", "매장", "점포", "POS", "계산", "담당",
    )

    fun parse(ocrText: String, today: LocalDate): ReceiptDraft {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val store = findStore(lines)
        val date = findDate(ocrText, today)

        val items = mutableListOf<ReceiptItem>()
        var total: Int? = null
        var pendingName: String? = null

        for (raw in lines) {
            val line = raw.replace('·', ' ').replace(Regex("\\s+"), " ")
            if (TOTAL_LINE.containsMatchIn(line)) {
                total = total ?: MONEY_ANY.findAll(line).lastOrNull()?.value?.let(::money)
                break // 합계 이후는 결제 정보
            }
            if (BARCODE.matches(line)) { pendingName = null; continue }

            // 할인 줄 → 직전 상품에 반영
            DISCOUNT.find(line)?.let { m ->
                if (items.isNotEmpty() && (line.contains("할인") || line.trimStart().startsWith("-") || m.range.first <= 2 || line.contains("-"))) {
                    val amount = money(m.groupValues[1])
                    val last = items.last()
                    val newTotal = last.totalWon - amount
                    if (amount > 0 && newTotal > 0) {
                        items[items.lastIndex] = last.copy(totalWon = newTotal, unitPriceWon = newTotal / last.quantity)
                    }
                    pendingName = null
                    return@let
                }
                null
            }?.let { continue }

            // 상품명 줄 다음의 숫자 줄
            NUMBERS_ONLY.matchEntire(line)?.let { m ->
                val name = pendingName
                pendingName = null
                if (name != null) {
                    val a = money(m.groupValues[1])
                    val b = m.groupValues[2].toIntOrNull()
                    val c = m.groupValues[3].takeIf { it.isNotEmpty() }?.let(::money)
                    val item = when {
                        c != null && b != null -> item(name, unit = a, qty = b, total = c)
                        b != null && c == null -> item(name, unit = a, qty = 1, total = a) // "금액 수량"은 드묾 → 금액으로 봄
                        else -> item(name, unit = a, qty = 1, total = a)
                    }
                    item?.let(items::add)
                }
                return@let
            }?.let { continue }

            val parsed = LINE_FULL.matchEntire(line)?.let { m ->
                item(m.groupValues[1], money(m.groupValues[2]), m.groupValues[3].toInt(), money(m.groupValues[4]))
            } ?: LINE_QTY_TOTAL.matchEntire(line)?.let { m ->
                val qty = m.groupValues[2].toInt()
                val total = money(m.groupValues[3])
                item(m.groupValues[1], if (qty > 0) total / qty else total, qty, total)
            } ?: LINE_TOTAL.matchEntire(line)?.let { m ->
                item(m.groupValues[1], money(m.groupValues[2]), 1, money(m.groupValues[2]))
            }

            if (parsed != null) {
                items += parsed
                pendingName = null
            } else {
                pendingName = cleanName(line).takeIf { isName(it) }
            }
        }
        return ReceiptDraft(store, date, items, total)
    }

    private val MONEY_ANY = Regex(MONEY)

    private fun money(s: String): Int = s.replace(",", "").toIntOrNull() ?: 0

    private fun item(rawName: String, unit: Int, qty: Int, total: Int): ReceiptItem? {
        val name = cleanName(rawName)
        if (!isName(name)) return null
        if (qty !in 1..99) return null
        if (total !in 100..2_000_000) return null
        val unitPrice = if (unit in 100..2_000_000) unit else total / qty
        return ReceiptItem(name, unitPrice, qty, total)
    }

    private fun cleanName(s: String): String =
        s.replace(LEADING_MARK, "").trim(' ', '*', '-', ':', '|').take(40)

    /** 상품명 판정: 한글 2자 이상 또는 영문 3자 이상, 제외어 없음, 숫자 비율 낮음. */
    private fun isName(s: String): Boolean {
        if (s.length < 2) return false
        val hangul = s.count { it in '가'..'힣' }
        val alpha = s.count { it in 'A'..'Z' || it in 'a'..'z' }
        if (hangul < 2 && alpha < 3) return false
        if (EXCLUDE.any { s.contains(it, ignoreCase = true) }) return false
        if (s.count { it.isDigit() } > s.length / 2) return false
        return true
    }

    private fun findStore(lines: List<String>): String? {
        for (line in lines.take(10)) {
            STORE_LINE.find(line)?.let { m ->
                val chain = m.groupValues[1].replace(Regex("\\s+"), " ")
                val branch = m.groupValues[2].takeIf { it.isNotBlank() }
                return if (branch != null) "$chain $branch" else chain
            }
        }
        // 체인명 없으면 상단의 "○○점"/"○○마트" 줄
        return lines.take(5).firstOrNull { l ->
            l.count { it in '가'..'힣' } >= 2 && (l.endsWith("점") || l.contains("마트") || l.contains("슈퍼")) &&
                !EXCLUDE.any { l.contains(it) }
        }?.take(30)
    }

    private fun findDate(text: String, today: LocalDate): Long? {
        DATE_FULL.find(text)?.let { m ->
            toDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), today)?.let { return it }
        }
        DATE_SHORT.find(text)?.let { m ->
            toDate(2000 + m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), today)?.let { return it }
        }
        return null
    }

    /** 미래 날짜·1년 넘은 날짜는 오독으로 보고 버린다. */
    private fun toDate(y: Int, m: Int, d: Int, today: LocalDate): Long? = runCatching {
        val date = LocalDate.of(y, m, d)
        if (date.isAfter(today) || date.isBefore(today.minusDays(400))) null else date.toEpochDay()
    }.getOrNull()
}
