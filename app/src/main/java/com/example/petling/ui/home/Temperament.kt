package com.example.petling.ui.home

import com.example.petling.domain.engine.AffectionLevel
import com.example.petling.domain.engine.AffectionRules
import com.example.petling.domain.model.Mood
import com.example.petling.domain.model.Personality
import com.example.petling.domain.model.Species

/**
 * 마당 캐릭터의 행동 상태. 렌더 스펙(애니메이션/표정/기분) 오버라이드를 결정한다.
 * (순수 Kotlin — Compose 의존 없음, 단위 테스트 가능)
 */
enum class YardBehavior {
    PAUSE, WALK, HOP, SLEEP, REACT, DRAG, FALL, CELEBRATE, GREET, PEEK, GROOM, BINKY, PECK, EAT,
    WALK_TO_PERCH, // perch로 이동(정면 홉)
    SIT,           // perch 위 착지·체류
    DISMOUNT,      // perch에서 바닥으로 뛰어내림
}

/** 이동 스타일(옆모습 보행의 속도·연출 차이). */
enum class WalkStyle { STROLL, TROT, SCURRY, HOP, ROLL }

/** 주인이 앱에 들어왔을 때의 인사 방식. */
enum class GreetStyle { RUSH, ALOOF_OR_RUB, PEEK_IN, BINKY_IN, ROLL_IN, WADDLE_IN, SCURRY_IN, SLOW_APPROACH }

/** 탭(쓰다듬기) 반응. 호감도 게이트에 따라 달라진다. */
enum class ReactStyle { COOL, STARTLE_DASH, STARTLE_HIDE, NORMAL, LOVING }

/**
 * 종별 기질 프로필. UI에 기질명을 표기하지 않는다 — 사용자가 키우면서 행동으로 발견하는 요소.
 * 앵김(강아지·병아리)/도도(고양이)/수줍음(여우)/겁많지만 활발(토끼)/부지런(햄스터)/
 * 천진 뒤뚱(펭귄)/느긋(판다)/천진(도토리).
 */
data class TemperamentProfile(
    val walkStyle: WalkStyle,
    val greetStyle: GreetStyle,
    val pauseMs: LongRange,
    /** 종 특화 idle 습성(그루밍/빼꼼/빙키/쪼기 등). null이면 없음. */
    val signature: YardBehavior?,
    val signatureWeight: Int,
)

fun temperamentFor(species: Species): TemperamentProfile = when (species) {
    Species.DOG -> TemperamentProfile(WalkStyle.TROT, GreetStyle.RUSH, 900L..2000L, YardBehavior.HOP, 12)
    Species.CHICK -> TemperamentProfile(WalkStyle.SCURRY, GreetStyle.RUSH, 900L..1800L, YardBehavior.PECK, 25)
    Species.CAT -> TemperamentProfile(WalkStyle.STROLL, GreetStyle.ALOOF_OR_RUB, 1400L..2600L, YardBehavior.GROOM, 28)
    Species.FOX -> TemperamentProfile(WalkStyle.TROT, GreetStyle.PEEK_IN, 1000L..2200L, YardBehavior.PEEK, 22)
    Species.RABBIT -> TemperamentProfile(WalkStyle.HOP, GreetStyle.BINKY_IN, 900L..2000L, YardBehavior.BINKY, 18)
    Species.HAMSTER -> TemperamentProfile(WalkStyle.SCURRY, GreetStyle.SCURRY_IN, 800L..1600L, YardBehavior.PECK, 20)
    Species.PENGUIN -> TemperamentProfile(WalkStyle.STROLL, GreetStyle.WADDLE_IN, 1000L..2200L, YardBehavior.HOP, 15)
    Species.PANDA -> TemperamentProfile(WalkStyle.STROLL, GreetStyle.SLOW_APPROACH, 1800L..3200L, YardBehavior.GROOM, 18)
    Species.ACORN -> TemperamentProfile(WalkStyle.ROLL, GreetStyle.ROLL_IN, 1000L..2200L, null, 0)
}

