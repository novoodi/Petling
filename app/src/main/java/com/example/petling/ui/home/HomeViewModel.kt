package com.example.petling.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.repository.CharacterRepository
import com.example.petling.data.repository.CompletionResult
import com.example.petling.data.repository.ScheduleRepository
import com.example.petling.domain.AppClock
import com.example.petling.domain.engine.GrowthStateMachine
import com.example.petling.domain.model.CharacterState
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Schedule
import com.example.petling.domain.personality.PhraseArgs
import com.example.petling.domain.personality.PhraseContext
import com.example.petling.domain.personality.PhraseSelector
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val character: CharacterState? = null,
    val todaySchedules: List<Schedule> = emptyList(),
    val greeting: String = "",
    val nextStageTarget: Int? = null,
    val progressInStage: Int = 0,
)

sealed interface HomeEvent {
    data class XpGained(val amount: Int, val message: String) : HomeEvent
    data class Evolved(val stage: GrowthStage, val message: String) : HomeEvent
}

class HomeViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val characterRepository: CharacterRepository,
    private val clock: AppClock,
    private val phraseSelector: PhraseSelector = PhraseSelector(),
) : ViewModel() {

    private val _greeting = MutableStateFlow("")
    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    /** 같은 일정에 대해 문구가 재추첨되지 않도록 캐시(일정 id → 문구). */
    private var upcomingCache: Pair<Long, String>? = null

    val uiState: StateFlow<HomeUiState> = combine(
        characterRepository.characterState,
        scheduleRepository.observeByDate(clock.today()),
        _greeting,
    ) { character, todays, greeting ->
        HomeUiState(
            character = character,
            todaySchedules = todays,
            greeting = speechFor(character, todays, greeting),
            nextStageTarget = character?.let { nextTarget(it.stage) },
            progressInStage = character?.captureCount ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /**
     * 캐릭터가 할 말을 고른다: 오늘 남은(미완료·시간 있는) 일정이 있으면
     * 그 일정을 직접 예고하고(다마고치가 비서 역할), 없으면 기본 인사를 쓴다.
     */
    private fun speechFor(
        character: CharacterState?,
        todays: List<Schedule>,
        baseGreeting: String,
    ): String {
        character ?: return baseGreeting
        val nowMin = clock.nowMinuteOfDay()
        val next = todays
            .filter {
                it.status == com.example.petling.domain.model.ScheduleStatus.PENDING &&
                    it.startMinuteOfDay != null && it.startMinuteOfDay >= nowMin
            }
            .minByOrNull { it.startMinuteOfDay!! }
            ?: return baseGreeting
        upcomingCache?.let { (id, phrase) -> if (id == next.id) return phrase }
        val phrase = phraseSelector.pick(
            character.personality,
            PhraseContext.UPCOMING,
            PhraseArgs(
                name = character.name,
                title = next.title,
                time = com.example.petling.ui.components.formatTime(next.startMinuteOfDay),
            ),
        )
        upcomingCache = next.id to phrase
        return phrase
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            scheduleRepository.sweepMissed()
            val refresh = characterRepository.refreshDailyState()
            val character = characterRepository.get()
            if (character != null) {
                val ctx = if (refresh.wasAway) PhraseContext.RETURN_WELCOME else PhraseContext.DAILY_GREETING
                _greeting.value = phraseSelector.pick(
                    character.personality,
                    ctx,
                    PhraseArgs(name = character.name),
                )
            }
        }
    }

    fun complete(schedule: Schedule) {
        viewModelScope.launch {
            val character = characterRepository.get() ?: return@launch
            val result = characterRepository.completeSchedule(schedule) ?: return@launch
            emitCompletionFeedback(schedule, result, character.name, character.personality)
        }
    }

    private suspend fun emitCompletionFeedback(
        schedule: Schedule,
        result: CompletionResult,
        name: String,
        personality: com.example.petling.domain.model.Personality,
    ) {
        val ctx = if (schedule.isImportant) PhraseContext.COMPLETED_IMPORTANT else PhraseContext.COMPLETED
        val msg = phraseSelector.pick(personality, ctx, PhraseArgs(name = name, title = schedule.title))
        _events.send(HomeEvent.XpGained(result.xpAmount, msg))
        val t = result.transition
        if (t is GrowthStateMachine.Transition.Advanced) {
            val levelMsg = phraseSelector.pick(personality, PhraseContext.LEVEL_UP, PhraseArgs(name = name))
            _events.send(HomeEvent.Evolved(t.to, levelMsg))
        }
    }

    private fun nextTarget(stage: GrowthStage): Int? = when (stage) {
        GrowthStage.JUVENILE -> GrowthStage.GROWTH1.requiredCompletions
        GrowthStage.GROWTH1 -> GrowthStage.GROWTH2.requiredCompletions
        GrowthStage.GROWTH2 -> GrowthStage.MATURE.requiredCompletions
        else -> null
    }
}
