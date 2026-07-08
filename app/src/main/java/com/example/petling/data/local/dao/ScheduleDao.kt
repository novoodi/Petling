package com.example.petling.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.petling.data.local.entity.ScheduleEntity
import com.example.petling.domain.model.ScheduleCategory
import com.example.petling.domain.model.ScheduleStatus
import kotlinx.coroutines.flow.Flow

/** 카테고리별 완료 건수 집계 결과. */
data class CategoryCount(val category: ScheduleCategory?, val cnt: Int)

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules WHERE dateEpochDay = :epochDay ORDER BY (startMinuteOfDay IS NULL), startMinuteOfDay ASC, id ASC")
    fun observeByDate(epochDay: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE dateEpochDay BETWEEN :startDay AND :endDay ORDER BY dateEpochDay ASC, (startMinuteOfDay IS NULL), startMinuteOfDay ASC")
    fun observeBetween(startDay: Long, endDay: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun observeById(id: Long): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntity?

    @Upsert
    suspend fun upsert(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduleEntity): Long

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Query("UPDATE schedules SET status = :status, completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: Long, status: ScheduleStatus, completedAt: Long?, now: Long)

    /**
     * 마감이 지난 PENDING 일정을 MISSED로 전환.
     * 종일 일정(startMinuteOfDay IS NULL)은 날짜가 지나야 MISSED가 된다.
     */
    @Query(
        """
        UPDATE schedules SET status = 'MISSED', updatedAt = :now
        WHERE status = 'PENDING' AND (
            dateEpochDay < :todayEpochDay
            OR (dateEpochDay = :todayEpochDay AND startMinuteOfDay IS NOT NULL AND startMinuteOfDay < :nowMinute)
        )
        """,
    )
    suspend fun sweepMissed(todayEpochDay: Long, nowMinute: Int, now: Long): Int

    /** 부팅 후 알람 재등록 대상: 아직 미래 트리거 시각이 남은 PENDING 일정. */
    @Query("SELECT * FROM schedules WHERE status = 'PENDING' AND reminderAtMillis IS NOT NULL AND reminderAtMillis > :nowMillis")
    suspend fun getPendingWithReminderAfter(nowMillis: Long): List<ScheduleEntity>

    @Query("SELECT COUNT(*) FROM schedules WHERE status = :status")
    suspend fun countByStatus(status: ScheduleStatus): Int

    @Query("SELECT COUNT(*) FROM schedules WHERE dateEpochDay = :epochDay AND status = 'COMPLETED'")
    suspend fun completedCountOnDate(epochDay: Long): Int

    /** 완료된 일정의 완료 시각(epoch millis) 목록. 스트릭 재계산용(완료한 실제 날짜 기준). */
    @Query("SELECT completedAt FROM schedules WHERE status = 'COMPLETED' AND completedAt IS NOT NULL")
    suspend fun completedTimestamps(): List<Long>

    /** 오늘 "지금까지 마감이 도래한" 일정 수(종일 포함): 기분 계산의 분모. */
    @Query(
        """
        SELECT COUNT(*) FROM schedules
        WHERE dateEpochDay = :epochDay
          AND (startMinuteOfDay IS NULL OR startMinuteOfDay <= :nowMinute)
        """,
    )
    suspend fun dueCountSoFar(epochDay: Long, nowMinute: Int): Int

    @Query("SELECT category AS category, COUNT(*) AS cnt FROM schedules WHERE status = 'COMPLETED' GROUP BY category")
    suspend fun completedCountByCategory(): List<CategoryCount>

    @Query("DELETE FROM schedules")
    suspend fun clear()
}
