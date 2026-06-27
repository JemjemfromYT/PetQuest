// ============================================================
// FILE: app/src/main/java/com/example/petquest/PetQuestApplication.kt
// FULL REPLACEMENT — adds SoundManager.init()
// ============================================================

package com.example.petquest

import android.app.Application
import com.example.petquest.data.local.AppDatabase
import com.example.petquest.data.repository.PetRepository
import com.example.petquest.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PetQuestApplication : Application() {

    lateinit var petRepository            : PetRepository
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()

        val db = AppDatabase.getDatabase(this)
        petRepository             = PetRepository(db.petQuestDao())
        userPreferencesRepository = UserPreferencesRepository(this)

        // Initialise sound system (loads SFX into SoundPool; no files = no crash)
        SoundManager.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            petRepository.seedAchievements()
        }
    }
}
