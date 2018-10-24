@file:Suppress("ConstantConditionIf")

package com.demo.pet.petapp.stt

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.demo.pet.petapp.Log
import com.demo.pet.petapp.errorLog
import com.demo.pet.petapp.debugLog
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Continuous sound (voice) recorder.
 */
class VoiceRecorder(val context: Context) {
    @Suppress("PrivatePropertyName", "SimplifyBooleanWithConstants")
    private val IS_DEBUG = false || Log.IS_DEBUG

    companion object {
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private const val AMPLITUDE_THRESHOLD = 1500.0f
        private const val SPEECH_TIMEOUT_MILLIS = 2000
        private const val MAX_SPEECH_LENGTH_MILLIS = 30 * 1000

    }

    private val samplingRate: Int

    init {
        // Sampling rate.
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        samplingRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt()

        debugLog("## samplingRat = $samplingRate")

    }

    private var audioRec: AudioRecord? = null

    private var backThread: Thread? = null

    private var buffer: ByteArray? = null

    private val lock = ReentrantLock()

    // Last sound level up timestamp. 0 means sound is not started. Based on uptimeMillis.
    private var soundUpTimeMillis = 0L

    // Last sound level down timestamp. MAX means sound is not stopped. Based on uptimeMillis.
    private var soundDownTimeMillis = Long.MAX_VALUE

    var callback :Callback? = null

    /** Callback interface. */
    interface Callback {
        /**
         * Life-Cycle callback.
         * @param samplingRate
         */
        fun onStarted(samplingRate: Int)

        /** Life-Cycle callback. */
        fun onStopped()

        /**
         * Recorded data callback.
         *
         * @param buffer Data
         * @param format AudioFormat
         * @param size Length of buffer
         */
        fun onRecorded(buffer: ByteArray, format: Int, size: Int)
    }

    /**
     * Release all references.
     */
    fun release() {
        stop()
        callback = null
    }

    /**
     * Life cycle API.
     */
     fun start() {
        if (IS_DEBUG) debugLog("VoiceRecorder.start() : E")

        // Stop before re-start.
        stop()

        // Create new recording session.
        val isSuccess = createAudioRecord()
        if (!isSuccess) throw RuntimeException("Failed to create AudioRecord.")

        // Start.
        audioRec?.startRecording()

        // Worker thread.
        backThread = Thread(AudioProc(), "voice-rec-back")
        backThread?.start()

        if (IS_DEBUG) debugLog("VoiceRecorder.start() : X")
    }

    /**
     * Life-Cycle API.
     */
     fun stop() {
        if (IS_DEBUG) debugLog("VoiceRecorder.stop() : E")

        lock.withLock {
            // Callback if voice rec is on-going.
            if (soundUpTimeMillis != 0L) {
                soundUpTimeMillis = 0L
                soundDownTimeMillis = Long.MAX_VALUE
                callback?.onStopped()
            }

            // Stop back thread.
            backThread?.interrupt()
            backThread = null

            // Release audio rec resource.
            audioRec?.stop()
            audioRec?.release()
            audioRec = null

            // Release mem.
            buffer = null
        }

        if (IS_DEBUG) debugLog("VoiceRecorder.stop() : X")
    }

    /**
     * Create new AudioRecord instance.
     * @return Success or not.
     */
    private fun createAudioRecord(): Boolean {
        // Buffer size.
        val minBufSize = AudioRecord.getMinBufferSize(
                samplingRate,
                CHANNEL,
                ENCODING)
        // Check.
        if (minBufSize == AudioRecord.ERROR_BAD_VALUE) {
            errorLog("Failed to get buf size.")
            return false
        }

        val ar = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                samplingRate,
                CHANNEL,
                ENCODING,
                minBufSize)
        // Check.
        return if (ar.state == AudioRecord.STATE_INITIALIZED) {
            buffer = ByteArray(minBufSize)
            audioRec = ar

            true
        } else {
            errorLog("Failed to create AudioRecord.")
            ar.release()

            false
        }
    }

    /**
     * Continuous recorded audio processing task.
     */
    private inner class AudioProc: Runnable {
        override fun run () {
            // Loop.
            while (true) {
                if (IS_DEBUG) debugLog("VoiceRecorder.AudioProc.run() : E")

                // Check stop.
                val isInterrupted = lock.withLock {
                    Thread.currentThread().isInterrupted
                }
                if (isInterrupted) break

                val ar = audioRec ?: throw RuntimeException("audioRec is already released.")
                val buf = buffer ?: throw RuntimeException("buffer is already released.")

                // Read data.
                val size = ar.read(buf, 0, buf.size)

                val now = SystemClock.uptimeMillis()

                // Check voice is silence or not.
                if (isSounded(buf, size, ENCODING)) {
                    if (IS_DEBUG) debugLog("VoiceRecorder.AudioProc.run() : SOUNDED")
                    // Voice available.

                    if (soundUpTimeMillis == 0L) {
                        // Voice rec started.
                        callback?.onStarted(samplingRate)
                        soundUpTimeMillis = now
                    }

                    soundDownTimeMillis = Long.MAX_VALUE

                    callback?.onRecorded(buf, ENCODING, size)

                    if (now - soundUpTimeMillis > MAX_SPEECH_LENGTH_MILLIS) {
                        // Speech is too long, stop forced.
                        onEnd()
                    }
                } else {
                    if (IS_DEBUG) debugLog("VoiceRecorder.AudioProc.run() : NO SOUND")
                    // No voice.

                    if (soundUpTimeMillis != 0L && soundDownTimeMillis == Long.MAX_VALUE) {
                        // Voice is disappeared.
                        soundDownTimeMillis = now
                    }

                    if (soundUpTimeMillis != 0L) {
                        // Speech is not end.
                        callback?.onRecorded(buf, ENCODING, size)
                    }

                    if (now - soundDownTimeMillis > SPEECH_TIMEOUT_MILLIS) {
                        // Speech is end.
                        onEnd()
                    }
                }

                if (IS_DEBUG) debugLog("VoiceRecorder.AudioProc.run() : X")
            }
        }

        private fun onEnd() {
            callback?.onStopped()
            soundUpTimeMillis = 0L
            soundDownTimeMillis = Long.MAX_VALUE
        }
    }

    // Detect current sound frame includes sound or not.
    private fun isSounded(buf: ByteArray, size: Int, format: Int): Boolean {
        if (format != AudioFormat.ENCODING_PCM_16BIT) {
            throw RuntimeException("Unsupported Format")
        }

        var total = 0

        for (i in 0 until size step 2) { // 0, 2, 4, ...

            // The buffer has LINEAR16 in little endian.
            var s = buf[i + 1].toInt()

            if (s < 0) s *= -1
            s = s shl 8 // Shift to left
            s += Math.abs(buf[i].toInt())

            total += s
        }

        val average = total.toFloat() / size.toFloat()

//        debugLog("VoiceRecorder.isSounded() : Level = $average")

        // Check sound level.
        return average > AMPLITUDE_THRESHOLD
    }

}
