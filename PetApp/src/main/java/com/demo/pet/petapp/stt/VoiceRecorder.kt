@file:Suppress("ConstantConditionIf", "RedundantLambdaArrow")

package com.demo.pet.petapp.stt

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.errorLog
import com.demo.pet.petapp.util.debugLog
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

/**
 * Continuous sound (voice) recorder.
 */
class VoiceRecorder(context: Context, private val speakThreshold: Int) {
    @Suppress("PrivatePropertyName", "SimplifyBooleanWithConstants")
    private val IS_DEBUG = false || Log.IS_DEBUG

    companion object {
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        private const val SPEECH_TIMEOUT_MILLIS = 1000
        private const val MAX_SPEECH_LENGTH_MILLIS = 30 * 1000

        // Buffer cache size millis before speak detected.
        private const val LAST_BUFFER_CACHE_MILLIS = 500

    }

    private val samplingRate: Int

    init {
        // Sampling rate.
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        samplingRate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt()

        debugLog("## samplingRate = $samplingRate")

        // Check devices.
        val devices = am.getDevices(AudioManager.GET_DEVICES_INPUTS)
        debugLog("## Devices")
        devices.forEach { it ->
            debugLog("#### DEVICE = ${it.productName}")
            if (AudioDeviceInfo.TYPE_AUX_LINE and it.type != 0) debugLog("         TYPE = TYPE_AUX_LINE")
            if (AudioDeviceInfo.TYPE_BLUETOOTH_A2DP and it.type != 0) debugLog("         TYPE = TYPE_BLUETOOTH_A2DP")
            if (AudioDeviceInfo.TYPE_BLUETOOTH_SCO and it.type != 0) debugLog("         TYPE = TYPE_BLUETOOTH_SCO")
            if (AudioDeviceInfo.TYPE_BUILTIN_EARPIECE and it.type != 0) debugLog("         TYPE = TYPE_BUILTIN_EARPIECE")
            if (AudioDeviceInfo.TYPE_BUILTIN_MIC and it.type != 0) debugLog("         TYPE = TYPE_BUILTIN_MIC")
            if (AudioDeviceInfo.TYPE_BUILTIN_SPEAKER and it.type != 0) debugLog("         TYPE = TYPE_BUILTIN_SPEAKER")
            if (AudioDeviceInfo.TYPE_BUS and it.type != 0) debugLog("         TYPE = TYPE_BUS")
            if (AudioDeviceInfo.TYPE_DOCK and it.type != 0) debugLog("         TYPE = TYPE_DOCK")
            if (AudioDeviceInfo.TYPE_FM and it.type != 0) debugLog("         TYPE = TYPE_FM")
            if (AudioDeviceInfo.TYPE_FM_TUNER and it.type != 0) debugLog("         TYPE = TYPE_FM_TUNER")
            if (AudioDeviceInfo.TYPE_HDMI and it.type != 0) debugLog("         TYPE = TYPE_HDMI")
            if (AudioDeviceInfo.TYPE_HDMI_ARC and it.type != 0) debugLog("         TYPE = TYPE_HDMI_ARC")
            if (AudioDeviceInfo.TYPE_IP and it.type != 0) debugLog("         TYPE = TYPE_IP")
            if (AudioDeviceInfo.TYPE_LINE_ANALOG and it.type != 0) debugLog("         TYPE = TYPE_LINE_ANALOG")
            if (AudioDeviceInfo.TYPE_LINE_DIGITAL and it.type != 0) debugLog("         TYPE = TYPE_LINE_DIGITAL")
            if (AudioDeviceInfo.TYPE_TELEPHONY and it.type != 0) debugLog("         TYPE = TYPE_TELEPHONY")
            if (AudioDeviceInfo.TYPE_TV_TUNER and it.type != 0) debugLog("         TYPE = TYPE_TV_TUNER")
            if (AudioDeviceInfo.TYPE_UNKNOWN and it.type != 0) debugLog("         TYPE = TYPE_UNKNOWN")
            if (AudioDeviceInfo.TYPE_USB_ACCESSORY and it.type != 0) debugLog("         TYPE = TYPE_USB_ACCESSORY")
            if (AudioDeviceInfo.TYPE_USB_DEVICE and it.type != 0) debugLog("         TYPE = TYPE_USB_DEVICE")
            if (AudioDeviceInfo.TYPE_USB_HEADSET and it.type != 0) debugLog("         TYPE = TYPE_USB_HEADSET")
            if (AudioDeviceInfo.TYPE_WIRED_HEADPHONES and it.type != 0) debugLog("         TYPE = TYPE_WIRED_HEADPHONES")
            if (AudioDeviceInfo.TYPE_WIRED_HEADSET and it.type != 0) debugLog("         TYPE = TYPE_WIRED_HEADSET")

            var chCnt = ""
            it.channelCounts.forEach { cnt ->
                chCnt += "$cnt, "
            }
            debugLog("         CHANNEL COUNT = $chCnt")

            debugLog("         CHANNEL MASKS :")
            it.channelMasks.forEach { chMask ->
                if (AudioFormat.CHANNEL_IN_BACK and chMask != 0) debugLog("             CHANNEL_IN_BACK")
                if (AudioFormat.CHANNEL_IN_BACK_PROCESSED and chMask != 0) debugLog("             CHANNEL_IN_BACK_PROCESSED")
                if (AudioFormat.CHANNEL_IN_FRONT and chMask != 0) debugLog("             CHANNEL_IN_FRONT")
                if (AudioFormat.CHANNEL_IN_FRONT_PROCESSED and chMask != 0) debugLog("             CHANNEL_IN_FRONT_PROCESSED")
                if (AudioFormat.CHANNEL_IN_LEFT and chMask != 0) debugLog("             CHANNEL_IN_LEFT")
                if (AudioFormat.CHANNEL_IN_LEFT_PROCESSED and chMask != 0) debugLog("             CHANNEL_IN_LEFT_PROCESSED")
                if (AudioFormat.CHANNEL_IN_MONO and chMask != 0) debugLog("             CHANNEL_IN_MONO")
                if (AudioFormat.CHANNEL_IN_PRESSURE and chMask != 0) debugLog("             CHANNEL_IN_PRESSURE")
                if (AudioFormat.CHANNEL_IN_RIGHT and chMask != 0) debugLog("             CHANNEL_IN_RIGHT")
                if (AudioFormat.CHANNEL_IN_RIGHT_PROCESSED and chMask != 0) debugLog("             CHANNEL_IN_RIGHT_PROCESSED")
                if (AudioFormat.CHANNEL_IN_STEREO and chMask != 0) debugLog("             CHANNEL_IN_STEREO")
                if (AudioFormat.CHANNEL_IN_VOICE_DNLINK and chMask != 0) debugLog("             CHANNEL_IN_VOICE_DNLINK")
                if (AudioFormat.CHANNEL_IN_VOICE_UPLINK and chMask != 0) debugLog("             CHANNEL_IN_VOICE_UPLINK")
                if (AudioFormat.CHANNEL_IN_X_AXIS and chMask != 0) debugLog("             CHANNEL_IN_X_AXIS")
                if (AudioFormat.CHANNEL_IN_Y_AXIS and chMask != 0) debugLog("             CHANNEL_IN_Y_AXIS")
                if (AudioFormat.CHANNEL_IN_Z_AXIS and chMask != 0) debugLog("             CHANNEL_IN_Z_AXIS")
            }

            debugLog("         ENCODINGS :")
            it.encodings.forEach { enc ->
                if (AudioFormat.ENCODING_AC3 and enc != 0) debugLog("             ENCODING_AC3")
                if (AudioFormat.ENCODING_DOLBY_TRUEHD and enc != 0) debugLog("             ENCODING_DOLBY_TRUEHD")
                if (AudioFormat.ENCODING_DTS and enc != 0) debugLog("             ENCODING_DTS")
                if (AudioFormat.ENCODING_DTS_HD and enc != 0) debugLog("             ENCODING_DTS_HD")
                if (AudioFormat.ENCODING_E_AC3 and enc != 0) debugLog("             ENCODING_E_AC3")
                if (AudioFormat.ENCODING_IEC61937 and enc != 0) debugLog("             ENCODING_IEC61937")
                if (AudioFormat.ENCODING_PCM_16BIT and enc != 0) debugLog("             ENCODING_PCM_16BIT")
                if (AudioFormat.ENCODING_PCM_8BIT and enc != 0) debugLog("             ENCODING_PCM_8BIT")
                if (AudioFormat.ENCODING_PCM_FLOAT and enc != 0) debugLog("             ENCODING_PCM_FLOAT")
            }

            var samplingRates = ""
            it.sampleRates.forEach { rate ->
                samplingRates += "$rate, "
            }
            debugLog("         SAMPLING RATES = $samplingRates")
        }



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
         * Callback for sound level changed.
         *
         * @param level Current sound level.
         * @param min Minimum sound level.
         * @param max Maximum sound level.
         */
        fun onSoundLevelChanged(level: Int, min: Int, max: Int)

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

        // Create new recording session.
        val isSuccess = createAudioRecord()
        if (!isSuccess) {
            errorLog("Failed to create AudioRecord. Could not activate VoiceRecorder.")
            return
        }

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

        // 1st priority.
        var ar = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                samplingRate,
                CHANNEL,
                ENCODING,
                minBufSize)

        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            ar = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    samplingRate,
                    CHANNEL,
                    ENCODING,
                    minBufSize)
        }

