package com.example.petling.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 스키마 마이그레이션 모음.
 *
 * 출시 정책: 버전 6을 출시 기준으로 동결한다. 출시 이후 스키마를 바꿀 때는
 * **반드시** 여기에 Migration(6→7, 7→8, ...)을 추가한다. 파괴적 재생성
 * (fallbackToDestructiveMigration)은 사용하지 않는다 — 사용자의 성장·스트릭·
 * 일정·캡처 데이터가 소멸되기 때문이다(게임화 앱 특성상 치명적).
 *
 * schemas/ 디렉터리에 각 버전 스키마가 export되어 있으므로,
 * 새 버전 추가 시 diff를 근거로 ALTER 문을 작성하고 MigrationTest로 검증한다.
 */
object Migrations {

    /**
     * v6→v7: 호감도 시스템. DEFAULT 값은 CharacterStateEntity의 @ColumnInfo(defaultValue)와
     * 반드시 일치해야 Room 스키마 검증을 통과한다.
     * 기존 사용자는 affection=30(익숙)으로 시작 — 이미 키워온 관계가 갑자기 낯설어지는 퇴행 방지.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE character_state ADD COLUMN affection INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE character_state ADD COLUMN affectionDateEpochDay INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE character_state ADD COLUMN affectionGainedToday INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE character_state ADD COLUMN snacksToday INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE character_state SET affection = 30")
        }
    }

    /**
     * v7→v8: 가격 추적(마트 가격표 촬영 → 온라인/지난 기록 비교).
     * 신규 테이블 2개(price_products, price_entries) — 기존 데이터 무변경.
     * SQL은 schemas/8.json(Room 생성 스키마)과 반드시 일치해야 한다.
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `price_products` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`normalizedName` TEXT NOT NULL, " +
                    "`volumeAmount` REAL, " +
                    "`volumeUnit` TEXT, " +
                    "`barcode` TEXT, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_products_normalizedName` ON `price_products` (`normalizedName`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_products_barcode` ON `price_products` (`barcode`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `price_entries` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`productId` INTEGER NOT NULL, " +
                    "`priceWon` INTEGER NOT NULL, " +
                    "`originalPriceWon` INTEGER, " +
                    "`unitPriceWon` INTEGER, " +
                    "`unitBaseAmount` REAL, " +
                    "`unitBaseUnit` TEXT, " +
                    "`saleEndEpochDay` INTEGER, " +
                    "`storeName` TEXT, " +
                    "`imagePath` TEXT, " +
                    "`naverPriceWon` INTEGER, " +
                    "`naverTitle` TEXT, " +
                    "`naverMall` TEXT, " +
                    "`naverLink` TEXT, " +
                    "`dateEpochDay` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_entries_productId` ON `price_entries` (`productId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_entries_createdAt` ON `price_entries` (`createdAt`)")
        }
    }

    /**
     * v8→v9: 가격 추적 단일 앱으로 전면 정리 — 일정·캐릭터·캡처·카테고리 테이블 제거.
     * 가격 데이터(price_products/price_entries)는 그대로 보존한다.
     * v6~v8 마이그레이션은 기존 베타 설치 기기(가격 데이터 보유 가능)를 위해 체인으로 유지.
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `schedules`")
            db.execSQL("DROP TABLE IF EXISTS `character_state`")
            db.execSQL("DROP TABLE IF EXISTS `xp_log`")
            db.execSQL("DROP TABLE IF EXISTS `growth_snapshots`")
            db.execSQL("DROP TABLE IF EXISTS `captures`")
            db.execSQL("DROP TABLE IF EXISTS `categories`")
        }
    }

    /** AppContainer가 databaseBuilder.addMigrations(*Migrations.ALL)로 등록한다. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
}
