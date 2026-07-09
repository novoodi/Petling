package com.example.petling.domain.parsing

import com.example.petling.domain.model.ScheduleSource
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 규칙 기반 한국어 일정 파서 (온디바이스 폴백 엔진).
 * Gemini Nano 미지원 기기의 UX 하한선을 결정하는 핵심 컴포넌트이므로
 * 순수 함수로 작성해 JVM 단위 테스트로 촘촘히 검증한다.
 *
 * 시간 기준(today)은 파라미터로 주입한다.
 */
object KoreanScheduleParser {

    /** weak = 날짜 신호가 약함(요일 단독, 비보강 M/d) — 시간이 동반될 때만 신뢰. */
    private data class Match(val value: Int?, val range: IntRange, val text: String? = null, val weak: Boolean = false)

    private val WEEKDAYS = mapOf(
        "월" to DayOfWeek.MONDAY, "화" to DayOfWeek.TUESDAY, "수" to DayOfWeek.WEDNESDAY,
        "목" to DayOfWeek.THURSDAY, "금" to DayOfWeek.FRIDAY, "토" to DayOfWeek.SATURDAY,
        "일" to DayOfWeek.SUNDAY,
    )

    private val LOCATION_KEYWORDS = listOf(
        "학원", "병원", "카페", "학교", "도서관", "서점", "미용실", "치과", "의원",
        "스터디카페", "피시방", "노래방", "헬스장", "수영장", "강남", "홍대", "신촌",
        "회사", "교실", "강의실", "체육관", "운동장", "식당",
    )

    private val IMPORTANT_KEYWORDS = listOf(
        "시험", "과제", "제출", "마감", "발표", "면접", "중요", "디데이", "수능", "모의고사",
    )

    private val STUDY_KEYWORDS = listOf("시험", "수업", "강의", "학원", "공부", "과제", "숙제", "스터디", "등교", "수능", "모의고사")
    private val APPOINTMENT_KEYWORDS = listOf("약속", "만남", "모임", "미팅", "생일", "면접", "상담")
    private val HOBBY_KEYWORDS = listOf("운동", "헬스", "취미", "게임", "노래", "영화", "연습", "농구", "축구")
    private val REST_KEYWORDS = listOf("휴식", "낮잠", "쉬기", "힐링")

    /** 파싱된 한 건. category/isImportant는 draft를 넘어선 부가 힌트. */
    data class ParsedLine(
        val draft: ParsedScheduleDraft,
        val category: com.example.petling.domain.model.ScheduleCategory?,
        val isImportant: Boolean,
    )

    /** 의도 정책이 켜는 파싱 옵션. 기본값이면 기존 동작과 동일. */
    data class ParseOptions(
        /** 시간표: 앞선 줄의 날짜를 시간만 있는 이후 줄에 전파. */
        val dateCarryAcrossLines: Boolean = false,
    )

    fun parse(
        rawText: String,
        today: LocalDate,
        source: ScheduleSource = ScheduleSource.MANUAL,
        options: ParseOptions = ParseOptions(),
    ): List<ParsedLine> {
        val lines = segment(rawText)
        val results = if (options.dateCarryAcrossLines) {
            parseWithDateCarry(lines, today, source)
        } else {
            lines.mapNotNull { parseLine(it, today, source) }
        }
        // 날짜/시간이 하나도 안 잡힌 경우: 전체를 제목 후보 1건으로
        if (results.isEmpty() && rawText.isNotBlank()) {
            val title = cleanTitle(rawText.replace("\n", " "))
            if (title.isNotBlank()) {
                return listOf(
                    ParsedLine(
                        ParsedScheduleDraft(title = title, confidence = 0.2f, source = source),
                        category = guessCategory(title),
                        isImportant = hasImportant(title),
                    ),
                )
            }
        }
        return results
    }

