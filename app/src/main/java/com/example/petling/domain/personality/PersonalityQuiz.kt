package com.example.petling.domain.personality

import com.example.petling.domain.model.Personality

/** 부화 시 성격을 결정하는 학생 공감형 퀴즈. */
data class QuizOption(val text: String, val weights: Map<Personality, Int>)
data class QuizQuestion(val text: String, val options: List<QuizOption>)

object PersonalityQuiz {

    /** 동점 시 이 우선순위 순서로 성격을 확정한다. */
    private val tieBreakOrder = listOf(
        Personality.SINCERE,
        Personality.WORRIER,
        Personality.FREE_SPIRIT,
        Personality.DREAMER,
    )

    val questions: List<QuizQuestion> = listOf(
        QuizQuestion(
            "시험 전날 밤, 너는?",
            listOf(
                QuizOption("계획대로 마무리하고 잔다", mapOf(Personality.SINCERE to 2)),
                QuizOption("벼락치기의 스릴을 즐긴다", mapOf(Personality.FREE_SPIRIT to 2)),
                QuizOption("불안해서 계속 확인한다", mapOf(Personality.WORRIER to 2)),
                QuizOption("딴생각하다 잠들어 버린다", mapOf(Personality.DREAMER to 2)),
            ),
        ),
        QuizQuestion(
            "약속 시간 30분 전, 너는?",
            listOf(
                QuizOption("이미 도착해서 기다린다", mapOf(Personality.SINCERE to 2)),
                QuizOption("느긋하게 이제 출발한다", mapOf(Personality.FREE_SPIRIT to 2)),
                QuizOption("늦을까 봐 몇 번이나 확인한다", mapOf(Personality.WORRIER to 2)),
                QuizOption("깜빡하고 다른 걸 하고 있다", mapOf(Personality.DREAMER to 2)),
            ),
        ),
        QuizQuestion(
            "갑자기 하루가 통째로 비면?",
            listOf(
                QuizOption("밀린 할 일을 처리한다", mapOf(Personality.SINCERE to 2)),
                QuizOption("무작정 놀러 나간다", mapOf(Personality.FREE_SPIRIT to 2)),
                QuizOption("이래도 되나 조금 불안하다", mapOf(Personality.WORRIER to 2)),
                QuizOption("멍하니 공상에 잠긴다", mapOf(Personality.DREAMER to 2)),
            ),
        ),
        QuizQuestion(
            "새 학기 다짐은?",
            listOf(
                QuizOption("빈틈없는 계획표를 짠다", mapOf(Personality.SINCERE to 2)),
                QuizOption("그때그때 재밌게 살기", mapOf(Personality.FREE_SPIRIT to 2)),
                QuizOption("이번엔 실수하지 말자", mapOf(Personality.WORRIER to 2)),
                QuizOption("멋진 상상부터 부풀린다", mapOf(Personality.DREAMER to 2)),
            ),
        ),
    )

    /**
     * 선택한 옵션 인덱스 목록으로 성격을 판정한다.
     * 가중치 합산 최대, 동점이면 tieBreakOrder 우선순위로 확정.
     */
    fun determine(selected: List<Int>): Personality {
        val scores = Personality.entries.associateWith { 0 }.toMutableMap()
        selected.forEachIndexed { qIndex, optIndex ->
            val question = questions.getOrNull(qIndex) ?: return@forEachIndexed
            val option = question.options.getOrNull(optIndex) ?: return@forEachIndexed
            option.weights.forEach { (personality, weight) ->
                scores[personality] = (scores[personality] ?: 0) + weight
            }
        }
        val maxScore = scores.values.maxOrNull() ?: 0
        return tieBreakOrder.first { scores[it] == maxScore }
    }
}
