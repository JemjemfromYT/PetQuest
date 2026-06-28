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

    // In-memory cache: local URI -> Firebase Storage download URL.
    // Prevents re-uploading the same photo every time pushProfile is called.
    private val photoCache = mutableMapOf<String, String>()

    // Persistent URL cache keyed by "uid/petIndex" -> download URL.
    // Survives within the app session so we never lose an already-uploaded URL
    // even if the local content:// URI becomes inaccessible later.
    private val savedUrlCache = mutableMapOf<String, String>()

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

        // ── Step 1: Fetch whatever is already stored in Firestore ────────────
        // This is the key fix: if an upload fails later, we fall back to the
        // URL that's already saved rather than overwriting it with null.
        val existingPets: List<PetSummary> = try {
            db.collection("profiles")
                .document(uid)
                .get()
                .await()
                .toObject(PublicProfile::class.java)
                ?.pets
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        // Also populate savedUrlCache from what Firestore already has,
        // so future calls (within the same session) don't need a round-trip.
        existingPets.forEachIndexed { index, pet ->
            val url = pet.photoUri
            if (url != null && url.startsWith("https://")) {
                savedUrlCache["$uid/$index"] = url
            }
        }

        // ── Step 2: Upload any new local photos and build the updated list ───
        val petsWithWebUrls = profile.pets.mapIndexed { index, pet ->
            val uri      = pet.photoUri
            val cacheKey = "$uid/$index"

            when {
                // Already a Firebase Storage URL — nothing to do, just cache it
                uri != null && uri.startsWith("https://") -> {
                    savedUrlCache[cacheKey] = uri
                    pet
                }

                // Local Android URI (content:// or file://) — upload to Storage
                uri != null && uri != "admin_verified" -> {
                    val downloadUrl: String? = try {
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
                        // Upload failed (e.g. content:// URI expired between sessions).
                        // CRITICAL FIX: fall back to the URL we already have stored —
                        // either from the in-session cache or from Firestore.
                        // Do NOT overwrite with null.
                        savedUrlCache[cacheKey]
                    }

                    // Cache the URL we ended up with so future syncs don't re-upload
                    if (downloadUrl != null) savedUrlCache[cacheKey] = downloadUrl

                    pet.copy(photoUri = downloadUrl)
                }

                // "admin_verified" string or null — pet has no uploadable local photo.
                // CRITICAL FIX: if we previously uploaded a photo for this pet slot,
                // preserve that URL instead of saving null.
                else -> {
                    val preserved = savedUrlCache[cacheKey]
                    pet.copy(photoUri = preserved)
                }
            }
        }

        // ── Step 3: Write the merged profile to Firestore ───────────────────
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
