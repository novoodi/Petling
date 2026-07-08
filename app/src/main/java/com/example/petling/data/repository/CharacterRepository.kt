package com.example.petling.data.repository

import androidx.room.withTransaction
import com.example.petling.data.local.PetlingDatabase
import com.example.petling.data.local.entity.GrowthSnapshotEntity
import com.example.petling.data.local.entity.XpLogEntity
import com.example.petling.data.local.entity.toDomain
import com.example.petling.data.local.entity.toEntity
import com.example.petling.domain.AppClock
import com.example.petling.domain.capture.CaptureBranchResolver
import com.example.petling.domain.engine.AffectionLevel
import com.example.petling.domain.engine.AffectionRules
import com.example.petling.domain.engine.BranchResolver
import com.example.petling.domain.engine.GrowthStateMachine
import com.example.petling.domain.engine.MoodCalculator
import com.example.petling.domain.engine.StreakCalculator
import com.example.petling.domain.engine.XpRules
import com.example.petling.domain.engine.XpEngine
import com.example.petling.domain.model.Branch
import com.example.petling.domain.model.CharacterAnimation
import com.example.petling.domain.model.CharacterSpec
import com.example.petling.domain.model.CharacterState
import com.example.petling.domain.model.Expression
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Mood
import com.example.petling.domain.model.Schedule
import com.example.petling.domain.model.ScheduleStatus
import com.example.petling.domain.model.XpReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** 일정 완료 처리 결과 — UI가 문구/애니메이션/진화 연출에 사용한다. */
data class CompletionResult(
    val xpAmount: Int,
    val newStreakDays: Int,
    val transition: GrowthStateMachine.Transition,
    val newBranch: Branch?,
    val affectionLevelUp: AffectionLevel? = null,
)

/** 홈 진입 갱신 결과. */
data class DailyRefresh(
    val wasAway: Boolean,
    val affectionLevelUp: AffectionLevel? = null,
)

/** 간식 주기 결과. null이면 오늘 소진. */
data class SnackResult(
    val remaining: Int,
    val affectionLevelUp: AffectionLevel? = null,
)

/** 캡처 보관 시 캐릭터 성장 결과. */
data class CaptureGrowth(
    val xpAmount: Int,
    val isNewType: Boolean,
    val transition: GrowthStateMachine.Transition,
    val newBranch: Branch?,
)

/**
 * 캐릭터 상태·XP·성장·기분·스냅샷을 총괄한다.
 * 완료 처리는 단일 트랜잭션으로 원자적으로 수행한다.
 */
