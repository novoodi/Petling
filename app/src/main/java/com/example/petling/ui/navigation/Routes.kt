package com.example.petling.ui.navigation

import kotlinx.serialization.Serializable

/** type-safe 내비게이션 라우트. */

@Serializable object PriceRoute

@Serializable data class PriceProductRoute(val productId: Long)

/** 시세 탭: 참가격 상품 검색. */
@Serializable object MarketRoute

@Serializable data class MarketProductRoute(val goodId: Long)

@Serializable object SettingsRoute
