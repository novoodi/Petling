package com.example.petling.di

import android.content.Context
import androidx.room.Room
import com.example.petling.data.capture.GeminiNanoCaptureClassifier
import com.example.petling.data.capture.ImageStore
import com.example.petling.data.capture.OcrTextExtractor
import com.example.petling.data.parsing.GeminiNanoScheduleParser
import com.example.petling.data.datastore.SettingsDataStore
import com.example.petling.data.local.Migrations
import com.example.petling.data.local.PetlingDatabase
import com.example.petling.data.repository.CaptureRepository
import com.example.petling.data.repository.CategoryRepository
import com.example.petling.data.repository.CharacterRepository
import com.example.petling.data.repository.ScheduleRepository
import com.example.petling.domain.AppClock
import com.example.petling.domain.SystemAppClock
import com.example.petling.domain.capture.CaptureClassifier
import com.example.petling.domain.capture.CompositeCaptureClassifier
import com.example.petling.domain.capture.RuleBasedCaptureClassifier
import com.example.petling.domain.model.ScheduleSource
import com.example.petling.domain.parsing.CompositeScheduleParser
import com.example.petling.domain.parsing.RuleBasedScheduleParser
import com.example.petling.domain.parsing.ScheduleParser
import com.example.petling.notifications.AlarmScheduler
import com.example.petling.notifications.ScheduleAlarmScheduler

/**
 * 수동 DI 컨테이너. Hilt 도입 전까지 앱 전역 의존성을 조립한다.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val clock: AppClock = SystemAppClock()

    val database: PetlingDatabase = Room.databaseBuilder(
        appContext,
        PetlingDatabase::class.java,
        PetlingDatabase.NAME,
    )
        // 출시 정책: 파괴적 재생성 금지(사용자 데이터 보존). 스키마 변경 시 Migrations에 명시적 마이그레이션 추가.
        .addMigrations(*Migrations.ALL)
        .build()

    val settings = SettingsDataStore(appContext)

    val alarmScheduler: AlarmScheduler = ScheduleAlarmScheduler(appContext, database, clock)

    val characterRepository = CharacterRepository(database, clock)

    val scheduleRepository = ScheduleRepository(
        scheduleDao = database.scheduleDao(),
        characterRepository = characterRepository,
        alarmScheduler = alarmScheduler,
        clock = clock,
    )

    val ocrTextExtractor = OcrTextExtractor(appContext)

    /**
     * 일정 파서: Gemini Nano(지원 기기) → 규칙 파서(전 기기) 순으로 조합한다.
     * Nano는 미지원/실패/빈 결과 시 빈 리스트를 반환하므로 자동으로 규칙 파서로 폴백된다.
     */
    val captureParser: ScheduleParser = CompositeScheduleParser(
        primary = GeminiNanoScheduleParser(clock, ScheduleSource.CAPTURE),
        fallback = RuleBasedScheduleParser(clock, ScheduleSource.CAPTURE),
    )

    val voiceParser: ScheduleParser = CompositeScheduleParser(
        primary = GeminiNanoScheduleParser(clock, ScheduleSource.VOICE),
        fallback = RuleBasedScheduleParser(clock, ScheduleSource.VOICE),
    )

    val textParser: ScheduleParser = CompositeScheduleParser(
        primary = GeminiNanoScheduleParser(clock, ScheduleSource.TEXT),
        fallback = RuleBasedScheduleParser(clock, ScheduleSource.TEXT),
    )

    val imageStore = ImageStore(appContext)

    /** Gemini Nano 분류기(지원 기기에서만). 설정의 AI 상태 표시에도 사용. */
    val captureNano = GeminiNanoCaptureClassifier()

    /** 캡처 분류기: Nano 우선, 미지원/실패 시 규칙 분류기로 폴백. */
    val captureClassifier: CaptureClassifier = CompositeCaptureClassifier(
        primary = captureNano,
        fallback = RuleBasedCaptureClassifier(),
    )

    /** 사용자 맞춤 분류 카테고리. 앱 시작 시 seedIfEmpty()로 카탈로그 시드. */
    val categoryRepository = CategoryRepository(database.categoryDao())

    val captureRepository = CaptureRepository(
        captureDao = database.captureDao(),
        imageStore = imageStore,
        ocr = ocrTextExtractor,
        parser = captureParser,
        classifier = captureClassifier,
        summarizer = captureNano,
        categoryRepository = categoryRepository,
        characterRepository = characterRepository,
        scheduleRepository = scheduleRepository,
        clock = clock,
    )
}
