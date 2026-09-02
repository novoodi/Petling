package com.example.petling.di

import android.content.Context
import androidx.room.Room
import com.example.petling.BuildConfig
import com.example.petling.data.backup.BackupRepository
import com.example.petling.data.capture.ImageStore
import com.example.petling.data.capture.OcrTextExtractor
import com.example.petling.data.local.Migrations
import com.example.petling.data.local.PetlingDatabase
import com.example.petling.data.price.GeminiNanoPriceTagExtractor
import com.example.petling.data.repository.PriceRepository
import com.example.petling.domain.AppClock
import com.example.petling.domain.SystemAppClock

/**
 * 수동 DI 컨테이너. 앱 = 마트 가격 추적(가격표 촬영→온디바이스 인식→기록·비교).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val clock: AppClock = SystemAppClock()

    val database: PetlingDatabase = Room.databaseBuilder(
        appContext,
        PetlingDatabase::class.java,
        PetlingDatabase.NAME,
    )
        // 파괴적 재생성 금지(가격 이력 보존). 스키마 변경 시 Migrations에 명시적 마이그레이션 추가.
        .addMigrations(*Migrations.ALL)
        .build()

    val ocrTextExtractor = OcrTextExtractor(appContext)

    val imageStore = ImageStore(appContext)

    /** 가격표 추출: 숫자는 규칙(OCR), Nano는 상품명·후보 선택만(미지원 기기는 규칙만). */
    val priceTagExtractor = GeminiNanoPriceTagExtractor()

    val priceRepository = PriceRepository(
        priceDao = database.priceDao(),
        imageStore = imageStore,
        ocr = ocrTextExtractor,
        extractor = priceTagExtractor,
        clock = clock,
    )

    /** JSON 백업 파일 내보내기/가져오기(사진 제외). 기기 교체 시 유일한 복구 경로. */
    val backupRepository = BackupRepository(
        context = appContext,
        database = database,
        priceDao = database.priceDao(),
        clock = clock,
        appVersion = BuildConfig.VERSION_NAME,
    )
}
