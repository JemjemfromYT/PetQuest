package com.example.petquest.data.repository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class PetSummary(
    val name       : String  = "",
    val species    : String  = "",
    val rarity     : String  = "",
    val bondLevel  : Int     = 1,
    val isVerified : Boolean = false,
    val photoUri   : String? = null,  // pet verified photo URL
    val virtue     : String  = ""     // virtue name for card flip back face
)

data class PublicProfile(
    val uid                 : String           = "",
    val trainerName         : String           = "",
    val level               : Int              = 1,
    val streak              : Int              = 0,
    val bondPoints          : Int              = 0,
    val petCount            : Int              = 0,
    val speciesCount        : Int              = 0,
    val pets                : List<PetSummary> = emptyList(),
    val unlockedBadgeTitles : List<String>     = emptyList(),
    val updatedAt           : Long             = 0L
)

class FirebaseRepository {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    suspend fun ensureSignedIn(): String {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Sign-in succeeded but uid is null")
    }

    fun getMyUid(): String? = auth.currentUser?.uid

    suspend fun pushProfile(profile: PublicProfile) {
        val uid = ensureSignedIn()
        db.collection("profiles")
            .document(uid)
            .set(profile.copy(uid = uid, updatedAt = System.currentTimeMillis()))
            .await()
    }

    suspend fun fetchProfile(uid: String): PublicProfile? {
        return try {
            db.collection("profiles")
                .document(uid)
                .get()
                .await()
                .toObject(PublicProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
