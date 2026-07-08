package com.example.petling.data.local

import androidx.room.migration.Migration

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
     * AppContainer가 databaseBuilder.addMigrations(*Migrations.ALL)로 등록한다.
     * 출시 시점(v6)에는 비어 있다. 신규 설치는 v6 스키마로 바로 생성되므로 마이그레이션이 필요 없다.
     * 예) v7 추가 시:
     * val MIGRATION_6_7 = object : Migration(6, 7) {
     *     override fun migrate(db: SupportSQLiteDatabase) {
     *         db.execSQL("ALTER TABLE schedules ADD COLUMN note TEXT")
     *     }
     * }
     */
    val ALL: Array<Migration> = emptyArray()
}
