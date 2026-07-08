package com.example.petling.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petling.data.datastore.SettingsDataStore
import com.example.petling.data.repository.CharacterRepository
import com.example.petling.domain.AppClock
import com.example.petling.domain.model.CharacterState
import com.example.petling.domain.model.GrowthStage
import com.example.petling.domain.model.Personality
import com.example.petling.domain.model.Species
import com.example.petling.domain.personality.PersonalityQuiz
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, HATCH, QUIZ, NAMING, CATEGORY }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val species: Species = Species.ACORN,
    val quizAnswers: List<Int> = emptyList(),
    val currentQuestion: Int = 0,
    val name: String = "",
    val colorHue: Float = 30f,
    val eyeStyle: Int = 0,
    val determinedPersonality: Personality? = null,
)

class OnboardingViewModel(
    private val characterRepository: CharacterRepository,
    private val settings: SettingsDataStore,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    val questions = PersonalityQuiz.questions

    /**
     * 부화 단계로 — 어떤 친구가 나올지는 **랜덤**(가챠 감성).
     * 종은 여기서 미리 정하되 부화 순간까지 화면에 공개하지 않는다.
     */
    fun goToHatch() {
        val species = Species.entries.filter { it.pickable }.random()
        _state.value = _state.value.copy(
            species = species,
            colorHue = species.defaultHue,
            step = OnboardingStep.HATCH,
        )
    }

    fun onHatched() {
        _state.value = _state.value.copy(step = OnboardingStep.QUIZ)
    }

    /**
     * 뒤로: 이전 단계(또는 퀴즈 직전 질문)로 되돌린다.
     * 첫 화면(WELCOME)이면 false를 반환해 시스템 뒤로에 위임(앱 종료).
     * CATEGORY는 이미 부화·완료된 뒤라 되돌리지 않고 소비만 한다.
     */
    fun goBack(): Boolean {
        val s = _state.value
        _state.value = when (s.step) {
            OnboardingStep.WELCOME -> return false
            OnboardingStep.HATCH -> s.copy(step = OnboardingStep.WELCOME)
            OnboardingStep.QUIZ ->
                if (s.currentQuestion > 0) {
                    s.copy(currentQuestion = s.currentQuestion - 1, quizAnswers = s.quizAnswers.dropLast(1))
                } else {
                    s.copy(step = OnboardingStep.HATCH)
                }
            OnboardingStep.NAMING -> s.copy(
                step = OnboardingStep.QUIZ,
                currentQuestion = (questions.size - 1).coerceAtLeast(0),
                quizAnswers = s.quizAnswers.dropLast(1),
                determinedPersonality = null,
            )
            OnboardingStep.CATEGORY -> return true // 부화 완료 후: 뒤로를 소비만(실수 종료 방지)
        }
        return true
    }

    fun answerQuestion(optionIndex: Int) {
        val s = _state.value
        val answers = s.quizAnswers + optionIndex
        if (answers.size >= questions.size) {
            val personality = PersonalityQuiz.determine(answers)
            _state.value = s.copy(
                quizAnswers = answers,
                determinedPersonality = personality,
                step = OnboardingStep.NAMING,
            )
        } else {
            _state.value = s.copy(quizAnswers = answers, currentQuestion = s.currentQuestion + 1)
        }
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name.take(12))
    }

    fun updateColorHue(hue: Float) {
        _state.value = _state.value.copy(colorHue = hue)
    }

    fun updateEyeStyle(style: Int) {
        _state.value = _state.value.copy(eyeStyle = style)
    }

    /** 작명 완료 → 캐릭터 부화·저장 후 카테고리 고르기 단계로. */
    fun completeNaming() {
        val s = _state.value
        val personality = s.determinedPersonality ?: Personality.SINCERE
        val now = clock.nowMillis()
        val today = clock.today().toEpochDay()
        val state = CharacterState(
            name = s.name.ifBlank { s.species.displayName },
            personality = personality,
            species = s.species,
            stage = GrowthStage.JUVENILE,
            colorHue = s.colorHue,
            eyeStyle = s.eyeStyle,
            moodDateEpochDay = today,
            lastVisitEpochDay = today,
            quizAnswersJson = s.quizAnswers.joinToString(","),
            hatchedAt = now,
        )
        viewModelScope.launch {
            characterRepository.hatch(state)
            settings.setOnboardingCompleted(true)
            _state.value = _state.value.copy(step = OnboardingStep.CATEGORY)
        }
    }
}
