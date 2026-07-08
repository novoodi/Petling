package com.example.petling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.GrowthStage

/**
 * 성장 다이어리 스냅샷. 이미지 대신 렌더 스펙 JSON을 저장해
 * 언제든 그 시점 모습을 다시 그린다(용량 0, 해상도 무관).
 */
@Entity(tableName = "growth_snapshots")
data class GrowthSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val stage: GrowthStage,
    val branch: Branch?,
    val characterSpecJson: String,
    val totalXpAtTime: Int,
    val completedCountAtTime: Int,
    val note: String?,
    val achievedAt: Long,
)
