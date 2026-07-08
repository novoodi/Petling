package com.example.petling.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.petling.data.local.entity.XpLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface XpLogDao {

    @Insert
    suspend fun insert(log: XpLogEntity): Long

    @Query("SELECT * FROM xp_log ORDER BY createdAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<XpLogEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_log")
    suspend fun sumAmount(): Int

    /** 특정 일정에 지급된 가장 최근 양수 XP(완료 보상, 배율 포함). 완료 취소 시 정확히 상쇄한다. */
    @Query("SELECT amount FROM xp_log WHERE scheduleId = :scheduleId AND amount > 0 ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun lastPositiveGrantForSchedule(scheduleId: Long): Int?

    @Query("DELETE FROM xp_log")
    suspend fun clear()
}
