package com.example.petling.data.local

import androidx.room.TypeConverter
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.CaptureType
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Mood
import com.example.petling.domain.model.Personality
import com.example.petling.domain.model.ScheduleCategory
import com.example.petling.domain.model.ScheduleSource
import com.example.petling.domain.model.ScheduleStatus
import com.example.petling.domain.model.Species
import com.example.petling.domain.model.XpReason

/** enum ↔ String 변환. 날짜/시간은 이미 Long/Int로 저장되어 컨버터가 필요 없다. */
class Converters {
    @TypeConverter fun statusToString(v: ScheduleStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): ScheduleStatus = ScheduleStatus.valueOf(v)

    @TypeConverter fun categoryToString(v: ScheduleCategory?): String? = v?.name
    @TypeConverter fun stringToCategory(v: String?): ScheduleCategory? = v?.let { ScheduleCategory.valueOf(it) }

    @TypeConverter fun sourceToString(v: ScheduleSource): String = v.name
    @TypeConverter fun stringToSource(v: String): ScheduleSource = ScheduleSource.valueOf(v)

    @TypeConverter fun personalityToString(v: Personality): String = v.name
    @TypeConverter fun stringToPersonality(v: String): Personality = Personality.valueOf(v)

    @TypeConverter fun stageToString(v: GrowthStage): String = v.name
    @TypeConverter fun stringToStage(v: String): GrowthStage = GrowthStage.valueOf(v)

    @TypeConverter fun branchToString(v: Branch?): String? = v?.name
    @TypeConverter fun stringToBranch(v: String?): Branch? = v?.let { Branch.valueOf(it) }

    @TypeConverter fun moodToString(v: Mood): String = v.name
    @TypeConverter fun stringToMood(v: String): Mood = Mood.valueOf(v)

    @TypeConverter fun xpReasonToString(v: XpReason): String = v.name
    @TypeConverter fun stringToXpReason(v: String): XpReason = XpReason.valueOf(v)

    @TypeConverter fun captureTypeToString(v: CaptureType): String = v.name
    @TypeConverter fun stringToCaptureType(v: String): CaptureType = CaptureType.valueOf(v)

    @TypeConverter fun speciesToString(v: Species): String = v.name
    @TypeConverter fun stringToSpecies(v: String): Species = Species.valueOf(v)
}
