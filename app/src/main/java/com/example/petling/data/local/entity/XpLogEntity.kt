package com.example.petling.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.petling.domain.model.XpReason

@Entity(
    tableName = "xp_log",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("scheduleId")],
)
data class XpLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val scheduleId: Long?,
    val reason: XpReason,
    val baseXp: Int,
    val streakMultiplier: Float,
    /** 최종 지급 XP. REVERT(완료 취소)는 음수. */
    val amount: Int,
    val streakDaysAtTime: Int,
    val createdAt: Long,
)
