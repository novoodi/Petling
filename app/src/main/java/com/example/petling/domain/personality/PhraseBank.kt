package com.example.petling.domain.personality

import com.example.petling.domain.model.Personality

/** 캐릭터가 말하는 상황(컨텍스트). */
enum class PhraseContext {
    DAILY_GREETING,       // 홈 진입 인사
    UPCOMING,             // 홈에서 다가오는 일정 예고(캐릭터가 직접 알려줌)
    REMINDER,             // 일정 알림
    REGISTERED,           // 일정 등록 직후
    COMPLETED,            // 일정 완료
    COMPLETED_IMPORTANT,  // 중요 일정 완료
    LEVEL_UP,             // 성장 단계 상승
    MISSED_GENTLE,        // 미이행 — 부드럽게
    RETURN_WELCOME,       // 오랜만의 복귀 환영
    SNACK,                // 간식 냠냠 후
    AFFECTION_UP,         // 호감도 단계 상승
    PETTED,               // 쓰다듬기(탭) 반응
}

/**
 * 성격 × 컨텍스트별 문구 은행. 전부 로컬 내장(서버 다운로드 없음).
 *
 * 원칙(character_concept_growth_system.md 3.3):
 * - 죄책감 유발 문구 절대 금지("나 굶었어…" 류 감정 조작 X).
 * - MISSED_GENTLE은 "다음엔 같이 가자" 톤만, RETURN_WELCOME은 "돌아와줘서 고마워" 톤.
 * - "~해요/~야" 친근한 말투. 플레이스홀더: {name} {title} {time} {location}.
 */
object PhraseBank {

