package com.example.petling.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** 시간 소스 추상화 — 테스트에서 고정할 수 있게 한다. */
interface AppClock {
    fun today(): LocalDate
    fun now(): Instant
    fun nowMillis(): Long
    fun nowMinuteOfDay(): Int
    /** epoch millis → 로컬 시간대 기준 epochDay. */
    fun epochDayOf(millis: Long): Long
}

class SystemAppClock(private val zone: ZoneId = ZoneId.systemDefault()) : AppClock {
    override fun today(): LocalDate = LocalDate.now(zone)
    override fun now(): Instant = Instant.now()
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun nowMinuteOfDay(): Int = LocalTime.now(zone).let { it.hour * 60 + it.minute }
    override fun epochDayOf(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
}
