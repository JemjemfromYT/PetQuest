package com.example.petquest.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.File

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

    // In-memory cache: local URI string -> Firebase Storage download URL.
    // Prevents re-uploading the same photo every time pushProfile is called.
    private val photoCache = mutableMapOf<String, String>()

    // Persistent URL cache keyed by "uid/petIndex" -> download URL.
    // Survives within the same app session so already-uploaded URLs are never lost.
    private val savedUrlCache = mutableMapOf<String, String>()

    suspend fun ensureSignedIn(): String {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Sign-in succeeded but uid is null")
    }

    fun getMyUid(): String? = auth.currentUser?.uid

    /**
     * Open a local URI as an InputStream.
     *
     * Handles both file:// URIs (internal app storage, e.g. from CameraX) and
     * content:// URIs (MediaStore / gallery picks).
     */
    private fun openUriStream(context: Context, uri: Uri): java.io.InputStream? {
        return if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            if (file.exists()) file.inputStream() else null
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }

    /**
     * Upload a single pet photo to Firebase Storage and return the download URL.
     * Uses putStream() so it works for both file:// and content:// URIs.
     */
    private suspend fun uploadPetPhoto(
        context  : Context,
        uid      : String,
        index    : Int,
        petName  : String,
        uriString: String
    ): String? {
        val uri = Uri.parse(uriString)
        val safeKey = petName.lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .take(40)
        val storageRef = storage.reference
            .child("pet_photos/$uid/${safeKey}_$index.jpg")

        val stream = openUriStream(context, uri) ?: return null
        return try {
            stream.use { s ->
                storageRef.putStream(s).await()
            }
            storageRef.downloadUrl.await().toString()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun pushProfile(profile: PublicProfile, context: Context) {
        val uid = ensureSignedIn()

        // ── Step 1: Fetch whatever is already saved in Firestore ────────────
        // This lets us preserve existing photo URLs if a new upload fails.
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

        // Populate savedUrlCache from Firestore so we don't re-upload unnecessarily.
        existingPets.forEachIndexed { index, pet ->
            val url = pet.photoUri
            if (url != null && url.startsWith("https://")) {
                savedUrlCache["$uid/$index"] = url
            }
        }

        // ── Step 2: Upload any new local photos ─────────────────────────────
        val petsWithWebUrls = profile.pets.mapIndexed { index, pet ->
            val uriString = pet.photoUri
            val cacheKey  = "$uid/$index"

            when {
                // Already a web URL — nothing to upload
                uriString != null && uriString.startsWith("https://") -> {
                    savedUrlCache[cacheKey] = uriString
                    pet
                }

                // Local file (file:// or content://) — upload to Firebase Storage
                uriString != null && uriString != "admin_verified" -> {
                    val downloadUrl: String? = photoCache.getOrElse(uriString) {
                        // Try to upload; returns null if the file is unreadable
                        val uploaded = uploadPetPhoto(context, uid, index, pet.name, uriString)
                        if (uploaded != null) {
                            photoCache[uriString] = uploaded
                        }
                        uploaded
                    } ?: savedUrlCache[cacheKey]  // fall back to previously saved URL

                    if (downloadUrl != null) savedUrlCache[cacheKey] = downloadUrl

                    pet.copy(photoUri = downloadUrl ?: existingPets.getOrNull(index)?.photoUri)
                }

                // null or "admin_verified" — preserve any previously uploaded photo
                else -> {
                    pet.copy(photoUri = savedUrlCache[cacheKey] ?: existingPets.getOrNull(index)?.photoUri)
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
        } catch (_: Exception) {
            null
        }
    }
}
