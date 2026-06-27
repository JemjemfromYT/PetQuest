// ============================================================
// UPDATED FILE: app/src/main/java/com/example/petquest/SoundManager.kt
// FULL REPLACEMENT
//
// KEY FIXES vs previous version:
//  1. Stores applicationContext internally after init — no more context passing at runtime
//  2. Uses MediaPlayer() + prepare() instead of MediaPlayer.create() — gives error callbacks
//  3. Sets AudioAttributes with USAGE_MEDIA so Android knows this is music
//  4. shouldPlayMusic flag — music only starts after user passes onboarding
//  5. enableAndStartMusic() — call once from MainScreen composable
//  6. onAppForegrounded() — call from MainActivity.onResume (no context needed)
//  7. Volume raised to 0.55f (was 0.30f which was too quiet)
// ============================================================

package com.example.petquest

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.util.Log

object SoundManager {

    private val TAG = "SoundManager"

    private var appContext     : Context?     = null
    private var mediaPlayer    : MediaPlayer? = null
    private var soundPool      : SoundPool?   = null

    private var idTaskComplete = 0
    private var idStreak       = 0
    private var idLevelUp      = 0
    private var idTap          = 0
    private var idPetHappy     = 0

    var musicEnabled = true
        set(value) { field = value; if (!value) pauseMusic() else onAppForegrounded() }

    var sfxEnabled = true

    // True once the user has passed the Welcome / Benefits screens
    private var shouldPlayMusic = false
    private var initialized     = false

    // ── Call once from PetQuestApplication.onCreate() ─────────────────────
    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext

        val sfxAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(sfxAttrs)
            .build()

        idTaskComplete = load("sfx_task_complete")
        idStreak       = load("sfx_streak")
        idLevelUp      = load("sfx_level_up")
        idTap          = load("sfx_tap")
        idPetHappy     = load("sfx_pet_happy")

        initialized = true
        Log.d(TAG, "SoundManager initialised. SFX ids: task=$idTaskComplete streak=$idStreak level=$idLevelUp tap=$idTap pet=$idPetHappy")
    }

    // ── Call from MainScreen's LaunchedEffect(Unit) ────────────────────────
    // This marks that the user has passed onboarding and music should play.
    fun enableAndStartMusic() {
        shouldPlayMusic = true
        startMusicInternal()
    }

    // ── Call from MainActivity.onResume() ─────────────────────────────────
    fun onAppForegrounded() {
        if (!shouldPlayMusic || !musicEnabled) return
        if (mediaPlayer == null) {
            startMusicInternal()           // wasn't started yet — start now
        } else {
            try { mediaPlayer?.start() } catch (e: Exception) { Log.e(TAG, "resume failed", e) }
        }
    }

    fun pauseMusic() {
        try { mediaPlayer?.pause() } catch (e: Exception) { Log.e(TAG, "pause failed", e) }
    }

    // ── Sound effects ──────────────────────────────────────────────────────
    fun playTaskComplete() = play(idTaskComplete, 0.90f)
    fun playStreak()       = play(idStreak,       1.00f)
    fun playLevelUp()      = play(idLevelUp,      1.00f)
    fun playTap()          = play(idTap,          0.65f)
    fun playPetHappy()     = play(idPetHappy,     0.85f)

    // ── Lifecycle — call from MainActivity.onDestroy() ────────────────────
    fun release() {
        mediaPlayer?.release(); mediaPlayer = null
        soundPool?.release();   soundPool   = null
        shouldPlayMusic = false
        initialized     = false
        Log.d(TAG, "SoundManager released")
    }

    // ── Private ────────────────────────────────────────────────────────────
    private fun startMusicInternal() {
        val ctx = appContext ?: return
        val res = ctx.resources.getIdentifier("bg_music", "raw", ctx.packageName)
        if (res == 0) {
            Log.w(TAG, "bg_music not found in res/raw — skipping music")
            return
        }
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(ctx, Uri.parse("android.resource://${ctx.packageName}/$res"))
                isLooping = true
                setVolume(0.55f, 0.55f)
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra — music will not play")
                    mediaPlayer = null
                    false
                }
                prepare()          // synchronous — fine for local raw resources
                start()
            }
            Log.d(TAG, "Background music started OK")
        } catch (e: Exception) {
            Log.e(TAG, "startMusicInternal failed: ${e.message}", e)
            mediaPlayer = null
        }
    }

    private fun load(name: String): Int {
        val ctx = appContext ?: return 0
        val res = ctx.resources.getIdentifier(name, "raw", ctx.packageName)
        return if (res != 0 && soundPool != null) soundPool!!.load(ctx, res, 1) else 0
    }

    private fun play(id: Int, vol: Float) {
        if (!sfxEnabled || id == 0) return
        try { soundPool?.play(id, vol, vol, 1, 0, 1f) } catch (e: Exception) { Log.e(TAG, "play failed", e) }
    }
}
