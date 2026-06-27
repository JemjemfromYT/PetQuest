package com.example.petquest

import android.app.Application
import com.example.petquest.data.local.AppDatabase
import com.example.petquest.data.repository.FirebaseRepository
import com.example.petquest.data.repository.PetRepository
import com.example.petquest.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PetQuestApplication : Application() {

    lateinit var petRepository            : PetRepository
    lateinit var userPreferencesRepository: UserPreferencesRepository

    // ── KEY FIX ──────────────────────────────────────────────────────────────
    // "by lazy" means FirebaseRepository() is NOT constructed here in the
    // class constructor. It is constructed the first time .firebaseRepository
    // is accessed — which happens inside Composables, well after onCreate()
    // has run and Firebase has auto-initialized via google-services.
    //
    // Without "lazy", Firebase.auth is called before Firebase exists → crash.
    // ─────────────────────────────────────────────────────────────────────────
    val firebaseRepository by lazy { FirebaseRepository() }

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
