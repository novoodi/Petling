package com.example.petling.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.petling.domain.model.Schedule
import com.example.petling.domain.model.ScheduleCategory
import com.example.petling.domain.model.ScheduleSource
import com.example.petling.domain.model.ScheduleStatus
import java.time.LocalDate

@Entity(tableName = "schedules", indices = [Index("dateEpochDay")])
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val dateEpochDay: Long,
    val startMinuteOfDay: Int?,
    val location: String?,
    val memo: String?,
    val category: ScheduleCategory?,
    val isImportant: Boolean,
    val status: ScheduleStatus,
    val source: ScheduleSource,
    val reminderOffsetMin: Int?,
    val reminderAtMillis: Long?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ScheduleEntity.toDomain(): Schedule = Schedule(
    id = id,
    title = title,
    date = LocalDate.ofEpochDay(dateEpochDay),
    startMinuteOfDay = startMinuteOfDay,
    location = location,
    memo = memo,
    category = category,
    isImportant = isImportant,
    status = status,
    source = source,
    reminderOffsetMin = reminderOffsetMin,
    reminderAtMillis = reminderAtMillis,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Schedule.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    title = title,
    dateEpochDay = date.toEpochDay(),
    startMinuteOfDay = startMinuteOfDay,
    location = location,
    memo = memo,
    category = category,
    isImportant = isImportant,
    status = status,
    source = source,
    reminderOffsetMin = reminderOffsetMin,
    reminderAtMillis = reminderAtMillis,
    completedAt = completedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