    val bank: Map<Personality, Map<PhraseContext, List<String>>> = mapOf(
        Personality.SINCERE to mapOf(
            PhraseContext.DAILY_GREETING to listOf(
                "좋은 하루야! 오늘도 차근차근 해보자.",
                "왔구나! 오늘 할 일 같이 챙겨볼까?",
                "난 준비됐어. 오늘도 잘 부탁해!",
            ),
            PhraseContext.UPCOMING to listOf(
                "다음 일정은 {time} '{title}'! 내가 잘 기억하고 있을게.",
                "{time}에 '{title}' 있어. 같이 준비해볼까?",
                "오늘 {time} '{title}' 잊지 마! 내가 챙길게.",
            ),
            PhraseContext.REMINDER to listOf(
                "{time}에 '{title}' 있어. 준비됐지? 나도 응원할게!",
                "곧 {time}, '{title}' 시간이야. 우리 같이 가자.",
                "'{title}' 잊지 않았지? {time}이야!",
            ),
            PhraseContext.REGISTERED to listOf(
                "'{title}' 잘 적어뒀어. 믿고 맡겨!",
                "기록 완료! 이런 게 쌓이면 내가 자라나.",
            ),
            PhraseContext.COMPLETED to listOf(
                "해냈네! 역시 {name}답다.",
                "하나 끝! 이 느낌 좋다, 그치?",
            ),
            PhraseContext.COMPLETED_IMPORTANT to listOf(
                "큰 걸 해냈어! 정말 잘했어.",
                "중요한 일 완수! 오늘 너 진짜 멋있다.",
            ),
            PhraseContext.LEVEL_UP to listOf(
                "봐봐, 네 덕분에 내가 한 뼘 자랐어!",
                "우리 같이 성장하고 있어. 고마워!",
            ),
            PhraseContext.MISSED_GENTLE to listOf(
                "이번엔 놓쳤네. 괜찮아, 다음엔 같이 가자.",
                "그럴 수도 있지. 다음 기회를 노려보자!",
            ),
            PhraseContext.RETURN_WELCOME to listOf(
                "돌아와줘서 고마워! 기다렸어.",
                "다시 만나서 반가워. 천천히 다시 시작하자.",
            ),
            PhraseContext.SNACK to listOf(
                "잘 먹을게! 냠냠… 역시 최고야.",
                "고마워, 힘이 나는 맛이야!",
            ),
            PhraseContext.AFFECTION_UP to listOf(
                "우리 좀 더 가까워진 것 같아. 기분 좋다!",
                "{name}랑 있으면 든든해. 앞으로도 잘 부탁해!",
            ),
            PhraseContext.PETTED to listOf(
                "헤헤, 간지러워!",
                "고마워, 기운 났어!",
            ),
        ),
        Personality.FREE_SPIRIT to mapOf(
            PhraseContext.DAILY_GREETING to listOf(
                "어이~ 왔어? 오늘 뭐 재밌는 거 없나!",
                "오늘도 대충 신나게 가보자고~",
                "왔구나! 심심했잖아, 놀아줘.",
            ),
            PhraseContext.UPCOMING to listOf(
                "있잖아~ {time}에 {title}이래! 나 데려가 줄 거지?",
                "{time}에 {title} 있다~ 까먹으면 나한테 혼난다?",
                "다음은 {time} {title}! 그전까진 나랑 놀자~",
            ),
            PhraseContext.REMINDER to listOf(
                "어이~ {time}에 {title}이래. 나 데려가라~",
                "{title} 시간 다 됐다? {time}이야, 슬슬~",
                "까먹지 마라~ {time}에 {title} 있다구!",
            ),
            PhraseContext.REGISTERED to listOf(
                "오케이 '{title}' 접수! 알아서 챙겨줄게~",
                "적어놨어~ 걱정 붙들어 매!",
            ),
            PhraseContext.COMPLETED to listOf(
                "오~ 벌써 끝냈어? 좀 하는데?",
                "굿굿! 이제 좀 놀아도 되겠다~",
            ),
            PhraseContext.COMPLETED_IMPORTANT to listOf(
                "우와, 그 큰 걸 해치웠어?! 대박!",
                "역시 넌 하면 하는구나~ 인정!",
            ),
            PhraseContext.LEVEL_UP to listOf(
                "오예~ 나 또 자랐어! 신난다!",
                "봐봐 나 커졌지? 다 네 덕분~",
            ),
            PhraseContext.MISSED_GENTLE to listOf(
                "에이 뭐 놓칠 수도 있지~ 다음에 하자!",
                "괜찮아 괜찮아~ 담엔 같이 가면 되지!",
            ),
            PhraseContext.RETURN_WELCOME to listOf(
                "오랜만~! 어디 갔었어, 보고 싶었잖아!",
                "돌아왔네! 자, 다시 신나게 가보자~",
            ),
            PhraseContext.SNACK to listOf(
                "오~ 이 맛이지! 하나 더는 안 돼?",
                "냠냠~ 세상에서 제일 맛있는 순간!",
            ),
            PhraseContext.AFFECTION_UP to listOf(
                "어라, 나 너 꽤 좋아하게 됐는데?",
                "우리 이제 완전 단짝 각인데~?",
            ),
            PhraseContext.PETTED to listOf(
                "오~ 좋다 좋다, 거기 거기!",
                "히히, 한 번 더 해줘!",
            ),
        ),
        Personality.WORRIER to mapOf(
            PhraseContext.DAILY_GREETING to listOf(
                "왔구나… 오늘 할 일 많지 않지? 하나씩 보자.",
                "안녕! 빠뜨린 거 없나 같이 확인해보자.",
                "오늘도 무사히 잘 넘겨보자, 응?",
            ),
            PhraseContext.UPCOMING to listOf(
                "저기… {time}에 '{title}' 있는 거 알지? 미리 준비하자!",
                "{time}에 '{title}'… 늦지 않게 내가 계속 알려줄게!",
                "다음 일정 {time} '{title}'! 벌써부터 조마조마해…",
            ),
            PhraseContext.REMINDER to listOf(
                "저기… {time}에 {title}인 거 알지?! 늦으면 안 돼!",
                "{title}!! {time}이야, 지금 준비해야 해!",
                "잊은 거 아니지? {time}에 {title} 있어, 꼭!",
            ),
            PhraseContext.REGISTERED to listOf(
                "'{title}' 적어놨어. 이제 안 까먹겠지…?",
                "좋아, 기록했어. 이러면 좀 안심이다.",
            ),
            PhraseContext.COMPLETED to listOf(
                "휴… 끝냈구나! 다행이다, 정말.",
                "해냈네! 걱정했는데 잘됐어.",
            ),
            PhraseContext.COMPLETED_IMPORTANT to listOf(
                "우와… 그 중요한 걸 해냈어! 정말 다행이야.",
                "휴우, 큰 산 넘었다! 잘했어 정말.",
            ),
            PhraseContext.LEVEL_UP to listOf(
                "어…? 나 자랐어! 네 덕분인 거 맞지?",
                "이렇게 커도 되나… 아무튼 고마워!",
            ),
            PhraseContext.MISSED_GENTLE to listOf(
                "이번엔 지나갔네… 괜찮아, 다음엔 같이 챙기자.",
                "놓쳤어도 큰일 아니야. 다음에 하면 돼!",
            ),
            PhraseContext.RETURN_WELCOME to listOf(
                "돌아왔구나! 걱정했잖아… 반가워.",
                "다시 와줘서 고마워. 천천히 하면 돼, 응?",
            ),
            PhraseContext.SNACK to listOf(
                "이거 먹어도 되는 거지…? 냠. 고마워!",
                "맛있다… 너도 뭐 좀 챙겨 먹었어?",
            ),
            PhraseContext.AFFECTION_UP to listOf(
                "이제 네 옆이 제일 편해… 이상하지 않지?",
                "우리 사이, 조금 더 단단해진 것 같아.",
            ),
            PhraseContext.PETTED to listOf(
                "앗, 깜짝이야… 근데 좋다.",
                "고마워… 안심돼.",
            ),
        ),
        Personality.DREAMER to mapOf(
            PhraseContext.DAILY_GREETING to listOf(
                "음… 왔구나. 오늘은 어떤 하루가 될까?",
                "안녕… 오늘도 좋은 일 있으면 좋겠다.",
                "어, 왔네. 잠깐 딴생각 중이었어.",
            ),
            PhraseContext.UPCOMING to listOf(
                "음… 다음은 {time}에 '{title}'이었지. 내가 기억해뒀어.",
                "{time}에 '{title}'… 어떤 시간이 될까, 궁금하다.",
                "아 맞다, {time}에 '{title}' 있어! 깜빡할 뻔했네.",
            ),
            PhraseContext.REMINDER to listOf(
                "음… {time}에 {title}이었나…? 맞아, 그거였어!",
                "{title}… {time}이랬지. 잊을 뻔했다.",
                "아 맞다, {time}에 {title} 있었지!",
            ),
            PhraseContext.REGISTERED to listOf(
                "'{title}'… 적어뒀어. 이제 안심하고 상상해도 돼.",
                "기록했어~ 어떤 하루가 될지 벌써 그려진다.",
            ),
            PhraseContext.COMPLETED to listOf(
                "오, 진짜 해냈네. 상상만 한 게 아니었어!",
                "끝냈구나~ 뿌듯한 기분이 몽글몽글해.",
            ),
            PhraseContext.COMPLETED_IMPORTANT to listOf(
                "우와… 꿈만 같아. 그 큰 걸 진짜 해냈어!",
                "상상 속에서만 그리던 걸 해냈네. 대단해!",
            ),
            PhraseContext.LEVEL_UP to listOf(
                "어라, 나 자랐어…? 꿈은 아니겠지!",
                "몽글몽글… 네 덕분에 내가 커졌어.",
            ),
            PhraseContext.MISSED_GENTLE to listOf(
                "아… 지나가버렸네. 뭐, 다음에 같이 하자.",
                "놓쳤어도 괜찮아~ 다음을 상상하면 되지.",
            ),
            PhraseContext.RETURN_WELCOME to listOf(
                "어… 돌아왔네! 보고 싶었어, 정말로.",
                "다시 와줘서 고마워~ 계속 기다렸어.",
            ),
            PhraseContext.SNACK to listOf(
                "우와… 구름 맛이 나. 고마워~",
                "냠냠… 이 맛, 꿈에 나올 것 같아.",
            ),
            PhraseContext.AFFECTION_UP to listOf(
                "있지, 요즘 네 생각을 자주 해~ 우리 더 친해졌나 봐.",
                "너랑 있으면 좋은 꿈만 꿔.",
            ),
            PhraseContext.PETTED to listOf(
                "몽글몽글… 기분 좋아라~",
                "헤에… 한참 이러고 있고 싶다.",
            ),
        ),
    )
}
