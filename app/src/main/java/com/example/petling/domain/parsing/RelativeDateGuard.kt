package com.example.petling.domain.parsing

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * LLM(Nano) 파서가 계산한 상대 날짜를 결정적으로 검증·보정한다.
 *
 * 온디바이스 소형 모델은 "토요일 11시" 같은 요일→날짜 산술을 자주 틀린다
 * (프롬프트에 준 오늘 날짜를 그대로 반환하는 패턴). 분류기의 동의어 흡수와
 * 같은 원칙: Nano의 자연어 이해는 쓰되, 산술은 코드가 책임진다.
 *
 * 의미론은 KoreanScheduleParser와 동일하게 맞춘다:
 * - 요일 단독 = 오늘 포함 다음 해당 요일(nextOrSame)
 * - "다음 주 X요일" = 차주(월요일 기준 주)의 해당 요일
 * - "이번 주 X요일" = 이번 주(월요일 기준)의 해당 요일
 *
 * 보정하지 않는 경우(=Nano 값 신뢰):
 * - 원문에 절대 날짜(M월 d일, M/d, ISO)가 있으면 — 상대 토큰은 장식일 수 있음
 * - 요일이 2종 이상 언급되면(시간표 등 다중 일정) — 어느 시드의 날짜인지 모호
 */
object RelativeDateGuard {

    private val WEEKDAYS = mapOf(
        "월" to DayOfWeek.MONDAY, "화" to DayOfWeek.TUESDAY, "수" to DayOfWeek.WEDNESDAY,
        "목" to DayOfWeek.THURSDAY, "금" to DayOfWeek.FRIDAY, "토" to DayOfWeek.SATURDAY,
        "일" to DayOfWeek.SUNDAY,
    )

    private val ABSOLUTE_DATE = Regex("""\d{1,2}\s*월\s*\d{1,2}\s*일|\d{1,2}/\d{1,2}|\d{4}-\d{2}-\d{2}""")
    private val NEXT_WEEK = Regex("""(다음|담)\s*주\s*([월화수목금토일])(요일|욜)""")
    private val THIS_WEEK = Regex("""이번\s*주\s*([월화수목금토일])(요일|욜)""")
    private val WEEKDAY = Regex("""([월화수목금토일])(요일|욜)""")
    private val DAY_AFTER_TOMORROW = Regex("""내일\s*모레|모레""")

    /**
     * Nano가 반환한 날짜(parsed)를 원문의 상대 날짜 토큰과 대조해,
     * 불일치하면 코드가 계산한 기대 날짜로 교체한다. parsed가 null이어도
     * 토큰이 있으면 채워 준다. 판단 근거가 없으면 parsed 그대로.
     */
    fun verify(rawText: String, parsed: LocalDate?, today: LocalDate): LocalDate? {
        if (ABSOLUTE_DATE.containsMatchIn(rawText)) return parsed
        val expected = expectedDate(rawText, today) ?: return parsed
        return expected
    }

    private fun expectedDate(text: String, today: LocalDate): LocalDate? {
        val hasDayOffset = DAY_AFTER_TOMORROW.containsMatchIn(text) || text.contains("내일")
        val mentioned = WEEKDAY.findAll(text).map { it.groupValues[1] }.distinct().toList()

        // 상대 표현이 2종류 이상 섞이면("내일 3시, 금요일 2시" / 요일 2종=시간표)
        // 어느 시드의 날짜인지 모호 → 보정 포기(Nano 값 유지)
        if (hasDayOffset && mentioned.isNotEmpty()) return null
        if (mentioned.size > 1) return null

        if (DAY_AFTER_TOMORROW.containsMatchIn(text)) return today.plusDays(2)
        if (text.contains("내일")) return today.plusDays(1)

        NEXT_WEEK.find(text)?.let { m ->
            return weekdayInWeek(today.plusWeeks(1), WEEKDAYS.getValue(m.groupValues[2]))
        }
        THIS_WEEK.find(text)?.let { m ->
            return weekdayInWeek(today, WEEKDAYS.getValue(m.groupValues[1]))
        }
        WEEKDAY.find(text)?.let { m ->
            return today.with(TemporalAdjusters.nextOrSame(WEEKDAYS.getValue(m.groupValues[1])))
        }
        return null
    }

    private fun weekdayInWeek(reference: LocalDate, target: DayOfWeek): LocalDate {
        val monday = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.with(TemporalAdjusters.nextOrSame(target))
    }
}