    /**
     * 시간표 모드: "수요일" 같은 헤더 줄의 날짜를 기억했다가,
     * 시간만 있는 이후 줄("10:00 과학")에 전파한다.
     * 전파된 날짜는 직접 신호(0.45)보다 약한 0.35만 confidence에 기여.
     */
    private fun parseWithDateCarry(lines: List<String>, today: LocalDate, source: ScheduleSource): List<ParsedLine> {
        var carryDateIso: String? = null
        val results = mutableListOf<ParsedLine>()
        for (line in lines) {
            val parsed = parseLine(line, today, source) ?: continue
            if (parsed.draft.dateIso != null) {
                carryDateIso = parsed.draft.dateIso
                results += parsed
            } else if (carryDateIso != null) {
                results += parsed.copy(
                    draft = parsed.draft.copy(
                        dateIso = carryDateIso,
                        confidence = (parsed.draft.confidence + 0.35f).coerceIn(0f, 1f),
                    ),
                )
            } else {
                results += parsed
            }
        }
        return results
    }

    /** 줄/불릿 단위로 분할하고, 날짜나 시간 신호가 있는 줄만 후보로 남긴다. */
    private fun segment(text: String): List<String> {
        return text.split("\n")
            .map { it.trim().trimStart('•', '·', '-', '*', '▪', '◦', '‣').trim() }
            .map { it.replace(Regex("^\\d+[.)]\\s*"), "") } // "1. ", "2) " 제거
            .filter { it.isNotBlank() }
    }

    private fun parseLine(line: String, today: LocalDate, source: ScheduleSource): ParsedLine? {
        val consumed = mutableListOf<IntRange>()

        val date = findDate(line, today)?.also { consumed += it.range }
        val time = findTime(line)?.also { consumed += it.range }
        val location = findLocation(line)?.also { consumed += it.range }

        // 날짜도 시간도 없으면 이 줄은 일정 후보가 아님
        if (date == null && time == null) return null

        val title = cleanTitle(removeRanges(line, consumed))
        val effectiveTitle = title.ifBlank { location?.text ?: "일정" }

        var confidence = 0f
        if (date != null) confidence += 0.45f
        if (time != null) {
            confidence += 0.30f
            if (time.value != null && time.text == "no-ampm") confidence -= 0.05f
        }
        if (title.isNotBlank()) confidence += 0.20f
        if (location != null) confidence += 0.05f
        confidence = confidence.coerceIn(0f, 1f)
        // 약한 날짜 신호는 시간이 함께 잡혔을 때만 신뢰 ("일요일은 휴무입니다", "7/15 발표" 등)
        if (date != null && date.weak && time == null) confidence = confidence.coerceAtMost(0.3f)

        val draft = ParsedScheduleDraft(
            title = effectiveTitle,
            dateIso = date?.let { LocalDate.ofEpochDay(it.value!!.toLong()).toString() },
            time = time?.value?.let { "%02d:%02d".format(it / 60, it % 60) },
            location = location?.text,
            confidence = confidence,
            source = source,
        )
        return ParsedLine(
            draft = draft,
            category = guessCategory(line),
            isImportant = hasImportant(line),
        )
    }

