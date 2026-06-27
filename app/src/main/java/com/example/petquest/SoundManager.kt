// ============================================================
// NEW FILE: app/src/main/java/com/example/petquest/SoundManager.kt
//
// SETUP STEPS:
//  1. Create folder: app/src/main/res/raw/   (right-click res → New → Directory, name it "raw")
//  2. Drop your generated .ogg / .mp3 files in that folder with EXACT these names:
//       bg_music.ogg          ← looping background music
//       sfx_task_complete.ogg ← task done chime
//       sfx_streak.ogg        ← streak fanfare
//       sfx_level_up.ogg      ← level-up sound
//       sfx_tap.ogg           ← subtle button tap
//       sfx_pet_happy.ogg     ← pet happy sound
//  3. The code below uses getIdentifier() so it compiles and runs
//     even before any files are added — missing files are silently skipped.
// ============================================================

package com.example.petquest

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

object SoundManager {

    private var mediaPlayer : MediaPlayer? = null
    private var soundPool   : SoundPool?   = null

    private var idTaskComplete = 0
    private var idStreak       = 0
    private var idLevelUp      = 0
    private var idTap          = 0
    private var idPetHappy     = 0

    var musicEnabled = true
        set(value) { field = value; if (!value) pauseMusic() else resumeMusic() }

    var sfxEnabled = true

    private var initialized = false

    // ── Call once from PetQuestApplication.onCreate() ─────────────────────
    fun init(context: Context) {
        if (initialized) return
        val ctx = context.applicationContext

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()

        idTaskComplete = load(ctx, "sfx_task_complete")
        idStreak       = load(ctx, "sfx_streak")
        idLevelUp      = load(ctx, "sfx_level_up")
        idTap          = load(ctx, "sfx_tap")
        idPetHappy     = load(ctx, "sfx_pet_happy")

        initialized = true
    }

    // ── Start looping background music ─────────────────────────────────────
    // Call this when the user reaches the main screen (not the welcome screen).
    fun startMusic(context: Context) {
        if (!musicEnabled) return
        val ctx = context.applicationContext
        val res = ctx.resources.getIdentifier("bg_music", "raw", ctx.packageName)
        if (res == 0) return                    // file not added yet — skip silently
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(ctx, res)?.apply {
                isLooping = true
                setVolume(0.30f, 0.30f)         // quiet enough not to be annoying
                start()
            }
        } catch (_: Exception) { /* ignore */ }
    }

    fun pauseMusic()  { try { mediaPlayer?.pause() }  catch (_: Exception) {} }
    fun resumeMusic() { if (musicEnabled) try { mediaPlayer?.start() } catch (_: Exception) {} }

    // ── Sound effects ──────────────────────────────────────────────────────
    fun playTaskComplete() = play(idTaskComplete, 0.90f)
    fun playStreak()       = play(idStreak,       1.00f)
    fun playLevelUp()      = play(idLevelUp,      1.00f)
    fun playTap()          = play(idTap,          0.65f)
    fun playPetHappy()     = play(idPetHappy,     0.85f)

    // ── Lifecycle ──────────────────────────────────────────────────────────
    fun release() {
        mediaPlayer?.release(); mediaPlayer = null
        soundPool?.release();   soundPool   = null
        initialized = false
    }

    // ── Private helpers ────────────────────────────────────────────────────
    private fun load(ctx: Context, name: String): Int {
        val res = ctx.resources.getIdentifier(name, "raw", ctx.packageName)
        return if (res != 0 && soundPool != null) soundPool!!.load(ctx, res, 1) else 0
    }

    private fun play(id: Int, vol: Float) {
        if (!sfxEnabled || id == 0) return
        try { soundPool?.play(id, vol, vol, 1, 0, 1f) } catch (_: Exception) {}
    }
}
