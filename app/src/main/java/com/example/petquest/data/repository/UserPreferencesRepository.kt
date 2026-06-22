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
        val STREAK_KEY = intPreferencesKey("streak")
        val LAST_STREAK_DATE_KEY = stringPreferencesKey("last_streak_date")
        val HAS_ONBOARDED_KEY = booleanPreferencesKey("has_onboarded")
        val LAST_TASK_DATE_KEY = stringPreferencesKey("last_task_date")
    }

    val userStreak: Flow<Int> = dataStore.data.map { it[STREAK_KEY] ?: 0 }
    val lastStreakDate: Flow<String> = dataStore.data.map { it[LAST_STREAK_DATE_KEY] ?: "" }
    val hasOnboarded: Flow<Boolean> = dataStore.data.map { it[HAS_ONBOARDED_KEY] ?: false }
    val lastTaskDate: Flow<String> = dataStore.data.map { it[LAST_TASK_DATE_KEY] ?: "" }

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
}