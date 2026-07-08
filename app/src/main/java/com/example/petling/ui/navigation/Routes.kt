package com.example.petling.ui.navigation

import com.example.petling.domain.model.ParsedDraftSeed
import com.example.petling.domain.model.ScheduleCategory
import com.example.petling.domain.model.ScheduleSource
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** type-safe 내비게이션 라우트. */

@Serializable object OnboardingRoute

@Serializable object HomeRoute

@Serializable object CalendarRoute

@Serializable object CharacterRoute

@Serializable object SettingsRoute

@Serializable object CategoryRoute

@Serializable
data class ScheduleEditRoute(
    val scheduleId: Long? = null,
    val presetEpochDay: Long? = null,
    // 캡처/음성 파싱 draft 프리필용 seed 필드 (모두 선택)
    val seedTitle: String? = null,
    val seedEpochDay: Long? = null,
    val seedMinuteOfDay: Int? = null,
    val seedLocation: String? = null,
    val seedCategory: String? = null,
    val seedImportant: Boolean = false,
    val seedSource: String? = null,
)

@Serializable
data class ScheduleDetailRoute(val scheduleId: Long)

@Serializable object CaptureRoute

@Serializable object VoiceRoute

@Serializable object PasteRoute

@Serializable object LibraryRoute

@Serializable data class CaptureDetailRoute(val captureId: Long)

/** 파싱 seed를 편집 화면 라우트로 변환(캡처/음성 → 확인 화면 진입). */
fun ParsedDraftSeed.toEditRoute(): ScheduleEditRoute = ScheduleEditRoute(
    seedTitle = title,
    seedEpochDay = date?.toEpochDay(),
    seedMinuteOfDay = startMinuteOfDay,
    seedLocation = location,
    seedCategory = category?.name,
    seedImportant = isImportant,
    seedSource = source.name,
)

/** 편집 화면 라우트에 담긴 seed 필드를 다시 ParsedDraftSeed로 복원. seed가 없으면 null. */
fun ScheduleEditRoute.toSeed(): ParsedDraftSeed? {
    val hasSeed = seedTitle != null || seedEpochDay != null || seedMinuteOfDay != null ||
        seedLocation != null || seedSource != null
    if (!hasSeed) return null
    return ParsedDraftSeed(
        title = seedTitle,
        date = seedEpochDay?.let { LocalDate.ofEpochDay(it) },
        startMinuteOfDay = seedMinuteOfDay,
        location = seedLocation,
        category = seedCategory?.let { runCatching { ScheduleCategory.valueOf(it) }.getOrNull() },
        isImportant = seedImportant,
        // 편집 화면 진입 시 항상 검토가 필요하다고 표시하기 위해 낮은 confidence로 복원
        confidence = 0.5f,
        source = seedSource?.let { runCatching { ScheduleSource.valueOf(it) }.getOrNull() } ?: ScheduleSource.MANUAL,
    )
}
