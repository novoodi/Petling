package com.example.petling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.petling.domain.model.Category
import com.example.petling.domain.model.CaptureType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val key: String,
    val label: String,
    val emoji: String,
    val description: String,
    val baseType: CaptureType,
    val isBuiltIn: Boolean,
    val enabled: Boolean,
    val sortOrder: Int,
)

fun CategoryEntity.toDomain(): Category = Category(
    key = key,
    label = label,
    emoji = emoji,
    description = description,
    baseType = baseType,
    isBuiltIn = isBuiltIn,
    enabled = enabled,
    sortOrder = sortOrder,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    key = key,
    label = label,
    emoji = emoji,
    description = description,
    baseType = baseType,
    isBuiltIn = isBuiltIn,
    enabled = enabled,
    sortOrder = sortOrder,
)
