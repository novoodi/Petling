package com.example.petling.data.repository

import com.example.petling.data.local.dao.ScheduleDao
import com.example.petling.data.local.entity.toDomain
import com.example.petling.data.local.entity.toEntity
import com.example.petling.domain.AppClock
import com.example.petling.domain.model.Schedule
import com.example.petling.notifications.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * 일정 CRUD + 알림 연동 + MISSED sweep.
 * 완료 처리 자체는 캐릭터 성장과 얽혀 있어 CharacterRepository가 담당한다.
 */
class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val characterRepository: CharacterRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: AppClock,
) {

    fun observeByDate(date: LocalDate): Flow<List<Schedule>> =
        scheduleDao.observeByDate(date.toEpochDay()).map { list -> list.map { it.toDomain() } }

    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<Schedule>> =
        scheduleDao.observeBetween(start.toEpochDay(), end.toEpochDay())
            .map { list -> list.map { it.toDomain() } }

    fun observeById(id: Long): Flow<Schedule?> =
        scheduleDao.observeById(id).map { it?.toDomain() }

    suspend fun getById(id: Long): Schedule? = scheduleDao.getById(id)?.toDomain()

    /** 신규 등록. 등록 XP(+1) 지급 후 알람을 건다. 반환값은 생성된 id. */
    suspend fun create(schedule: Schedule): Long {
        val now = clock.nowMillis()
        val withReminder = schedule.copy(reminderAtMillis = ReminderTime.compute(schedule, now))
        val id = scheduleDao.insert(withReminder.copy(createdAt = now, updatedAt = now).toEntity())
        val saved = withReminder.copy(id = id, createdAt = now, updatedAt = now)
        characterRepository.grantRegistrationXp(id)
        alarmScheduler.schedule(saved)
        return id
    }

    /** 수정. 알람을 재등록(덮어쓰기)한다. 등록 XP는 재지급하지 않는다. */
    suspend fun update(schedule: Schedule) {
        val updated = schedule.copy(
            reminderAtMillis = ReminderTime.compute(schedule, clock.nowMillis()),
            updatedAt = clock.nowMillis(),
        )
        scheduleDao.update(updated.toEntity())
        alarmScheduler.schedule(updated)
    }

    suspend fun delete(schedule: Schedule) {
        scheduleDao.delete(schedule.toEntity())
        alarmScheduler.cancel(schedule.id)
    }

    /** 마감 지난 PENDING → MISSED 일괄 전환. 반환값은 전환된 건수. */
    suspend fun sweepMissed(): Int =
        scheduleDao.sweepMissed(
            todayEpochDay = clock.today().toEpochDay(),
            nowMinute = clock.nowMinuteOfDay(),
            now = clock.nowMillis(),
        )
}
