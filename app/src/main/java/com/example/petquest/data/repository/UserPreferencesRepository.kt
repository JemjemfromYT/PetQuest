// ============================================================
// FILE: app/src/main/java/com/example/petquest/data/repository/UserPreferencesRepository.kt
//       (COMPLETE FILE — replace fully)
// CHANGES:
//   1. Added NOTIFICATIONS_ENABLED_KEY, NOTIFICATION_HOUR_KEY, NOTIFICATION_MINUTE_KEY
//   2. Added notificationsEnabled, notificationHour, notificationMinute flows
//   3. Added setNotificationsEnabled() and setReminderTime() suspend functions
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
        // ── Existing keys ──────────────────────────────────────────────────
        val STREAK_KEY           = intPreferencesKey("streak")
        val LAST_STREAK_DATE_KEY = stringPreferencesKey("last_streak_date")
        val HAS_ONBOARDED_KEY    = booleanPreferencesKey("has_onboarded")
        val LAST_TASK_DATE_KEY   = stringPreferencesKey("last_task_date")

        // ── V1.5 Notification keys ─────────────────────────────────────────
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR_KEY     = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE_KEY   = intPreferencesKey("notification_minute")
    }

    // ── Existing flows ─────────────────────────────────────────────────────
    val userStreak: Flow<Int>     = dataStore.data.map { it[STREAK_KEY] ?: 0 }
    val lastStreakDate: Flow<String> = dataStore.data.map { it[LAST_STREAK_DATE_KEY] ?: "" }
    val hasOnboarded: Flow<Boolean>  = dataStore.data.map { it[HAS_ONBOARDED_KEY] ?: false }
    val lastTaskDate: Flow<String>   = dataStore.data.map { it[LAST_TASK_DATE_KEY] ?: "" }

    // ── V1.5 Notification flows ────────────────────────────────────────────
    /** Whether the user has enabled daily reminders. Default: false. */
    val notificationsEnabled: Flow<Boolean> =
        dataStore.data.map { it[NOTIFICATIONS_ENABLED_KEY] ?: false }

    /** Hour component of the reminder time (24-hour). Default: 8 (8 AM). */
    val notificationHour: Flow<Int> =
        dataStore.data.map { it[NOTIFICATION_HOUR_KEY] ?: 8 }

    /** Minute component of the reminder time. Default: 0. */
    val notificationMinute: Flow<Int> =
        dataStore.data.map { it[NOTIFICATION_MINUTE_KEY] ?: 0 }

    // ── Existing suspend functions ─────────────────────────────────────────
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

    // ── V1.5 Notification suspend functions ───────────────────────────────
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED_KEY] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[NOTIFICATION_HOUR_KEY]   = hour
            it[NOTIFICATION_MINUTE_KEY] = minute
        }
    }
}
