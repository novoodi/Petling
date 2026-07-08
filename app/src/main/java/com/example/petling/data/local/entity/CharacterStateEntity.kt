package com.example.petling.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.CharacterState
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Mood
import com.example.petling.domain.model.Personality
import com.example.petling.domain.model.Species

/** 단일 행(id=0)으로 캐릭터 상태를 보관한다. */
@Entity(tableName = "character_state")
data class CharacterStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val name: String,
    val personality: Personality,
    val species: Species = Species.ACORN,
    val stage: GrowthStage,
    val branch: Branch?,
    val totalXp: Int,
    val completedCount: Int,
    val captureCount: Int = 0,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val lastCompletionEpochDay: Long?,
    val mood: Mood,
    val moodDateEpochDay: Long,
    val lastVisitEpochDay: Long,
    val colorHue: Float,
    val eyeStyle: Int,
    val quizAnswersJson: String?,
    val hatchedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

fun CharacterStateEntity.toDomain(): CharacterState = CharacterState(
    name = name,
    personality = personality,
    species = species,
    stage = stage,
    branch = branch,
    totalXp = totalXp,
    completedCount = completedCount,
    captureCount = captureCount,
    currentStreakDays = currentStreakDays,
    bestStreakDays = bestStreakDays,
    lastCompletionEpochDay = lastCompletionEpochDay,
    mood = mood,
    moodDateEpochDay = moodDateEpochDay,
    lastVisitEpochDay = lastVisitEpochDay,
    colorHue = colorHue,
    eyeStyle = eyeStyle,
    quizAnswersJson = quizAnswersJson,
    hatchedAt = hatchedAt,
)

fun CharacterState.toEntity(): CharacterStateEntity = CharacterStateEntity(
    id = CharacterStateEntity.SINGLETON_ID,
    name = name,
    personality = personality,
    species = species,
    stage = stage,
    branch = branch,
    totalXp = totalXp,
    completedCount = completedCount,
    captureCount = captureCount,
    currentStreakDays = currentStreakDays,
    bestStreakDays = bestStreakDays,
    lastCompletionEpochDay = lastCompletionEpochDay,
    mood = mood,
    moodDateEpochDay = moodDateEpochDay,
    lastVisitEpochDay = lastVisitEpochDay,
    colorHue = colorHue,
    eyeStyle = eyeStyle,
    quizAnswersJson = quizAnswersJson,
    hatchedAt = hatchedAt,
)