class CharacterRepository(
    private val db: PetlingDatabase,
    private val clock: AppClock,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val characterDao = db.characterDao()
    private val scheduleDao = db.scheduleDao()
    private val xpLogDao = db.xpLogDao()
    private val snapshotDao = db.growthSnapshotDao()
    private val captureDao = db.captureDao()
    private val categoryDao = db.categoryDao()

    val characterState: Flow<CharacterState?> =
        characterDao.observe().map { it?.toDomain() }

    val snapshots = snapshotDao.observeAll()

    suspend fun get(): CharacterState? = characterDao.get()?.toDomain()

    /** 온보딩: 캐릭터 부화. 알/유생기 스냅샷 2건을 남긴다. */
    suspend fun hatch(state: CharacterState) {
        db.withTransaction {
            characterDao.upsert(state.toEntity())
            recordSnapshot(state.copy(stage = GrowthStage.EGG), note = "알에서 시작한 날")
            recordSnapshot(state, note = "${state.name}(으)로 부화했어요!")
        }
    }

    /** 일정 등록 시 소량 XP(+1). */
    suspend fun grantRegistrationXp(scheduleId: Long?) {
        val current = characterDao.get()?.toDomain() ?: return
        val gain = XpEngine.registrationGain()
        db.withTransaction {
            xpLogDao.insert(
                XpLogEntity(
                    scheduleId = scheduleId,
                    reason = XpReason.REGISTER,
                    baseXp = gain.base,
                    streakMultiplier = gain.multiplier.toFloat(),
                    amount = gain.total,
                    streakDaysAtTime = current.currentStreakDays,
                    createdAt = clock.nowMillis(),
                ),
            )
            characterDao.upsert(current.copy(totalXp = current.totalXp + gain.total).toEntity())
        }
    }

    /**
     * 캡처 보관 시 캐릭터 성장(정리함 핵심 루프).
     * 다양성 보너스 → captureCount 증가 → 단계 전이(→지배 타입 분기→스냅샷).
     * @param isNewType 이 카테고리를 처음 캡처하는지(캡처 insert 전에 판정해 넘긴다).
     */
    suspend fun recordCapture(categoryKey: String, isNewType: Boolean): CaptureGrowth? {
        val current = characterDao.get()?.toDomain() ?: return null
        val gain = XpRules.captureXp(isNewType)
        val newCaptureCount = current.captureCount + 1
        val transition = GrowthStateMachine.evaluate(current.stage, newCaptureCount)
        var resolvedBranch: Branch? = current.branch

        db.withTransaction {
            xpLogDao.insert(
                XpLogEntity(
                    scheduleId = null,
                    reason = if (isNewType) XpReason.CAPTURE_VARIETY else XpReason.CAPTURE,
                    baseXp = XpRules.CAPTURE,
                    streakMultiplier = 1.0f,
                    amount = gain,
                    streakDaysAtTime = current.currentStreakDays,
                    createdAt = clock.nowMillis(),
                ),
            )
            // 캡처 정리도 관계를 쌓는다(+1)
            val (withAffection, _) =
                gainAffection(rolloverAffection(current, clock.today().toEpochDay()), AffectionRules.CAPTURE)
            var updated = withAffection.copy(
                totalXp = current.totalXp + gain,
                captureCount = newCaptureCount,
            )
            if (transition is GrowthStateMachine.Transition.Advanced) {
                if (transition.requiresBranchChoice) {
                    resolvedBranch = CaptureBranchResolver.resolve(
                        captureDao.countByKey().associate { it.categoryKey to it.cnt },
                        categoryDao.getAll().map { it.toDomain() },
                    )
                }
                updated = updated.copy(stage = transition.to, branch = resolvedBranch)
                recordSnapshot(updated, note = captureSnapshotNote(transition.to, resolvedBranch))
            }
            characterDao.upsert(updated.toEntity())
        }

        return CaptureGrowth(
            xpAmount = gain,
            isNewType = isNewType,
            transition = transition,
            newBranch = resolvedBranch,
        )
    }

    /** 개발용: 캡처 카운트를 주입해 성장 경계를 테스트한다. */
    suspend fun debugSetCaptureCount(count: Int) {
        val current = characterDao.get()?.toDomain() ?: return
        val stage = GrowthStateMachine.stageFor(count)
        val branch = if (stage.ordinal >= GrowthStage.GROWTH2.ordinal && current.branch == null) {
            Branch.BALANCED
        } else current.branch
        characterDao.upsert(current.copy(captureCount = count, stage = stage, branch = branch).toEntity())
    }

    /**
     * 일정 완료 처리. 스트릭 갱신 → XP → 상태 갱신 → 단계 전이(→분기→스냅샷) → 기분 재계산.
     * 반환값이 null이면 캐릭터가 없거나 이미 완료된 일정.
     */
    suspend fun completeSchedule(schedule: Schedule): CompletionResult? {
        val current = characterDao.get()?.toDomain() ?: return null
        if (schedule.status == ScheduleStatus.COMPLETED) return null

        val today = clock.today()
        val lastDay = current.lastCompletionEpochDay?.let { LocalDate.ofEpochDay(it) }
        val newStreak = StreakCalculator.onCompletion(lastDay, today, current.currentStreakDays)
        val gain = XpEngine.completionGain(schedule.isImportant, newStreak)
        val newCompletedCount = current.completedCount + 1
        val (withAffection, affectionLevelUp) =
            gainAffection(rolloverAffection(current, today.toEpochDay()), AffectionRules.COMPLETE)

        db.withTransaction {
            scheduleDao.setStatus(schedule.id, ScheduleStatus.COMPLETED, clock.nowMillis(), clock.nowMillis())
            xpLogDao.insert(
                XpLogEntity(
                    scheduleId = schedule.id,
                    reason = gain.reason,
                    baseXp = gain.base,
                    streakMultiplier = gain.multiplier.toFloat(),
                    amount = gain.total,
                    streakDaysAtTime = newStreak,
                    createdAt = clock.nowMillis(),
                ),
            )
            // 정리함 컨셉: 성장 단계는 캡처가 구동한다. 일정 완료는 XP·스트릭·통계·호감도만 반영.
            characterDao.upsert(
                withAffection.copy(
                    totalXp = current.totalXp + gain.total,
                    completedCount = newCompletedCount,
                    currentStreakDays = newStreak,
                    bestStreakDays = maxOf(current.bestStreakDays, newStreak),
                    lastCompletionEpochDay = today.toEpochDay(),
                    mood = recalcMood(today),
                    moodDateEpochDay = today.toEpochDay(),
                ).toEntity(),
            )
        }

        return CompletionResult(
            xpAmount = gain.total,
            newStreakDays = newStreak,
            transition = GrowthStateMachine.Transition.None,
            newBranch = current.branch,
            affectionLevelUp = affectionLevelUp,
        )
    }

    /**
     * 완료 취소. 단계는 되돌리지 않는다.
     * - XP: 이 완료로 실제 지급된 금액(스트릭 배율 포함)을 로그에서 조회해 정확히 상쇄한다.
     * - 스트릭: 남은 완료 이력(완료한 실제 날짜 기준)으로부터 재계산해 부풀림 없이 복원한다.
     */
    suspend fun revertCompletion(schedule: Schedule) {
        val current = characterDao.get()?.toDomain() ?: return
        if (schedule.status != ScheduleStatus.COMPLETED) return

        db.withTransaction {
            // 실제 지급된 XP를 정확히 상쇄. 로그가 없으면(구버전 데이터) 규칙 기준으로 근사.
            val grantedXp = xpLogDao.lastPositiveGrantForSchedule(schedule.id)
                ?: XpEngine.completionGain(schedule.isImportant, current.currentStreakDays).total

            scheduleDao.setStatus(schedule.id, ScheduleStatus.PENDING, null, clock.nowMillis())
            xpLogDao.insert(
                XpLogEntity(
                    scheduleId = schedule.id,
                    reason = XpReason.REVERT,
                    baseXp = grantedXp,
                    streakMultiplier = 1.0f,
                    amount = -grantedXp,
                    streakDaysAtTime = current.currentStreakDays,
                    createdAt = clock.nowMillis(),
                ),
            )

            // 이 일정을 PENDING으로 되돌린 뒤 남은 완료 이력으로 스트릭·마지막 완료일 재계산
            val completedDays = scheduleDao.completedTimestamps()
                .map { clock.epochDayOf(it) }
                .toSet()
            val newLastDay = completedDays.maxOrNull()
            val newStreak = StreakCalculator.streakEndingAt(newLastDay, completedDays)

            val today = clock.today()
            characterDao.upsert(
                current.copy(
                    totalXp = (current.totalXp - grantedXp).coerceAtLeast(0),
                    completedCount = (current.completedCount - 1).coerceAtLeast(0),
                    currentStreakDays = newStreak,
                    lastCompletionEpochDay = newLastDay,
                    mood = recalcMood(today),
                    moodDateEpochDay = today.toEpochDay(),
                ).toEntity(),
            )
        }
    }

    /** 일일 카운터(호감도 상한·간식)를 날짜가 바뀌었으면 리셋한다. */
    private fun rolloverAffection(state: CharacterState, todayEpoch: Long): CharacterState =
        if (state.affectionDateEpochDay != todayEpoch) {
            state.copy(affectionDateEpochDay = todayEpoch, affectionGainedToday = 0, snacksToday = 0)
        } else {
            state
        }

    /** 호감도 가산을 적용한 상태와 레벨업 여부를 돌려준다. */
    private fun gainAffection(state: CharacterState, gain: Int): Pair<CharacterState, AffectionLevel?> {
        val applied = AffectionRules.apply(state.affection, state.affectionGainedToday, gain)
        return state.copy(
            affection = applied.newValue,
            affectionGainedToday = state.affectionGainedToday + applied.gained,
        ) to applied.levelUp
    }

    /** 홈 진입 등에서 기분/방문일을 갱신하고, 하루 첫 방문이면 호감도를 준다. */
    suspend fun refreshDailyState(): DailyRefresh {
        val current = characterDao.get()?.toDomain() ?: return DailyRefresh(wasAway = false)
        val today = clock.today()
        val todayEpoch = today.toEpochDay()
        val wasAway = current.lastVisitEpochDay in 1 until (todayEpoch - 2)
        val firstVisitToday = current.lastVisitEpochDay != todayEpoch

        var state = rolloverAffection(current, todayEpoch)
        if (wasAway) {
            // 감쇠 먼저(가산 전) — 단계는 떨어지지 않음
            val daysAway = todayEpoch - current.lastVisitEpochDay
            state = state.copy(affection = AffectionRules.decayOnReturn(state.affection, daysAway))
        }
        var levelUp: AffectionLevel? = null
        if (firstVisitToday) {
            val (gained, up) = gainAffection(state, AffectionRules.FIRST_VISIT)
            state = gained
            levelUp = up
        }

        characterDao.upsert(
            state.copy(
                mood = recalcMood(today),
                moodDateEpochDay = todayEpoch,
                lastVisitEpochDay = todayEpoch,
            ).toEntity(),
        )
        return DailyRefresh(wasAway = wasAway, affectionLevelUp = levelUp)
    }

    /**
     * 간식 주기. 하루 [AffectionRules.SNACKS_PER_DAY]개 한도, 호감도 +5.
     * 한도 소진이면 null.
     */
    suspend fun giveSnack(): SnackResult? {
        val current = characterDao.get()?.toDomain() ?: return null
        val todayEpoch = clock.today().toEpochDay()
        var state = rolloverAffection(current, todayEpoch)
        if (state.snacksToday >= AffectionRules.SNACKS_PER_DAY) return null
        val (gained, levelUp) = gainAffection(state, AffectionRules.SNACK)
        state = gained.copy(snacksToday = gained.snacksToday + 1)
        characterDao.upsert(state.toEntity())
        return SnackResult(
            remaining = AffectionRules.SNACKS_PER_DAY - state.snacksToday,
            affectionLevelUp = levelUp,
        )
    }

    /** 개발용: 완료 카운트를 주입해 성장 경계를 테스트한다. */
    suspend fun debugSetCompletedCount(count: Int) {
        val current = characterDao.get()?.toDomain() ?: return
        val stage = GrowthStateMachine.stageFor(count)
        val branch = if (stage.ordinal >= GrowthStage.GROWTH2.ordinal && current.branch == null) {
            Branch.BALANCED
        } else current.branch
        characterDao.upsert(current.copy(completedCount = count, stage = stage, branch = branch).toEntity())
    }

    suspend fun updateCustomization(name: String, colorHue: Float, eyeStyle: Int) {
        val current = characterDao.get()?.toDomain() ?: return
        characterDao.upsert(current.copy(name = name, colorHue = colorHue, eyeStyle = eyeStyle).toEntity())
    }

    private suspend fun recalcMood(today: LocalDate): Mood {
        val epochDay = today.toEpochDay()
        val nowMinute = clock.nowMinuteOfDay()
        val due = scheduleDao.dueCountSoFar(epochDay, nowMinute)
        val completed = scheduleDao.completedCountOnDate(epochDay)
        return MoodCalculator.calculate(due, completed)
    }

    private suspend fun recordSnapshot(state: CharacterState, note: String?) {
        val spec = CharacterSpec.from(
            state,
            expression = if (state.stage == GrowthStage.EGG) Expression.NEUTRAL else Expression.HAPPY,
            animation = CharacterAnimation.IDLE,
        )
        snapshotDao.insert(
            GrowthSnapshotEntity(
                stage = state.stage,
                branch = state.branch,
                characterSpecJson = json.encodeToString(CharacterSpec.serializer(), spec),
                totalXpAtTime = state.totalXp,
                completedCountAtTime = state.completedCount,
                note = note,
                achievedAt = clock.nowMillis(),
            ),
        )
    }

    private fun snapshotNote(stage: GrowthStage, branch: Branch?): String = when (stage) {
        GrowthStage.GROWTH1 -> "성장기 1에 접어들었어요"
        GrowthStage.GROWTH2 -> "성장기 2 — ${branch?.displayName ?: "균형형"}(으)로 진화했어요"
        GrowthStage.MATURE -> "성숙기에 도달했어요!"
        else -> "${stage.displayName}이 되었어요"
    }

    private fun captureSnapshotNote(stage: GrowthStage, branch: Branch?): String = when (stage) {
        GrowthStage.GROWTH1 -> "캡처가 쌓여 성장기 1에 접어들었어요"
        GrowthStage.GROWTH2 -> "다양한 걸 모아 ${CaptureBranchResolver.displayName(branch)}(으)로 진화했어요"
        GrowthStage.MATURE -> "성숙기에 도달했어요!"
        else -> "${stage.displayName}이 되었어요"
    }

    fun specFromSnapshotJson(jsonStr: String): CharacterSpec =
        json.decodeFromString(CharacterSpec.serializer(), jsonStr)
}
