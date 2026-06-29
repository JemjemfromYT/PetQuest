package com.example.petquest.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.File

private const val TAG = "FirebaseRepo"

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

    // Application context — always valid because Firebase was initialised with it
    private val appContext get() = FirebaseApp.getInstance().applicationContext

    // Session upload cache: local URI string -> Firebase Storage download URL
    private val photoCache = mutableMapOf<String, String>()

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true only for real local Android URIs that can be opened as a stream.
     * Rejects "admin_verified", "ml_verified_TIMESTAMP", or anything without a scheme.
     */
    private fun isUploadableUri(uriString: String): Boolean =
        uriString.startsWith("file://") || uriString.startsWith("content://")

    /**
     * Open a local URI as an InputStream.
     * file://  — CameraX internal storage; opened directly via File.
     * content:// — MediaStore / gallery; opened via ContentResolver.
     */
    private fun openUriStream(uri: Uri): java.io.InputStream? {
        return if (uri.scheme == "file") {
            val path = uri.path ?: run {
                Log.w(TAG, "file:// URI has null path: $uri")
                return null
            }
            val file = File(path)
            if (file.exists()) {
                Log.d(TAG, "Opening file stream: $path (${file.length()} bytes)")
                file.inputStream()
            } else {
                Log.w(TAG, "File does not exist: $path")
                null
            }
        } else {
            Log.d(TAG, "Opening content stream: $uri")
            try {
                appContext.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                Log.e(TAG, "ContentResolver failed for $uri", e)
                null
            }
        }
    }

    /**
     * Upload one pet photo to Firebase Storage and return the https download URL.
     * Uses putStream() — works for file:// (CameraX) and content:// (gallery) URIs.
     */
    private suspend fun uploadPetPhoto(
        uid      : String,
        index    : Int,
        petName  : String,
        uriString: String
    ): String? {
        // Return cached URL if we've already uploaded this URI this session
        photoCache[uriString]?.let {
            Log.d(TAG, "Cache hit for ${petName}: $it")
            return it
        }

        val uri      = Uri.parse(uriString)
        val safeKey  = petName.lowercase().replace(Regex("[^a-z0-9]"), "_").take(40)
        val remotePath = "pet_photos/$uid/${safeKey}_$index.jpg"
        val storageRef = storage.reference.child(remotePath)

        val stream = openUriStream(uri) ?: run {
            Log.w(TAG, "Could not open stream for ${petName} ($uriString) — skipping upload")
            return null
        }

        return try {
            Log.d(TAG, "Uploading ${petName} photo -> gs://.../$remotePath")
            stream.use { s -> storageRef.putStream(s).await() }
            val downloadUrl = storageRef.downloadUrl.await().toString()
            photoCache[uriString] = downloadUrl
            Log.d(TAG, "Upload success for ${petName}: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Upload FAILED for ${petName} ($uriString)", e)
            null
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun ensureSignedIn(): String {
        if (auth.currentUser == null) {
            Log.d(TAG, "No current user — signing in anonymously")
            auth.signInAnonymously().await()
        }
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Sign-in succeeded but uid is null")
        Log.d(TAG, "Signed in as uid=$uid")
        return uid
    }

    fun getMyUid(): String? = auth.currentUser?.uid

    suspend fun pushProfile(profile: PublicProfile) {
        val uid = ensureSignedIn()
        Log.d(TAG, "pushProfile: uid=$uid, pets=${profile.pets.size}")

        // ── Fetch existing Firestore data so we can preserve previously-uploaded URLs ──
        val existingPets: List<PetSummary> = try {
            db.collection("profiles").document(uid).get().await()
                .toObject(PublicProfile::class.java)?.pets ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch existing profile (first push?): ${e.message}")
            emptyList()
        }

        // Build a quick map of index -> existing download URL
        val existingUrls = existingPets.mapIndexedNotNull { i, p ->
            val url = p.photoUri
            if (url != null && url.startsWith("https://")) i to url else null
        }.toMap()

        // ── Upload any new local photos ────────────────────────────────────────────
        val petsWithWebUrls = profile.pets.mapIndexed { index, pet ->
            val uriString = pet.photoUri

            when {
                // Already a Firebase Storage / web URL — keep as-is
                uriString != null && uriString.startsWith("https://") -> {
                    Log.d(TAG, "Pet[${pet.name}]: already a web URL, no upload needed")
                    pet
                }

                // Valid local URI (file:// or content://) — upload to Storage
                uriString != null && isUploadableUri(uriString) -> {
                    Log.d(TAG, "Pet[${pet.name}]: uploading local URI $uriString")
                    val downloadUrl = uploadPetPhoto(uid, index, pet.name, uriString)
                        ?: existingUrls[index]  // fall back to previously-uploaded URL
                    Log.d(TAG, "Pet[${pet.name}]: final photoUri = $downloadUrl")
                    pet.copy(photoUri = downloadUrl)
                }

                // "admin_verified", "ml_verified_...", null, or anything else —
                // not a real photo; preserve any previously-uploaded URL instead
                else -> {
                    val preserved = existingUrls[index]
                    Log.d(TAG, "Pet[${pet.name}]: no photo URI (got '$uriString'), preserving existing: $preserved")
                    pet.copy(photoUri = preserved)
                }
            }
        }

        // ── Write merged profile to Firestore ──────────────────────────────────────
        val final = profile.copy(
            uid       = uid,
            pets      = petsWithWebUrls,
            updatedAt = System.currentTimeMillis()
        )
        db.collection("profiles").document(uid).set(final).await()
        Log.d(TAG, "pushProfile: Firestore write OK for uid=$uid")
    }

    suspend fun fetchProfile(uid: String): PublicProfile? {
        return try {
            db.collection("profiles").document(uid).get().await()
                .toObject(PublicProfile::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "fetchProfile error for uid=$uid", e)
            null
        }
    }
}
