package com.example.petling.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 한국소비자원 참가격 데이터의 로컬 사본(martmemo-data 게시본에서 받음).
 * 촬영 시 네트워크를 타지 않기 위해 전부 Room에 둔다. 사용자 데이터와 섞이지 않는 읽기 전용 테이블.
 */

/** 참가격 판매점(630개). [type]은 LM 대형마트 / SM SSM / DP 백화점 / CS 편의점. */
@Entity(tableName = "market_stores")
data class MarketStoreEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val type: String,
    /** 시도 코드(예: 020100000 서울). */
    val area: String?,
    val areaDetail: String?,
    val addr: String?,
)

/** 참가격 상품(604개). [name]은 "서울우유 흰우유(1L)"처럼 괄호 안에 규격이 들어 있다. */
@Entity(tableName = "market_products", indices = [Index("normalizedName")])
data class MarketProductEntity(
    @PrimaryKey val id: Long,
    val name: String,
    /** 괄호 규격을 뺀 정규화 이름(매칭용). */
    val normalizedName: String,
    /** 총 용량과 단위(G/ML/EA 등). 없으면 null. */
    val totalAmount: Double?,
    val totalUnit: String?,
    /** 소분류 코드. */
    val cls: String?,
)

/** 상품 × 조사일 × 업태별 중앙값(trend.json). type "ALL"은 전 업태. */
@Entity(
    tableName = "market_medians",
    primaryKeys = ["goodId", "day", "type"],
    indices = [Index("goodId")],
)
data class MarketMedianEntity(
    val goodId: Long,
    /** 조사일 YYYYMMDD. */
    val day: String,
    val type: String,
    val priceWon: Int,
)
