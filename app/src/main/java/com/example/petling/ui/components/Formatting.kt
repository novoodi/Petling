package com.example.petling.ui.components

import com.example.petling.domain.model.Schedule
import java.time.LocalTime

/** "오후 3:00" 형태의 한국어 시각 표기. 종일 일정은 "종일". */
fun formatTime(minuteOfDay: Int?): String {
    if (minuteOfDay == null) return "종일"
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val ampm = if (time.hour < 12) "오전" else "오후"
    val hour12 = when {
        time.hour == 0 -> 12
        time.hour > 12 -> time.hour - 12
        else -> time.hour
    }
    val minute = time.minute.toString().padStart(2, '0')
    return "$ampm $hour12:$minute"
}

fun Schedule.timeLabel(): String = formatTime(startMinuteOfDay)
