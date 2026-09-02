package com.example.petling.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 가격 추적 대상 상품(재방문 매칭의 축). */
@Entity(
    tableName = "price_products",
    indices = [Index("normalizedName"), Index("barcode")],
)
data class PriceProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** 매칭용 정규화 이름(공백·기호 제거 소문자). */
    val normalizedName: String,
    val volumeAmount: Double? = null,
    val volumeUnit: String? = null,
    /** 가격표 바코드(13자리) — 있으면 최우선 매칭 키. */
    val barcode: String? = null,
    val createdAt: Long,
    /** 사용자가 확인한 참가격 상품 id(시장 비교용). null이면 매번 이름으로 매칭. */
    val marketGoodId: Long? = null,
)

/** 특정 날짜에 관측한 가격 한 건(사진 1장 = 1건). */
@Entity(
    tableName = "price_entries",
    indices = [Index("productId"), Index("createdAt")],
)
data class PriceEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    /** 최종 판매가(할인 반영). */
    val priceWon: Int,
    /** 할인 전 정가(표기된 경우). */
    val originalPriceWon: Int? = null,
    /** 가격표의 단위가(예: 100g당 936원 → 936). */
    val unitPriceWon: Int? = null,
    val unitBaseAmount: Double? = null,
    val unitBaseUnit: String? = null,
    /** 행사 종료일(epochDay). */
    val saleEndEpochDay: Long? = null,
    val storeName: String? = null,
    val imagePath: String? = null,
    /** 관측 당시 네이버 최저가 스냅샷(조회 성공 시). */
    val naverPriceWon: Int? = null,
    val naverTitle: String? = null,
    val naverMall: String? = null,
    val naverLink: String? = null,
    /** 관측 날짜(epochDay) — 같은 날 재촬영 판정용. */
    val dateEpochDay: Long,
    val createdAt: Long,
)
