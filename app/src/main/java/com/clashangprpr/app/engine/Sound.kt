package com.clashangprpr.app.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

object Sound {
    private val names = listOf(
        "swing", "hit", "hurt", "coin", "buy", "click", "deny", "dash", "magic", "explode",
        "potion", "portal", "slowmo", "chop", "mine", "gate", "boss", "gameover", "victory", "era_clear", "heart", "whisper"
    )
    private var ctx: Context? = null
    private var pool: SoundPool? = null
    private val ids = HashMap<String, Int>()
    private var music: MediaPlayer? = null
    private var currentTrack = -1
    var sfxOn = true
    var musicOn = true
    private var paused = false

    fun init(c: Context) {
        ctx = c.applicationContext
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val sp = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
            for (n in names) {
                try {
                    val afd = ctx!!.assets.openFd("audio/$n.wav")
                    ids[n] = sp.load(afd, 1)
                    afd.close()
                } catch (e: Exception) {
                }
            }
            pool = sp
        } catch (e: Exception) {
            pool = null
        }
    }

    fun play(name: String, volume: Float = 1f) {
        if (!sfxOn) return
        val id = ids[name] ?: return
        try {
            pool?.play(id, volume, volume, 1, 0, 1f)
        } catch (e: Exception) {
        }
    }

    fun playMusic(track: Int) {
        if (track == currentTrack && music != null) {
            applyMusicState()
            return
        }
        stopMusic()
        currentTrack = track
        val c = ctx ?: return
        try {
            val mp = MediaPlayer()
            val afd = c.assets.openFd("audio/bgm_$track.wav")
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.isLooping = true
            mp.setVolume(0.30f, 0.30f)
            mp.prepare()
            music = mp
            applyMusicState()
        } catch (e: Exception) {
            music = null
        }
    }

    private fun applyMusicState() {
        val mp = music ?: return
        try {
            if (musicOn && !paused) {
                if (!mp.isPlaying) mp.start()
            } else {
                if (mp.isPlaying) mp.pause()
            }
        } catch (e: Exception) {
        }
    }

    fun setMusic(on: Boolean) {
        musicOn = on
        applyMusicState()
    }

    fun pauseAll() {
        paused = true
        applyMusicState()
    }

    fun resumeAll() {
        paused = false
        applyMusicState()
    }

    fun stopMusic() {
        try {
            music?.stop()
            music?.release()
        } catch (e: Exception) {
        }
        music = null
        currentTrack = -1
    }
}
