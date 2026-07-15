package com.example.trenchwar.game

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    fun playShoot() {
        scope.launch {
            val duration = 0.12f // seconds
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // Fast pitch drop from 800Hz to 150Hz for a nice punchy gunshot
                val freq = 800f - (t / duration) * 650f
                // Add some crunch noise
                val noise = (Math.random() * 2.0 - 1.0).toFloat() * 0.18f
                val sinValue = sin(2.0 * Math.PI * freq * t).toFloat()
                buffer[i] = (((sinValue + noise) * 0.35f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playHit() {
        scope.launch {
            val duration = 0.05f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val freq = 1100f - (t / duration) * 700f
                buffer[i] = ((sin(2.0 * Math.PI * freq * t) * 0.22f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playDeath() {
        scope.launch {
            val duration = 0.22f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                // Slide pitch down to symbolize dying/falling
                val freq = 380f - (t / duration) * 280f
                val sinValue = sin(2.0 * Math.PI * freq * t).toFloat()
                buffer[i] = ((sinValue * 0.28f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playStep() {
        scope.launch {
            val duration = 0.03f
            val numSamples = (duration * SAMPLE_RATE).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / SAMPLE_RATE
                val freq = 90f + (t / duration) * 30f
                val noise = (Math.random() * 2.0 - 1.0).toFloat() * 0.12f
                val sinValue = sin(2.0 * Math.PI * freq * t).toFloat()
                buffer[i] = (((sinValue + noise) * 0.12f) * Short.MAX_VALUE).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playVictory() {
        scope.launch {
            // Happy military-like triumphant fanfare: C4 -> E4 -> G4 -> C5 -> E5 -> G5
            val notes = listOf(261.63f, 329.63f, 392.00f, 523.25f, 659.25f, 783.99f)
            val noteDuration = 0.12f
            val samplesPerNote = (noteDuration * SAMPLE_RATE).toInt()
            val totalSamples = samplesPerNote * notes.size
            val buffer = ShortArray(totalSamples)
            for (n in notes.indices) {
                val freq = notes[n]
                val offset = n * samplesPerNote
                for (i in 0 until samplesPerNote) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val envelope = 1.0f - (i.toFloat() / samplesPerNote)
                    val value = sin(2.0 * Math.PI * freq * t).toFloat() * envelope
                    buffer[offset + i] = ((value * 0.3f) * Short.MAX_VALUE).toInt().toShort()
                }
            }
            playPcm(buffer)
        }
    }

    fun playDefeat() {
        scope.launch {
            // Sad dramatic brass-like falling fanfare: G4 -> E4b -> C4 -> B3b -> A3
            val notes = listOf(392.00f, 311.13f, 261.63f, 233.08f, 220.00f)
            val noteDuration = 0.25f
            val samplesPerNote = (noteDuration * SAMPLE_RATE).toInt()
            val totalSamples = samplesPerNote * notes.size
            val buffer = ShortArray(totalSamples)
            for (n in notes.indices) {
                val freq = notes[n]
                val offset = n * samplesPerNote
                for (i in 0 until samplesPerNote) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val envelope = 1.0f - (i.toFloat() / samplesPerNote)
                    val value = sin(2.0 * Math.PI * freq * t).toFloat() * envelope
                    buffer[offset + i] = ((value * 0.3f) * Short.MAX_VALUE).toInt().toShort()
                }
            }
            playPcm(buffer)
        }
    }

    private fun playPcm(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer.size * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            scope.launch {
                val durationMs = (buffer.size.toFloat() / SAMPLE_RATE * 1000).toLong() + 100
                kotlinx.coroutines.delay(durationMs)
                audioTrack.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
