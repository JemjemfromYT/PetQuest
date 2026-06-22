package com.example.petquest.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val STREAK = intPreferencesKey("streak")
        val USER_LEVEL = intPreferencesKey("user_level")
        val TOTAL_BOND_POINTS = intPreferencesKey("total_bond_points")
        val LAST_STREAK_DATE = longPreferencesKey("last_streak_date")
    }

    val userStreak: Flow<Int> = context.dataStore.data.map { it[Keys.STREAK] ?: 0 }
    val userLevel: Flow<Int> = context.dataStore.data.map { it[Keys.USER_LEVEL] ?: 1 }
    val totalBondPoints: Flow<Int> = context.dataStore.data.map { it[Keys.TOTAL_BOND_POINTS] ?: 0 }
    val lastStreakDate: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_STREAK_DATE] ?: 0L }

    suspend fun updateStreak(v: Int) = context.dataStore.edit { it[Keys.STREAK] = v }
    suspend fun updateLastStreakDate(v: Long) = context.dataStore.edit { it[Keys.LAST_STREAK_DATE] = v }
    suspend fun addBondPoints(points: Int) = context.dataStore.edit { prefs ->
        val total = (prefs[Keys.TOTAL_BOND_POINTS] ?: 0) + points
        prefs[Keys.TOTAL_BOND_POINTS] = total
        prefs[Keys.USER_LEVEL] = (total / 100) + 1
    }
}