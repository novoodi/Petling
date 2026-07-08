package com.example.petling.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.petling.domain.model.CaptureItem

@Entity(tableName = "captures", indices = [Index("categoryKey"), Index("createdAt")])
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val imagePath: String,
    val ocrText: String,
    val categoryKey: String,
    val title: String,
    val summary: String? = null,
    val note: String?,
    val linkUrl: String? = null,
    val sourceScheduleId: Long?,
    val pinned: Boolean,
    val createdAt: Long,
)

fun CaptureEntity.toDomain(): CaptureItem = CaptureItem(
    id = id,
    imagePath = imagePath,
    ocrText = ocrText,
    categoryKey = categoryKey,
    title = title,
    summary = summary,
    note = note,
    linkUrl = linkUrl,
    sourceScheduleId = sourceScheduleId,
    pinned = pinned,
    createdAt = createdAt,
)

fun CaptureItem.toEntity(): CaptureEntity = CaptureEntity(
    id = id,
    imagePath = imagePath,
    ocrText = ocrText,
    categoryKey = categoryKey,
    title = title,
    summary = summary,
    note = note,
    linkUrl = linkUrl,
    sourceScheduleId = sourceScheduleId,
    pinned = pinned,
    createdAt = createdAt,
)
