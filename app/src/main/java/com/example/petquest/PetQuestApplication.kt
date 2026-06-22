package com.example.petquest

import android.app.Application
import com.example.petquest.data.local.AppDatabase
import com.example.petquest.data.repository.PetRepository
import com.example.petquest.data.repository.UserPreferencesRepository

class PetQuestApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val petRepository by lazy { PetRepository(database.petQuestDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
}