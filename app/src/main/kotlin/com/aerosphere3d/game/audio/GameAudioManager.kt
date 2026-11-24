package com.aerosphere3d.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.aerosphere3d.game.R

/**
 * AeroSphere 3D Native Audio Manager
 * Manages low-latency arcade SFX via SoundPool and looping BGM & Victory Fanfare via MediaPlayer
 * 100% Zero-Ads, fully offline, kid-friendly audio design
 */
class GameAudioManager private constructor(private val context: Context) {

    private val soundPool: SoundPool
    private val sfxMap = mutableMapOf<Int, Int>()

    private var bgmPlayer: MediaPlayer? = null
    private var victoryPlayer: MediaPlayer? = null
    private var isMuted: Boolean = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        // Preload sound effects
        loadSfx(SFX_COIN, R.raw.sfx_coin)
        loadSfx(SFX_KEY, R.raw.sfx_key)
        loadSfx(SFX_LIFE, R.raw.sfx_life)
        loadSfx(SFX_SMASH, R.raw.sfx_smash)
        loadSfx(SFX_CHECKPOINT, R.raw.sfx_checkpoint)
        loadSfx(SFX_PLINKO_PEG, R.raw.sfx_plinko_peg)
        loadSfx(SFX_BUTTON, R.raw.sfx_button)
    }

    private fun loadSfx(key: Int, resId: Int) {
        try {
            val soundId = soundPool.load(context, resId, 1)
            sfxMap[key] = soundId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load SFX $key", e)
        }
    }

    private fun playSfx(key: Int, volume: Float = 1.0f) {
        if (isMuted) return
        val soundId = sfxMap[key] ?: return
        soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    fun playCoin() = playSfx(SFX_COIN, 0.9f)
    fun playKey() = playSfx(SFX_KEY, 1.0f)
    fun playLife() = playSfx(SFX_LIFE, 1.0f)
    fun playSmash() = playSfx(SFX_SMASH, 1.0f)
    fun playCheckpoint() = playSfx(SFX_CHECKPOINT, 0.95f)
    fun playPlinkoPeg() = playSfx(SFX_PLINKO_PEG, 0.75f)
    fun playButton() = playSfx(SFX_BUTTON, 0.6f)

    /**
     * Start cheerful background music loop
     */
    fun startBgm() {
        if (isMuted) return
        if (bgmPlayer == null) {
            try {
                bgmPlayer = MediaPlayer.create(context, R.raw.bgm_gameplay).apply {
                    isLooping = true
                    setVolume(0.55f, 0.55f)
                    start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start BGM", e)
            }
        } else if (!bgmPlayer!!.isPlaying) {
            bgmPlayer?.start()
        }
    }

    fun pauseBgm() {
        if (bgmPlayer?.isPlaying == true) {
            bgmPlayer?.pause()
        }
    }

    fun resumeBgm() {
        if (!isMuted && bgmPlayer?.isPlaying == false) {
            bgmPlayer?.start()
        }
    }

    fun stopBgm() {
        bgmPlayer?.stop()
        bgmPlayer?.release()
        bgmPlayer = null
    }

    /**
     * Play level victory celebration fanfare when finishing a stage!
     */
    fun playVictoryFanfare() {
        if (isMuted) return
        pauseBgm() // Pause background music to let celebration music shine

        victoryPlayer?.release()
        try {
            victoryPlayer = MediaPlayer.create(context, R.raw.sfx_victory_fanfare).apply {
                setVolume(0.85f, 0.85f)
                setOnCompletionListener {
                    // Do not auto-restart BGM immediately; keep atmosphere calm until next stage
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play victory fanfare", e)
        }
    }

    fun stopVictoryFanfare() {
        victoryPlayer?.stop()
        victoryPlayer?.release()
        victoryPlayer = null
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            pauseBgm()
            stopVictoryFanfare()
        } else {
            resumeBgm()
        }
        return isMuted
    }

    fun release() {
        stopBgm()
        stopVictoryFanfare()
        soundPool.release()
        sfxMap.clear()
        instance = null
    }

    companion object {
        private const val TAG = "GameAudioManager"

        const val SFX_COIN = 1
        const val SFX_KEY = 2
        const val SFX_LIFE = 3
        const val SFX_SMASH = 4
        const val SFX_CHECKPOINT = 5
        const val SFX_PLINKO_PEG = 6
        const val SFX_BUTTON = 7

        @Volatile
        private var instance: GameAudioManager? = null

        fun getInstance(context: Context): GameAudioManager {
            return instance ?: synchronized(this) {
                instance ?: GameAudioManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
