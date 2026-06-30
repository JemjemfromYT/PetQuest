// NEW FILE: app/src/main/java/com/example/petquest/data/repository/SupabaseRepository.kt
// HOW TO APPLY:
//   1. In Android Studio, right-click the "repository" folder → New → Kotlin Class/File
//   2. Name it: SupabaseRepository
//   3. Delete everything in the new file, paste this entire content
//   4. Then DELETE FirebaseRepository.kt (right-click → Delete)
//
// IMPORTANT: Fill in YOUR Supabase URL and anon key below (see STEP1_SETUP_GUIDE.txt)

package com.example.petquest.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

private const val TAG = "SupabaseRepo"

// Photo compression settings — higher quality now that we have real storage
private const val THUMB_MAX_PX = 512
private const val THUMB_QUALITY = 80

// SharedPreferences keys for storing the auth token between app launches
private const val PREFS_NAME = "supabase_auth"
private const val KEY_ACCESS_TOKEN  = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_ID       = "user_id"

// ── FILL THESE IN ────────────────────────────────────────────────────────────
// Get these from: Supabase Dashboard → Settings → API
// The anon key is SAFE to include in the app — it's designed to be public.
private const val SUPABASE_URL      = "https://aktoowoflxvniphzgpvx.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFrdG9vd29mbHh2bmlwaHpncHZ4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI2NjU2NTQsImV4cCI6MjA5ODI0MTY1NH0.5YQ1E3F6aQK_gcBSGaQATherncEY4m1Diqio-sUwMMw"
// ─────────────────────────────────────────────────────────────────────────────

// ── Data classes shared across the app ───────────────────────────────────────
// These are the same as before — PetSummary and PublicProfile stay identical
// so nothing else in the app needs to change.

data class PetSummary(
    val name       : String  = "",
    val species    : String  = "",
    val rarity     : String  = "",
    val bondLevel  : Int     = 1,
    val isVerified : Boolean = false,
    val photoUri   : String? = null,   // now a real Supabase Storage URL like https://...
    val virtue     : String  = "",
    val petId      : Int     = 0        // local Room ID — used for stable upload filenames
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
    val updatedAt           : Long             = 0L,
    val bannerIndex         : Int              = 1
)

// ── Internal: Supabase database row (snake_case column names) ─────────────────
// Gson's @SerializedName maps our camelCase Kotlin fields to snake_case DB columns.
// The 'pets' and 'unlockedBadgeTitles' are stored as JSONB arrays in Supabase.

private data class ProfileRow(
    @SerializedName("uid")                    val uid                : String,
    @SerializedName("trainer_name")           val trainerName        : String,
    @SerializedName("level")                  val level              : Int,
    @SerializedName("streak")                 val streak             : Int,
    @SerializedName("bond_points")            val bondPoints         : Int,
    @SerializedName("pet_count")              val petCount           : Int,
    @SerializedName("species_count")          val speciesCount       : Int,
    @SerializedName("pets")                   val pets               : List<PetSummary>,
    @SerializedName("unlocked_badge_titles")  val unlockedBadgeTitles: List<String>,
    @SerializedName("updated_at")             val updatedAt          : Long,
    @SerializedName("banner_index")           val bannerIndex        : Int = 1
)

// ── Main repository class ─────────────────────────────────────────────────────

class SupabaseRepository(private val appContext: Context) {

