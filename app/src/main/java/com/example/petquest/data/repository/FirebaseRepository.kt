package com.example.petquest.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

private const val TAG = "FirebaseRepo"

// Maximum thumbnail dimension in pixels (maintains aspect ratio)
private const val THUMB_MAX_PX = 256
// JPEG quality 0-100
private const val THUMB_QUALITY = 65

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

    private val auth = Firebase.auth
    private val db   = Firebase.firestore
    // NOTE: Firebase Storage intentionally NOT used — requires paid Blaze plan.
    // Pet photos are compressed to thumbnails and stored as data URIs in Firestore.

    private val appContext get() = FirebaseApp.getInstance().applicationContext

    // Session cache: local URI string → data URI (avoids re-compressing same photo)
    private val thumbCache = mutableMapOf<String, String>()

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Open a local Android URI as a stream (file:// and content:// both handled). */
    private fun openStream(uri: Uri): InputStream? {
        return if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            if (file.exists()) file.inputStream()
            else { Log.w(TAG, "File not found: $path"); null }
        } else {
            try { appContext.contentResolver.openInputStream(uri) }
            catch (e: Exception) { Log.e(TAG, "ContentResolver failed: $uri", e); null }
        }
    }

    /**
     * Compress a local photo to a 256×256 JPEG thumbnail and return it as a
     * data URI string ("data:image/jpeg;base64,...").
     * This can be stored directly in Firestore and displayed by AsyncImage / <img>.
     * Free-plan safe — no Firebase Storage required.
     */
    private fun compressToDataUri(stream: InputStream): String? {
        return try {
            val bitmap = BitmapFactory.decodeStream(stream) ?: return null
            val w = bitmap.width
            val h = bitmap.height
            val scale = THUMB_MAX_PX.toFloat() / maxOf(w, h)
            val sw = (w * scale).toInt().coerceAtLeast(1)
            val sh = (h * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, true)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, baos)
            val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        } catch (e: Exception) {
            Log.e(TAG, "compressToDataUri failed", e)
            null
        }
    }

    /** Returns true only for real local Android URIs (file:// or content://). */
    private fun isLocalUri(s: String) =
        s.startsWith("file://") || s.startsWith("content://")

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun ensureSignedIn(): String {
        if (auth.currentUser == null) {
            Log.d(TAG, "Signing in anonymously…")
            auth.signInAnonymously().await()
        }
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Sign-in succeeded but uid is null")
    }

    fun getMyUid(): String? = auth.currentUser?.uid

    suspend fun pushProfile(profile: PublicProfile) {
        val uid = ensureSignedIn()
        Log.d(TAG, "pushProfile uid=$uid pets=${profile.pets.size}")

        // Fetch existing Firestore data so we can preserve already-stored thumbnails
        val existingPets: List<PetSummary> = try {
            db.collection("profiles").document(uid).get().await()
                .toObject(PublicProfile::class.java)?.pets ?: emptyList()
        } catch (_: Exception) { emptyList() }

        val existingDataUris: Map<Int, String> = existingPets.mapIndexedNotNull { i, p ->
            val uri = p.photoUri
            if (uri != null && (uri.startsWith("data:") || uri.startsWith("https://"))) i to uri
            else null
        }.toMap()

        val petsWithThumbs = profile.pets.mapIndexed { index, pet ->
            val uriString = pet.photoUri
            when {
                // Already a data URI or web URL — nothing to do
                uriString != null && (uriString.startsWith("data:") ||
                        uriString.startsWith("https://")) -> {
                    Log.d(TAG, "Pet[${pet.name}]: already has thumbnail/URL")
                    pet
                }

                // Local file (file:// or content://) — compress to thumbnail
                uriString != null && isLocalUri(uriString) -> {
                    val dataUri = thumbCache.getOrElse(uriString) {
                        Log.d(TAG, "Pet[${pet.name}]: compressing local photo…")
                        val uri    = Uri.parse(uriString)
                        val stream = openStream(uri)
                        val result = if (stream != null)
                            compressToDataUri(stream) else null
                        if (result != null) {
                            thumbCache[uriString] = result
                            Log.d(TAG, "Pet[${pet.name}]: thumbnail OK (${result.length} chars)")
                        } else {
                            Log.w(TAG, "Pet[${pet.name}]: compression failed, preserving existing")
                        }
                        result ?: existingDataUris[index] ?: ""
                    }.ifEmpty { null }

                    pet.copy(photoUri = dataUri ?: existingDataUris[index])
                }

                // null / "admin_verified" / "ml_verified_…" — preserve existing thumb
                else -> {
                    val preserved = existingDataUris[index]
                    Log.d(TAG, "Pet[${pet.name}]: no local photo, preserving: ${preserved?.take(30)}")
                    pet.copy(photoUri = preserved)
                }
            }
        }

        db.collection("profiles")
            .document(uid)
            .set(profile.copy(uid = uid, pets = petsWithThumbs, updatedAt = System.currentTimeMillis()))
            .await()
        Log.d(TAG, "pushProfile: Firestore write OK for uid=$uid")
    }

    suspend fun fetchProfile(uid: String): PublicProfile? {
        return try {
            db.collection("profiles").document(uid).get().await()
                .toObject(PublicProfile::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "fetchProfile error uid=$uid", e)
            null
        }
    }
}
