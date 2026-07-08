package com.example.petling.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "petling_settings")

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val notificationsEnabled: Boolean = true,
    /** 기본 알림 오프셋(분). null=알림 없음. */
    val defaultReminderOffsetMin: Int? = 10,
    /** 스크린샷 자동 감지(옵트인, 기본 off). */
    val screenshotWatchEnabled: Boolean = false,
)

/** 앱 설정 저장소. 온보딩 완료 여부와 알림 기본값을 관리한다. */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_REMINDER_OFFSET = intPreferencesKey("default_reminder_offset_min")
        val SCREENSHOT_WATCH = booleanPreferencesKey("screenshot_watch_enabled")
        val LAST_SEEN_SCREENSHOT_ID = longPreferencesKey("last_seen_screenshot_id")
        const val NO_REMINDER_SENTINEL = -1
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val offsetRaw = prefs[Keys.DEFAULT_REMINDER_OFFSET] ?: 10
        AppSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            defaultReminderOffsetMin = offsetRaw.takeIf { it != Keys.NO_REMINDER_SENTINEL },
            screenshotWatchEnabled = prefs[Keys.SCREENSHOT_WATCH] ?: false,
        )
    }

    suspend fun setScreenshotWatchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SCREENSHOT_WATCH] = enabled }
    }

    suspend fun isScreenshotWatchEnabled(): Boolean =
        settings.first().screenshotWatchEnabled

    suspend fun getLastSeenScreenshotId(): Long =
        context.dataStore.data.first()[Keys.LAST_SEEN_SCREENSHOT_ID] ?: 0L

    suspend fun setLastSeenScreenshotId(id: Long) {
        context.dataStore.edit { it[Keys.LAST_SEEN_SCREENSHOT_ID] = id }
    }

    suspend fun setOnboardingCompleted(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun isNotificationsEnabled(): Boolean =
        settings.first().notificationsEnabled

    suspend fun setDefaultReminderOffset(offsetMin: Int?) {
        context.dataStore.edit {
            it[Keys.DEFAULT_REMINDER_OFFSET] = offsetMin ?: Keys.NO_REMINDER_SENTINEL
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