    private val gson   = Gson()
    private val prefs  = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val http   = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)   // longer for photo uploads
        .build()

    // ── Stored auth ───────────────────────────────────────────────────────────

    private fun getStoredToken()  : String? = prefs.getString(KEY_ACCESS_TOKEN,  null)
    private fun getStoredRefresh(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    private fun getStoredUid()    : String? = prefs.getString(KEY_USER_ID,       null)

    fun getMyUid(): String? = getStoredUid()

    // ── Anonymous sign-in ─────────────────────────────────────────────────────
    // Supabase gives every device a stable anonymous UID for free.
    // The UID is saved in SharedPreferences so the user keeps the same ID
    // across app launches.

    suspend fun ensureSignedIn(): String {
        val existingUid   = getStoredUid()
        val existingToken = getStoredToken()
        if (existingUid != null && existingToken != null) {
            return existingUid
        }

        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Signing in anonymously with Supabase…")

            val body    = "{}".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/signup")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = http.newCall(request).execute()
            val rawBody  = response.body?.string() ?: throw Exception("Empty auth response")

            if (!response.isSuccessful) {
                throw Exception("Supabase sign-in failed (${response.code}): $rawBody")
            }

            @Suppress("UNCHECKED_CAST")
            val json         = gson.fromJson(rawBody, Map::class.java) as Map<String, Any?>
            val accessToken  = json["access_token"]  as? String ?: throw Exception("No access_token in auth response")
            val refreshToken = json["refresh_token"] as? String
            @Suppress("UNCHECKED_CAST")
            val userMap = json["user"] as? Map<String, Any?> ?: throw Exception("No user in auth response")
            val userId  = userMap["id"] as? String ?: throw Exception("No id in user object")

            prefs.edit()
                .putString(KEY_ACCESS_TOKEN,  accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID,       userId)
                .apply()

            Log.d(TAG, "Signed in. uid=$userId")
            userId
        }
    }

    // ── Token refresh (called automatically on 401) ───────────────────────────

    private suspend fun refreshToken(): Boolean {
        val refreshToken = getStoredRefresh() ?: return false
        return withContext(Dispatchers.IO) {
            try {
                val bodyJson = gson.toJson(mapOf("refresh_token" to refreshToken))
                val body     = bodyJson.toRequestBody("application/json".toMediaType())
                val request  = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/token?grant_type=refresh_token")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = http.newCall(request).execute()
                val rawBody  = response.body?.string() ?: return@withContext false

                if (!response.isSuccessful) return@withContext false

                @Suppress("UNCHECKED_CAST")
                val json        = gson.fromJson(rawBody, Map::class.java) as Map<String, Any?>
                val newAccess   = json["access_token"]  as? String ?: return@withContext false
                val newRefresh  = json["refresh_token"] as? String

                prefs.edit()
                    .putString(KEY_ACCESS_TOKEN,  newAccess)
                    .putString(KEY_REFRESH_TOKEN, newRefresh ?: getStoredRefresh())
                    .apply()

                Log.d(TAG, "Token refreshed OK")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh failed", e)
                false
            }
        }
    }

    // ── Photo helpers ─────────────────────────────────────────────────────────

    private fun openStream(uri: Uri): InputStream? {
        return if (uri.scheme == "file") {
            val file = File(uri.path ?: return null)
            if (file.exists()) file.inputStream()
            else { Log.w(TAG, "File not found: ${uri.path}"); null }
        } else {
            try { appContext.contentResolver.openInputStream(uri) }
            catch (e: Exception) { Log.e(TAG, "ContentResolver failed for $uri", e); null }
        }
    }

    // Compress a photo to a JPEG byte array (512px max dimension, 80% quality)
    private fun compressPhoto(stream: InputStream): ByteArray? {
        return try {
            val bitmap = BitmapFactory.decodeStream(stream) ?: return null
            val w      = bitmap.width
            val h      = bitmap.height
            val scale  = THUMB_MAX_PX.toFloat() / maxOf(w, h)
            val sw     = (w * scale).toInt().coerceAtLeast(1)
            val sh     = (h * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, true)
            val baos   = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Photo compression failed", e)
            null
        }
    }

    // Upload a JPEG byte array to Supabase Storage and return the public URL.
    // Uses a stable filename based on petId so re-uploads overwrite instead of creating duplicates.
    private suspend fun uploadPhotoToStorage(uid: String, petId: Int, petName: String, photoBytes: ByteArray): String? {
        return withContext(Dispatchers.IO) {
            try {
                val token    = getStoredToken() ?: return@withContext null
                val safeName = petName.replace(Regex("[^a-zA-Z0-9_]"), "_")
                // Stable filename: no timestamp → same pet always maps to the same file.
                // x-upsert:true tells Supabase to overwrite if the file already exists.
                val fileName = if (petId > 0) "${petId}_${safeName}.jpg" else "${safeName}.jpg"
                val path     = "$uid/$fileName"

                val body    = photoBytes.toRequestBody("image/jpeg".toMediaType())
                val request = Request.Builder()
                    .url("$SUPABASE_URL/storage/v1/object/pet-photos/$path")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "image/jpeg")
                    .addHeader("x-upsert", "true")   // overwrite existing file — no duplicates!
                    .post(body)
                    .build()

                val response = http.newCall(request).execute()
                if (response.isSuccessful) {
                    val publicUrl = "$SUPABASE_URL/storage/v1/object/public/pet-photos/$path"
                    Log.d(TAG, "Photo uploaded: $publicUrl")
                    publicUrl
                } else {
                    Log.e(TAG, "Photo upload failed (${response.code}): ${response.body?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "uploadPhoto error", e)
                null
            }
        }
    }

    // ── Push profile to Supabase ──────────────────────────────────────────────

    // Returns a map of petId → new Supabase URL for every pet photo that was just uploaded.
    // The caller should use this to update the local Room DB so future syncs skip re-uploads.
    suspend fun pushProfile(profile: PublicProfile): Map<Int, String> {
        val uid   = ensureSignedIn()
        val token = getStoredToken() ?: throw Exception("No auth token — call ensureSignedIn() first")

        val uploadedUrls = mutableMapOf<Int, String>()

        // Process each pet: upload local photos to Storage, keep existing URLs as-is
        val processedPets = withContext(Dispatchers.IO) {
            profile.pets.map { pet ->
                val uri = pet.photoUri
                when {
                    // Already an https:// URL or data: URI — nothing to do
                    uri != null && (uri.startsWith("https://") || uri.startsWith("data:")) -> {
                        Log.d(TAG, "Pet[${pet.name}]: already has URL/data URI, keeping as-is")
                        pet
                    }
                    // Local file — compress and upload to Supabase Storage
                    uri != null && (uri.startsWith("file://") || uri.startsWith("content://")) -> {
                        Log.d(TAG, "Pet[${pet.name}]: uploading local photo…")
                        val stream  = openStream(Uri.parse(uri))
                        val bytes   = if (stream != null) compressPhoto(stream) else null
                        val pubUrl  = if (bytes != null) uploadPhotoToStorage(uid, pet.petId, pet.name, bytes) else null
                        if (pubUrl != null) uploadedUrls[pet.petId] = pubUrl
                        pet.copy(photoUri = pubUrl ?: uri)   // fallback to original if upload fails
                    }
                    // No photo
                    else -> pet
                }
            }
        }

        // Build the database row with snake_case field names
        val row = ProfileRow(
            uid                 = uid,
            trainerName         = profile.trainerName,
            level               = profile.level,
            streak              = profile.streak,
            bondPoints          = profile.bondPoints,
            petCount            = profile.petCount,
            speciesCount        = profile.speciesCount,
            pets                = processedPets,
            unlockedBadgeTitles = profile.unlockedBadgeTitles,
            updatedAt           = System.currentTimeMillis(),
            bannerIndex         = profile.bannerIndex
        )

        withContext(Dispatchers.IO) {
            val json    = gson.toJson(row)
            val body    = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/profiles")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                // merge-duplicates = upsert (insert or update based on primary key)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body)
                .build()

            var response = http.newCall(request).execute()

            // If token expired, refresh and retry once
            if (response.code == 401) {
                Log.d(TAG, "Token expired, refreshing…")
                if (refreshToken()) {
                    val newToken = getStoredToken() ?: return@withContext
                    response = http.newCall(
                        request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                    ).execute()
                }
            }

            if (!response.isSuccessful) {
                throw Exception("Profile push failed (${response.code}): ${response.body?.string()}")
            }
            Log.d(TAG, "Profile pushed OK for uid=$uid")
        }
        return uploadedUrls
    }

    // ── Fetch profile from Supabase ───────────────────────────────────────────

    suspend fun fetchProfile(uid: String): PublicProfile? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/profiles?uid=eq.$uid&select=*")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()

                val response = http.newCall(request).execute()
                val rawBody  = response.body?.string()

                if (!response.isSuccessful || rawBody == null) {
                    Log.e(TAG, "fetchProfile failed (${response.code})")
                    return@withContext null
                }

                // Supabase REST always returns an array, even for single-row queries
                val listType = object : TypeToken<List<ProfileRow>>() {}.type
                val rows     = gson.fromJson<List<ProfileRow>>(rawBody, listType)
                val row      = rows?.firstOrNull() ?: return@withContext null

                PublicProfile(
                    uid                 = row.uid,
                    trainerName         = row.trainerName,
                    level               = row.level,
                    streak              = row.streak,
                    bondPoints          = row.bondPoints,
                    petCount            = row.petCount,
                    speciesCount        = row.speciesCount,
                    pets                = row.pets,
                    unlockedBadgeTitles = row.unlockedBadgeTitles,
                    updatedAt           = row.updatedAt,
                    bannerIndex         = row.bannerIndex
                )
            } catch (e: Exception) {
                Log.e(TAG, "fetchProfile error for uid=$uid", e)
                null
            }
        }
    }
}