//        if (ar.state != AudioRecord.STATE_INITIALIZED) {
//            ar = AudioRecord(
//                    MediaRecorder.AudioSource.CAMCORDER,
//                    samplingRate,
//                    CHANNEL,
//                    ENCODING,
//                    minBufSize)
//        }
//        if (ar.state != AudioRecord.STATE_INITIALIZED) {
//            ar = AudioRecord(
//                    MediaRecorder.AudioSource.REMOTE_SUBMIX,
//                    samplingRate,
//                    CHANNEL,
//                    ENCODING,
//                    minBufSize)
//        }
//        if (ar.state != AudioRecord.STATE_INITIALIZED) {
//            ar = AudioRecord(
//                    MediaRecorder.AudioSource.UNPROCESSED,
//                    samplingRate,
//                    CHANNEL,
//                    ENCODING,
//                    minBufSize)
//        }
//        if (ar.state != AudioRecord.STATE_INITIALIZED) {
//            ar = AudioRecord(
//                    MediaRecorder.AudioSource.VOICE_CALL,
//                    samplingRate,
//                    CHANNEL,
//                    ENCODING,
//                    minBufSize)
//        }
//        if (ar.state != AudioRecord.STATE_INITIALIZED) {
//            ar = AudioRecord(
//                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
//                    samplingRate,
//                    CHANNEL,
//                    ENCODING,
//                    minBufSize)
//        }
//        if (ar.state != AudioRecord.STATE_INITIALIZED) {
//            ar = AudioRecord(
//                    MediaRecorder.AudioSource.VOICE_DOWNLINK,
//                    samplingRate,
//                    CHANNEL,
//                    ENCODING,
//                    minBufSize)
//        }
//        if (ar.state != AudioRecord.STATE_INITIALIZED) {
//            ar = AudioRecord(
//                    MediaRecorder.AudioSource.VOICE_UPLINK,
//                    samplingRate,
//                    CHANNEL,
//                    ENCODING,
//                    minBufSize)
//        }

        // Fallback to default.
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            ar = AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    samplingRate,
                    CHANNEL,
                    ENCODING,
                    minBufSize)
        }

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

        val lastBufCaches = mutableListOf<ByteArray>()
        val lastBufSizeBytes: Int

        init {
            when (ENCODING) {
                AudioFormat.ENCODING_PCM_16BIT -> {
                    val secBytes = samplingRate * 16 / 8
                    lastBufSizeBytes = (secBytes * (LAST_BUFFER_CACHE_MILLIS / 1000))
                }

                else -> {
                    throw RuntimeException("Unsupported Encoding")
                }
            }
        }

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

                // Cache last buffer.
                val totalCacheBytes = lastBufCaches.sumOf { it ->
                    it.size
                }
                if (lastBufSizeBytes < totalCacheBytes) {
                    // Cache out.
                    lastBufCaches.removeAt(0)
                }
                lastBufCaches.add(buf.copyOf())

                val now = SystemClock.uptimeMillis()

                // Check voice is silence or not.
                if (isSounded(buf, size, ENCODING)) {
                    if (IS_DEBUG) debugLog("VoiceRecorder.AudioProc.run() : SOUNDED")
                    // Voice available.

                    if (soundUpTimeMillis == 0L) {
                        // Voice rec started.
                        onStart(now)
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

        private fun onStart(now: Long) {
            callback?.onStarted(samplingRate)
            soundUpTimeMillis = now

            // Callback last recorded audio frames before speak started.
            lastBufCaches.forEach { buf ->
                callback?.onRecorded(buf, ENCODING, buf.size)
            }
        }

        private fun onEnd() {
            callback?.onStopped()
            soundUpTimeMillis = 0L
            soundDownTimeMillis = Long.MAX_VALUE
        }
    }

    // Detect current sound frame includes sound or not.
    @Suppress("SameParameterValue")
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
            s += abs(buf[i].toInt())

            total += s
        }

        val average = total.toFloat() / size.toFloat()

//        debugLog("VoiceRecorder.isSounded() : Level = $average")

        // Sound level callback.
        callback?.onSoundLevelChanged(
                average.toInt(),
               0,
                (speakThreshold.toFloat() * 5.0f / 4.0f).toInt())

        // Check sound level.
        return average > speakThreshold.toFloat()
    }

}
