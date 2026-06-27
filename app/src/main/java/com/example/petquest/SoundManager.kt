// ============================================================
// FILE PATH:  app/src/main/java/com/example/petquest/SoundManager.kt
//
// BUGS FIXED:
//   1. API 26 BUILD ERROR — AudioFocusRequest.Builder requires API 26 but
//      minSdk is 24. Fixed by version-checking at runtime: API 26+ uses the
//      modern AudioFocusRequest; API 24-25 uses the deprecated (but functional)
//      requestAudioFocus(listener, streamType, durationHint).
//   2. MUSIC RESTARTS ON BACK — Added isPlaying check so music doesn't restart
//      when MainScreen is re-created after navigating back from pet_verify.
//
// HOW TO APPLY:
//   1. Open SoundManager.kt in Android Studio
//   2. Select ALL (Ctrl+A), Delete
//   3. Paste everything BELOW this comment block
// ============================================================

package com.example.petquest

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.util.Log

object SoundManager {

    private const val TAG = "SoundManager"

    private var appContext   : Context?      = null
    private var audioManager : AudioManager? = null
    private var mediaPlayer  : MediaPlayer?  = null
    private var soundPool    : SoundPool?    = null

    // Stored as Any? because AudioFocusRequest class itself requires API 26
    private var focusRequest : Any? = null

    private var idTaskComplete = 0
    private var idStreak       = 0
    private var idLevelUp      = 0
    private var idTap          = 0
    private var idPetHappy     = 0

    var musicEnabled = true
        set(value) { field = value; if (!value) pauseMusic() else onAppForegrounded() }

    var sfxEnabled = true

    private var shouldPlayMusic = false
    private var initialized     = false

    // ── Audio focus listener (works on all API levels) ─────────────────────
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(0.55f, 0.55f)
                if (shouldPlayMusic && musicEnabled) {
                    if (mediaPlayer == null) startMusicInternal()
                    else try { mediaPlayer?.start() } catch (_: Exception) {}
                }
            }
            AudioManager.AUDIOFOCUS_LOSS ->
                try { mediaPlayer?.pause() } catch (_: Exception) {}
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                try { mediaPlayer?.pause() } catch (_: Exception) {}
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                try { mediaPlayer?.setVolume(0.15f, 0.15f) } catch (_: Exception) {}
        }
    }

    // ── Init — call once from PetQuestApplication.onCreate() ──────────────
    fun init(context: Context) {
        if (initialized) return
        appContext   = context.applicationContext
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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
    fun enableAndStartMusic() {
        shouldPlayMusic = true
        // Don't restart if already playing (happens when navigating back to MainScreen)
        if (mediaPlayer?.isPlaying == true) {
            Log.d(TAG, "enableAndStartMusic: already playing, skipping")
            return
        }
        requestFocusAndPlay()
    }

    // ── Call from MainActivity.onResume() ─────────────────────────────────
    fun onAppForegrounded() {
        if (!shouldPlayMusic || !musicEnabled) return
        if (mediaPlayer?.isPlaying == true) return
        requestFocusAndPlay()
    }

    fun pauseMusic() {
        try { mediaPlayer?.pause() } catch (e: Exception) { Log.e(TAG, "pause failed", e) }
        abandonFocus()
    }

    // ── Sound effects ──────────────────────────────────────────────────────
    fun playTaskComplete() = play(idTaskComplete, 0.90f)
    fun playStreak()       = play(idStreak,       1.00f)
    fun playLevelUp()      = play(idLevelUp,      1.00f)
    fun playTap()          = play(idTap,          0.65f)
    fun playPetHappy()     = play(idPetHappy,     0.85f)

    // ── Call from MainActivity.onDestroy() ────────────────────────────────
    fun release() {
        mediaPlayer?.release(); mediaPlayer = null
        soundPool?.release();   soundPool   = null
        abandonFocus()
        shouldPlayMusic = false
        initialized     = false
        Log.d(TAG, "SoundManager released")
    }

    // ── Private ────────────────────────────────────────────────────────────

    private fun requestFocusAndPlay() {
        val am = audioManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+ — modern AudioFocusRequest
            val attrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            focusRequest = request
            val result = am.requestAudioFocus(request)
            Log.d(TAG, "Audio focus request result (API 26+): $result")

            when (result) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> startMusicInternal()
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> Log.d(TAG, "Focus delayed — will start when granted")
                else -> Log.w(TAG, "Focus denied ($result)")
            }
        } else {
            // API 24–25 — deprecated but works fine
            @Suppress("DEPRECATION")
            val result = am.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            Log.d(TAG, "Audio focus request result (legacy): $result")
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                startMusicInternal()
            }
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (focusRequest as? android.media.AudioFocusRequest)?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(focusChangeListener)
        }
        focusRequest = null
    }

    private fun startMusicInternal() {
        val ctx = appContext ?: return
        if (!shouldPlayMusic || !musicEnabled) return
        if (mediaPlayer?.isPlaying == true) return

        val res = ctx.resources.getIdentifier("bg_music", "raw", ctx.packageName)
        if (res == 0) {
            Log.w(TAG, "bg_music not found in res/raw")
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
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    mediaPlayer = null
                    false
                }
                prepare()
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
        try { soundPool?.play(id, vol, vol, 1, 0, 1f) } catch (e: Exception) {
            Log.e(TAG, "play failed", e)
        }
    }
}
