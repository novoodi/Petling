package com.example.petling.ui.navigation

import kotlinx.serialization.Serializable

/** type-safe 내비게이션 라우트. */

@Serializable object PriceRoute

@Serializable data class PriceProductRoute(val productId: Long)

@Serializable object SettingsRoute
