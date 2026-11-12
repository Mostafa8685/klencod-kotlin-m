package com.clashangprpr.app.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.sin
import kotlin.random.Random

object SoundEngine {
    var isMuted: Boolean = false
    var sfxVolume: Float = 1.0f
    var musicVolume: Float = 0.5f

    private val executor = Executors.newSingleThreadExecutor()
    private const val SAMPLE_RATE = 22050

    fun playSlash() = playTone(520f, 180f, 75, 0.25f, noise = true)
    fun playHit() = playTone(220f, 85f, 90, 0.35f, noise = true)
    fun playCrit() = playTone(780f, 320f, 130, 0.40f, noise = false)
    fun playCoin() = playTone(980f, 1320f, 95, 0.22f, noise = false)
    fun playPotion() = playTone(440f, 880f, 160, 0.25f, noise = false)
    fun playExplosion() = playTone(140f, 42f, 260, 0.45f, noise = true)
    fun playMagic() = playTone(660f, 390f, 140, 0.25f, noise = false)
    fun playSlowMo() = playTone(600f, 160f, 300, 0.30f, noise = false)
    fun playUiClick() = playTone(720f, 840f, 45, 0.16f, noise = false)
    fun playLevelUp() = playTone(520f, 1180f, 280, 0.32f, noise = false)
    fun playHack() = playTone(880f, 1560f, 180, 0.28f, noise = false)

    private fun playTone(startFreq: Float, endFreq: Float, durationMs: Int, baseVolume: Float, noise: Boolean) {
        if (isMuted || sfxVolume <= 0.01f) return
        val volume = (baseVolume * sfxVolume).coerceIn(0f, 0.8f)
        executor.execute {
            try {
                val count = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
                val samples = ShortArray(count)
                var phase = 0.0
                for (i in 0 until count) {
                    val t = i.toFloat() / count
                    val freq = startFreq + (endFreq - startFreq) * t
                    val env = 1f - t
                    phase += 2.0 * Math.PI * freq / SAMPLE_RATE
                    val wave = if (sin(phase) >= 0.0) 1f else -1f
                    val n = if (noise) (Random.nextFloat() * 2f - 1f) * 0.45f else 0f
                    val mixed = ((wave * 0.7f + n) * env * volume).coerceIn(-1f, 1f)
                    samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 20L)
                track.stop()
                track.release()
            } catch (ignored: Exception) {
            }
        }
    }
}