    // ── 날짜 ──
    // Match.value = epochDay
    private fun findDate(line: String, today: LocalDate): Match? {
        // yyyy-M-d / yyyy.M.d / yyyy/M/d
        Regex("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})").find(line)?.let { m ->
            val (y, mo, d) = m.destructured
            runCatching { LocalDate.of(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()?.let {
                return Match(it.toEpochDay().toInt(), m.range)
            }
        }
        // M월 d일
        Regex("(\\d{1,2})월\\s*(\\d{1,2})일").find(line)?.let { m ->
            val (mo, d) = m.destructured
            resolveMonthDay(mo.toInt(), d.toInt(), today)?.let {
                return Match(it.toEpochDay().toInt(), m.range)
            }
        }
        // 상대 표현
        relativeDate(line, today)?.let { return it }
        // M/d (연도 없는 슬래시) — 비율·점수 문맥이면 날짜 아님, 요일 괄호 없으면 약한 신호
        Regex("(?<!\\d)(\\d{1,2})/(\\d{1,2})(?!\\d)").find(line)?.let { m ->
            val (mo, d) = m.destructured
            if (mo.toInt() in 1..12 && d.toInt() in 1..31 && !isRatioContext(line, m.range)) {
                resolveMonthDay(mo.toInt(), d.toInt(), today)?.let {
                    return Match(it.toEpochDay().toInt(), m.range, weak = !hasWeekdaySuffix(line, m.range))
                }
            }
        }
        return null
    }

    /** "평점 4/5", "진도 3/12", "4/5점" 등 비율·점수 문맥이면 날짜가 아니다. */
    private fun isRatioContext(line: String, range: IntRange): Boolean {
        val before = line.substring((range.first - 8).coerceAtLeast(0), range.first)
        val after = line.substring(range.last + 1, (range.last + 4).coerceAtMost(line.length)).trimStart()
        return Regex("(평점|별점|점수|평가|리뷰|진도|승률|정답률|만점)\\s*[:：]?\\s*$").containsMatchIn(before) ||
            after.startsWith("점") || after.startsWith("만점") || after.startsWith("개")
    }

    /** "7/15(수)"처럼 요일 괄호가 붙으면 강한 날짜 신호. */
    private fun hasWeekdaySuffix(line: String, range: IntRange): Boolean {
        val after = line.substring(range.last + 1, (range.last + 5).coerceAtMost(line.length))
        return Regex("^\\s*\\(?[월화수목금토일]\\)").containsMatchIn(after)
    }

    private fun relativeDate(line: String, today: LocalDate): Match? {
        data class R(val regex: Regex, val weak: Boolean = false, val resolve: (MatchResult) -> LocalDate?)
        val rules = listOf(
            R(Regex("내일모레|모레")) { today.plusDays(2) },
            R(Regex("글피")) { today.plusDays(3) },
            R(Regex("내일|낼")) { today.plusDays(1) },
            R(Regex("오늘")) { today },
            R(Regex("(다음|담)\\s*주\\s*([월화수목금토일])요일")) { m ->
                WEEKDAYS[m.groupValues[2]]?.let { weekdayInWeek(today.plusWeeks(1), it) }
            },
            R(Regex("이번\\s*주\\s*([월화수목금토일])요일")) { m ->
                WEEKDAYS[m.groupValues[1]]?.let { weekdayInWeek(today, it) }
            },
            R(Regex("(이번|담|다음)?\\s*주말")) { m ->
                val base = if (m.groupValues[1] == "다음" || m.groupValues[1] == "담") today.plusWeeks(1) else today
                base.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            },
            R(Regex("(다음|담)\\s*달\\s*(\\d{1,2})일")) { m ->
                val d = m.groupValues[2].toInt()
                runCatching { today.plusMonths(1).withDayOfMonth(d) }.getOrNull()
            },
            R(Regex("이번\\s*달\\s*(\\d{1,2})일")) { m ->
                val d = m.groupValues[1].toInt()
                runCatching { today.withDayOfMonth(d) }.getOrNull()
            },
            R(Regex("(\\d{1,2})일\\s*(후|뒤)")) { m -> today.plusDays(m.groupValues[1].toLong()) },
            R(Regex("([월화수목금토일])요일"), weak = true) { m ->
                WEEKDAYS[m.groupValues[1]]?.let { nextOrSameWeekday(today, it) }
            },
        )
        for (r in rules) {
            r.regex.find(line)?.let { m ->
                r.resolve(m)?.let { return Match(it.toEpochDay().toInt(), m.range, weak = r.weak) }
            }
        }
        return null
    }

    private fun weekdayInWeek(reference: LocalDate, target: DayOfWeek): LocalDate {
        val monday = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.plusDays((target.value - 1).toLong())
    }

    private fun nextOrSameWeekday(today: LocalDate, target: DayOfWeek): LocalDate =
        today.with(TemporalAdjusters.nextOrSame(target))

    /** 월/일만 있을 때 올해 기준으로 잡되, 이미 지난 날짜면 내년으로. */
    private fun resolveMonthDay(month: Int, day: Int, today: LocalDate): LocalDate? {
        val candidate = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return null
        return if (candidate.isBefore(today)) candidate.plusYears(1) else candidate
    }

    // ── 시간 ──
    // Match.value = minuteOfDay, text="no-ampm" 이면 오전/오후 불명
    private fun findTime(line: String): Match? {
        // 정오 / 자정
        Regex("정오").find(line)?.let { return Match(12 * 60, it.range) }
        Regex("자정").find(line)?.let { return Match(0, it.range) }

        // (오전|오후|아침|저녁|밤|새벽)? h시 (m분|반)?  또는 h:mm
        val colon = Regex("(오전|오후|아침|저녁|밤|새벽)?\\s*(\\d{1,2}):(\\d{2})").find(line)
        if (colon != null) {
            val period = colon.groupValues[1]
            var h = colon.groupValues[2].toInt()
            val m = colon.groupValues[3].toInt()
            h = applyPeriod(h, period)
            if (h in 0..23 && m in 0..59) {
                return Match(h * 60 + m, colon.range, if (period.isBlank()) null else "ampm")
            }
        }

        val hourMin = Regex("(오전|오후|아침|저녁|밤|새벽)?\\s*(\\d{1,2})시(?!간)\\s*(반|\\d{1,2}분)?").find(line)
        if (hourMin != null) {
            val period = hourMin.groupValues[1]
            var h = hourMin.groupValues[2].toInt()
            val minPart = hourMin.groupValues[3]
            val m = when {
                minPart == "반" -> 30
                minPart.endsWith("분") -> minPart.dropLast(1).toIntOrNull() ?: 0
                else -> 0
            }
            val noAmPm = period.isBlank()
            h = if (noAmPm) bareHourHeuristic(h) else applyPeriod(h, period)
            if (h in 0..23 && m in 0..59) {
                return Match(h * 60 + m, hourMin.range, if (noAmPm) "no-ampm" else "ampm")
            }
        }
        return null
    }

    private fun applyPeriod(hour: Int, period: String): Int = when (period) {
        "오후", "저녁", "밤" -> if (hour == 12) 12 else hour + 12
        "오전", "아침", "새벽" -> if (hour == 12) 0 else hour
        else -> hour
    }

    /** 오전/오후 표기가 없을 때: 1~7시는 오후로 간주(학생 일정 특성), 그 외는 그대로. */
    private fun bareHourHeuristic(hour: Int): Int = if (hour in 1..7) hour + 12 else hour

    // ── 장소 ──
    private fun findLocation(line: String): Match? {
        Regex("@([^\\s]+)").find(line)?.let {
            return Match(null, it.range, it.groupValues[1])
        }
        Regex("([가-힣A-Za-z0-9]+)\\s*에서").find(line)?.let {
            return Match(null, it.range, it.groupValues[1])
        }
        for (kw in LOCATION_KEYWORDS) {
            Regex("([가-힣A-Za-z0-9]*$kw)").find(line)?.let {
                return Match(null, it.range, it.groupValues[1])
            }
        }
        return null
    }

    // ── 제목/분류 ──
    private fun removeRanges(line: String, ranges: List<IntRange>): String {
        if (ranges.isEmpty()) return line
        val sb = StringBuilder()
        for (i in line.indices) {
            if (ranges.none { i in it }) sb.append(line[i])
        }
        return sb.toString()
    }

    private fun cleanTitle(raw: String): String {
        var t = raw.replace(Regex("\\s+"), " ").trim()
        // 잔여 조사/기호 정리
        t = t.trim(' ', ',', '.', '·', '-', ':', '~', '@', '(', ')', '[', ']')
        t = t.replace(Regex("^(에서|에|은|는|이|가|을|를|의|와|과|로|으로)\\s*"), "")
        t = t.replace(Regex("\\s*(에서|에|은|는|이|가|을|를|의)$"), "")
        return t.trim()
    }

    private fun guessCategory(text: String): com.example.petling.domain.model.ScheduleCategory? = when {
        STUDY_KEYWORDS.any { text.contains(it) } -> com.example.petling.domain.model.ScheduleCategory.STUDY
        APPOINTMENT_KEYWORDS.any { text.contains(it) } -> com.example.petling.domain.model.ScheduleCategory.APPOINTMENT
        HOBBY_KEYWORDS.any { text.contains(it) } -> com.example.petling.domain.model.ScheduleCategory.HOBBY
        REST_KEYWORDS.any { text.contains(it) } -> com.example.petling.domain.model.ScheduleCategory.REST
        else -> null
    }

    private fun hasImportant(text: String): Boolean = IMPORTANT_KEYWORDS.any { text.contains(it) }
}