/** 이동 기본 속도(dp/s). mood 배율은 호출부에서. */
fun walkSpeedDp(style: WalkStyle): Float = when (style) {
    WalkStyle.STROLL -> 48f
    WalkStyle.TROT -> 75f
    WalkStyle.SCURRY -> 90f
    WalkStyle.HOP -> 70f
    WalkStyle.ROLL -> 65f
}

/** 탭 반응 게이트: 도도한 종은 친해져야 반겨주고, 겁 많은 종은 낯설면 도망간다. */
fun reactStyleFor(species: Species, level: AffectionLevel): ReactStyle = when (species) {
    Species.CAT -> when {
        level >= AffectionLevel.BONDED -> ReactStyle.LOVING
        level >= AffectionLevel.CLOSE -> ReactStyle.NORMAL
        else -> ReactStyle.COOL
    }
    Species.FOX -> when {
        level >= AffectionLevel.CLOSE -> ReactStyle.LOVING
        level >= AffectionLevel.FAMILIAR -> ReactStyle.NORMAL
        else -> ReactStyle.STARTLE_HIDE
    }
    Species.RABBIT -> when {
        level >= AffectionLevel.FAMILIAR -> ReactStyle.NORMAL
        else -> ReactStyle.STARTLE_DASH
    }
    Species.DOG, Species.CHICK -> if (level >= AffectionLevel.CLOSE) ReactStyle.LOVING else ReactStyle.NORMAL
    Species.PENGUIN -> if (level >= AffectionLevel.CLOSE) ReactStyle.LOVING else ReactStyle.NORMAL
    Species.HAMSTER, Species.PANDA, Species.ACORN -> ReactStyle.NORMAL
}

/**
 * idle 행동 가중치. 정적임 해소: 짧은 PAUSE, 습성 행동 상시 후보,
 * 같은 습성 연속 방지(직전이면 0), 기분 연동(HAPPY 홉↑·TIRED 수면).
 */
fun idleWeights(species: Species, last: YardBehavior, mood: Mood): List<Pair<YardBehavior, Int>> {
    val t = temperamentFor(species)
    val happy = mood == Mood.HAPPY
    val tired = mood == Mood.TIRED
    return buildList {
        when (last) {
            YardBehavior.WALK -> {
                add(YardBehavior.PAUSE to 55)
                add(YardBehavior.HOP to 15)
                add(YardBehavior.WALK to 10)
            }
            else -> {
                add(YardBehavior.WALK to 50)
                add(YardBehavior.HOP to if (happy) 22 else 12)
                add(YardBehavior.PAUSE to 10)
            }
        }
        t.signature?.let { sig ->
            if (last != sig) add(sig to t.signatureWeight)
        }
        if (tired) add(YardBehavior.SLEEP to if (species == Species.PANDA || species == Species.CAT) 30 else 25)
    }
}

/** 호감도 원값 → 단계(편의 위임). */
fun affectionLevel(affection: Int): AffectionLevel = AffectionRules.levelFor(affection)

/**
 * 성격별 perch 편향. 종 기질([TemperamentProfile])과 분리 — 기질=마당 행동 선택,
 * 이건 "perch에 얼마나 자주·오래 앉는가, 어떤 perch를 고르는가".
 */
data class PersonalityBias(
    val perchUrge: Float,        // idle 차례에 perch로 이동할 확률
    val sitDwellMs: LongRange,   // 착지 후 체류 시간
    val prefersWeight: Boolean,  // true=weight(마감 임박) 최대 perch 우선(걱정형)
)

fun personalityBiasFor(p: Personality?): PersonalityBias = when (p) {
    Personality.FREE_SPIRIT -> PersonalityBias(0.50f, 2500L..4500L, false) // 자주 갈아탐, 짧게
    Personality.DREAMER -> PersonalityBias(0.20f, 9000L..16000L, false)    // 드물게, 한 자리 오래
    Personality.WORRIER -> PersonalityBias(0.40f, 3000L..6000L, true)      // 마감 임박 perch 우선
    Personality.SINCERE, null -> PersonalityBias(0.32f, 5000L..8000L, false)
}
