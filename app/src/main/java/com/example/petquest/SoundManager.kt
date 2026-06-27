// ============================================================
// FILE PATH:  app/src/main/java/com/example/petquest/SoundManager.kt
//
// BUGS FIXED:
//   1. MUSIC RESET ON BACK — startMusicInternal() was always releasing and
//      restarting the MediaPlayer. When navigating back from pet_verify, a new
//      MainScreen is created which called enableAndStartMusic() again, which
//      restarted the music from the beginning. Fixed by checking isPlaying first.
//   2. SILENT AUDIO ON API 26+ — Added AudioFocusRequest so Android actually
//      grants the app permission to play audio (required on modern Android).
//
// HOW TO APPLY:
//   1. Open SoundManager.kt in Android Studio
//   2. Select ALL (Ctrl+A), Delete
//   3. Paste everything BELOW this comment block
// ============================================================

package com.example.petquest

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.util.Log

object SoundManager {

    private const val TAG = "SoundManager"

    private var appContext   : Context?      = null
    private var audioManager : AudioManager? = null
    private var mediaPlayer  : MediaPlayer?  = null
    private var soundPool    : SoundPool?    = null
    private var focusRequest : AudioFocusRequest? = null

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
    private var hasFocus        = false

    // ── Audio focus listener ───────────────────────────────────────────────
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                mediaPlayer?.setVolume(0.55f, 0.55f)
                if (shouldPlayMusic && musicEnabled) {
                    if (mediaPlayer == null) startMusicInternal()
                    else try { mediaPlayer?.start() } catch (e: Exception) { Log.e(TAG, "focus gain resume failed", e) }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasFocus = false
                try { mediaPlayer?.pause() } catch (e: Exception) { /* ignore */ }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                try { mediaPlayer?.pause() } catch (e: Exception) { /* ignore */ }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                try { mediaPlayer?.setVolume(0.15f, 0.15f) } catch (e: Exception) { /* ignore */ }
            }
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
        // FIX: if music is already playing, do nothing — prevents restart on back-nav
        if (mediaPlayer?.isPlaying == true) {
            Log.d(TAG, "enableAndStartMusic: already playing, skipping restart")
            return
        }
        requestFocusAndPlay()
    }

    // ── Call from MainActivity.onResume() ─────────────────────────────────
    fun onAppForegrounded() {
        if (!shouldPlayMusic || !musicEnabled) return
        if (mediaPlayer?.isPlaying == true) return  // already going
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
        hasFocus        = false
        Log.d(TAG, "SoundManager released")
    }

    // ── Private ────────────────────────────────────────────────────────────

    private fun requestFocusAndPlay() {
        val am = audioManager ?: return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
        focusRequest = request

        val result = am.requestAudioFocus(request)
        Log.d(TAG, "Audio focus request result: $result")

        when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                hasFocus = true
                startMusicInternal()
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                Log.d(TAG, "Audio focus delayed — will play when granted")
            }
            else -> {
                Log.w(TAG, "Audio focus denied (result=$result)")
            }
        }
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        hasFocus = false
    }

    private fun startMusicInternal() {
        val ctx = appContext ?: return
        if (!shouldPlayMusic || !musicEnabled) return

        // FIX: don't restart if already playing
        if (mediaPlayer?.isPlaying == true) return

        val res = ctx.resources.getIdentifier("bg_music", "raw", ctx.packageName)
        if (res == 0) {
            Log.w(TAG, "bg_music not found in res/raw — skipping")
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
