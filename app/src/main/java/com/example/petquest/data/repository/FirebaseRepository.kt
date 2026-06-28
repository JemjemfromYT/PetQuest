package com.example.petquest.data.repository

import android.net.Uri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

data class PetSummary(
    val name       : String  = "",
    val species    : String  = "",
    val rarity     : String  = "",
    val bondLevel  : Int     = 1,
    val isVerified : Boolean = false,
    val photoUri   : String? = null,
    val virtue     : String  = ""
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

    private val auth    = Firebase.auth
    private val db      = Firebase.firestore
    private val storage = Firebase.storage

    // In-memory cache: local URI -> Firebase Storage download URL
    // Prevents re-uploading the same photo every time pushProfile is called.
    private val photoCache = mutableMapOf<String, String>()

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

        // For each pet, upload local photo to Firebase Storage if not already a web URL.
        // This makes pet photos visible on the share.html web page.
        val petsWithWebUrls = profile.pets.mapIndexed { index, pet ->
            val uri = pet.photoUri
            when {
                // Already a web URL — nothing to do
                uri != null && uri.startsWith("https://") -> pet

                // Local Android URI (content:// or file://) — upload to Storage
                uri != null && uri != "admin_verified" -> {
                    val downloadUrl = try {
                        // Return cached URL if we already uploaded this URI this session
                        photoCache.getOrPut(uri) {
                            val safeKey = pet.name.lowercase()
                                .replace(Regex("[^a-z0-9]"), "_")
                                .take(40)
                            val storageRef = storage.reference
                                .child("pet_photos/$uid/${safeKey}_$index.jpg")
                            storageRef.putFile(Uri.parse(uri)).await()
                            storageRef.downloadUrl.await().toString()
                        }
                    } catch (_: Exception) {
                        null // If upload fails, show emoji fallback on web
                    }
                    pet.copy(photoUri = downloadUrl)
                }

                // "admin_verified" or null — no real photo to upload
                else -> pet.copy(photoUri = null)
            }
        }

        db.collection("profiles")
            .document(uid)
            .set(
                profile.copy(
                    uid       = uid,
                    pets      = petsWithWebUrls,
                    updatedAt = System.currentTimeMillis()
                )
            )
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
