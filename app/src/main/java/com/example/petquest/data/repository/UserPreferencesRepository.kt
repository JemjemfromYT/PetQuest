// ============================================================
// FILE: app/src/main/java/com/example/petquest/data/repository/UserPreferencesRepository.kt
// FULL REPLACEMENT — adds HAS_SEEN_TUTORIAL_KEY for the Whisker cat guide
// ============================================================

package com.example.petquest.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val STREAK_KEY                    = intPreferencesKey("streak")
        val LAST_STREAK_DATE_KEY          = stringPreferencesKey("last_streak_date")
        val HAS_ONBOARDED_KEY             = booleanPreferencesKey("has_onboarded")
        val LAST_TASK_DATE_KEY            = stringPreferencesKey("last_task_date")
        val NOTIFICATIONS_ENABLED_KEY     = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR_KEY         = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE_KEY       = intPreferencesKey("notification_minute")
        val TOTAL_TASKS_COMPLETED_KEY     = intPreferencesKey("total_tasks_completed")
        val HAS_SEEN_ONBOARDING_KEY       = booleanPreferencesKey("has_seen_post_verify_onboarding")
        val PERSONAL_BEST_STREAK_KEY      = intPreferencesKey("personal_best_streak")
        val LAST_EVENT_CLAIM_DATE_KEY     = stringPreferencesKey("last_event_claim_date")
        // ── NEW: Whisker cat tutorial flag ────────────────────────────────────
        val HAS_SEEN_TUTORIAL_KEY         = booleanPreferencesKey("has_seen_whisker_tutorial")
        val PROFILE_BANNER_KEY            = intPreferencesKey("profile_banner_index")
    }

    val userStreak: Flow<Int>            = dataStore.data.map { it[STREAK_KEY] ?: 0 }
    val lastStreakDate: Flow<String>     = dataStore.data.map { it[LAST_STREAK_DATE_KEY] ?: "" }
    val hasOnboarded: Flow<Boolean>      = dataStore.data.map { it[HAS_ONBOARDED_KEY] ?: false }
    val lastTaskDate: Flow<String>       = dataStore.data.map { it[LAST_TASK_DATE_KEY] ?: "" }
    val notificationsEnabled: Flow<Boolean> =
        dataStore.data.map { it[NOTIFICATIONS_ENABLED_KEY] ?: false }
    val notificationHour: Flow<Int>      = dataStore.data.map { it[NOTIFICATION_HOUR_KEY] ?: 8 }
    val notificationMinute: Flow<Int>    = dataStore.data.map { it[NOTIFICATION_MINUTE_KEY] ?: 0 }
    val totalTasksCompleted: Flow<Int>   = dataStore.data.map { it[TOTAL_TASKS_COMPLETED_KEY] ?: 0 }
    val hasSeenOnboarding: Flow<Boolean> =
        dataStore.data.map { it[HAS_SEEN_ONBOARDING_KEY] ?: false }
    val personalBestStreak: Flow<Int>    = dataStore.data.map { it[PERSONAL_BEST_STREAK_KEY] ?: 0 }
    val lastEventClaimDate: Flow<String> = dataStore.data.map { it[LAST_EVENT_CLAIM_DATE_KEY] ?: "" }
    // ── NEW ──────────────────────────────────────────────────────────────────
    val hasSeenTutorial: Flow<Boolean>   =
        dataStore.data.map { it[HAS_SEEN_TUTORIAL_KEY] ?: false }
    val profileBannerIndex: Flow<Int>     =
        dataStore.data.map { it[PROFILE_BANNER_KEY] ?: 1 }

    suspend fun updateStreak(streak: Int) {
        dataStore.edit { it[STREAK_KEY] = streak }
    }

    suspend fun updateLastStreakDate(date: String) {
        dataStore.edit { it[LAST_STREAK_DATE_KEY] = date }
    }

    suspend fun setOnboarded() {
        dataStore.edit { it[HAS_ONBOARDED_KEY] = true }
    }

    suspend fun updateLastTaskDate(date: String) {
        dataStore.edit { it[LAST_TASK_DATE_KEY] = date }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED_KEY] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[NOTIFICATION_HOUR_KEY]   = hour
            it[NOTIFICATION_MINUTE_KEY] = minute
        }
    }

    suspend fun incrementTasksCompleted(count: Int = 1) {
        dataStore.edit { prefs ->
            val current = prefs[TOTAL_TASKS_COMPLETED_KEY] ?: 0
            prefs[TOTAL_TASKS_COMPLETED_KEY] = current + count
        }
    }

    suspend fun markOnboardingSeen() {
        dataStore.edit { it[HAS_SEEN_ONBOARDING_KEY] = true }
    }

    suspend fun updatePersonalBestStreak(streak: Int) {
        dataStore.edit { it[PERSONAL_BEST_STREAK_KEY] = streak }
    }

    suspend fun updateLastEventClaimDate(date: String) {
        dataStore.edit { it[LAST_EVENT_CLAIM_DATE_KEY] = date }
    }

    // ── NEW ──────────────────────────────────────────────────────────────────
    suspend fun markTutorialSeen() {
        dataStore.edit { it[HAS_SEEN_TUTORIAL_KEY] = true }
    }

    suspend fun updateProfileBannerIndex(index: Int) {
        dataStore.edit { it[PROFILE_BANNER_KEY] = index }
    }
}
