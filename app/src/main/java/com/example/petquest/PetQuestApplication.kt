// app/src/main/java/com/example/petquest/PetQuestApplication.kt
// HOW TO APPLY: Open this file → Ctrl+A → Delete → Paste this entire file
// CHANGE: Replaced FirebaseRepository with SupabaseRepository

package com.example.petquest

import android.app.Application
import com.example.petquest.data.local.AppDatabase
import com.example.petquest.data.repository.PetRepository
import com.example.petquest.data.repository.SupabaseRepository
import com.example.petquest.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PetQuestApplication : Application() {

    lateinit var petRepository            : PetRepository
    lateinit var userPreferencesRepository: UserPreferencesRepository

    // SupabaseRepository needs the app context to store the auth token in SharedPreferences.
    // "by lazy" means it is only created when first accessed, which is safe.
    val supabaseRepository by lazy { SupabaseRepository(applicationContext) }

    override fun onCreate() {
        super.onCreate()

        val db = AppDatabase.getDatabase(this)
        petRepository             = PetRepository(db.petQuestDao())
        userPreferencesRepository = UserPreferencesRepository(this)

        SoundManager.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            petRepository.seedAchievements()
        }
    }
}
