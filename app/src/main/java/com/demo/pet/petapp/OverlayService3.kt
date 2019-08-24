@file:Suppress(
        "PrivatePropertyName",
        "ConstantConditionIf",
        "PropertyName")

package com.demo.pet.petapp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.demo.pet.petapp.activespeak.FaceTrigger
import com.demo.pet.petapp.character.Character
import com.demo.pet.petapp.character.CharacterType
import com.demo.pet.petapp.character.createCharacter
import com.demo.pet.petapp.conversations.ConversationStrategy
import com.demo.pet.petapp.conversations.ConversationType
import com.demo.pet.petapp.conversations.createConversationStrategy
import com.demo.pet.petapp.stt.STTController
import com.demo.pet.petapp.stt.STTType
import com.demo.pet.petapp.stt.createSTTController
import com.demo.pet.petapp.tts.TTSController
import com.demo.pet.petapp.tts.TTSType
import com.demo.pet.petapp.tts.createTTSController
import com.demo.pet.petapp.util.Log
import com.demo.pet.petapp.util.debugLog

const val REQUEST_START_OVERLAY_3 = "com.demo.pet.petapp.action.REQUEST_START_OVERLAY_3"
const val REQUEST_STOP_OVERLAY_3 = "com.demo.pet.petapp.action.REQUEST_STOP_OVERLAY_3"

class OverlayService3 : Service() {
    @Suppress("SimplifyBooleanWithConstants")
    private val IS_DEBUG = Log.IS_DEBUG || false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val NOTIFICATION_CHANNEL_ONGOING = "ongoing"
    private val NOTIFICATION_ID = 1003

    private var chara: Character? = null

    private var itx: ConversationStrategy? = null

    private var tts: TTSController? = null
    private var stt: STTController? = null

    private var faceTrigger: FaceTrigger? = null

    @SuppressLint("NewApi")
    override fun onCreate() {
        if (IS_DEBUG) debugLog("OverlayService3.onCreate()")
        super.onCreate()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ONGOING,
                    "overlay on-going",
                    NotificationManager.IMPORTANCE_MIN)
            manager.createNotificationChannel(channel)

            Notification.Builder(this, NOTIFICATION_CHANNEL_ONGOING)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
                .setContentTitle(getText(R.string.app_name))
                .setSmallIcon(R.drawable.dog_sit)
                .build()

        // Foreground service.
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (IS_DEBUG) debugLog("OverlayService3.onStartCommand()")

        val action = intent.action

        if (action == null) {
            if (IS_DEBUG) debugLog("Unexpected Intent action = null")
        } else when (action) {
            REQUEST_START_OVERLAY_3 -> {
                startKatchy()
            }
            REQUEST_STOP_OVERLAY_3 -> {
                stopKatchy()
            }
            else -> {
                throw RuntimeException("Unexpected action = $action")
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (IS_DEBUG) debugLog("OverlayService3.onDestroy()")
        super.onDestroy()

        stopForeground(true)

    }

    private fun startKatchy() {
        if (IS_DEBUG) debugLog("OverlayService3.startKatchy() : E")

        PetApplication.isKatchy3Active = true

        // TTS
        val ttsType = PetApplication.getSP().getString(
                Constants.KEY_TTS_TYPE,
                TTSType.ANDROID.toString()) as String
        val ttsTypePackage = PetApplication.getSP().getString(
                Constants.KEY_TTS_TYPE_OPTION_PACKAGE,
                Constants.VAL_DEFAULT) as String
        tts = createTTSController(
                this,
                TTSType.valueOf(ttsType),
                ttsTypePackage,
                TTSCallbackImpl())

        // STT
        val sttType = PetApplication.getSP().getString(
                Constants.KEY_STT_TYPE,
                STTType.GOOGLE_CLOUD_PLATFORM.toString()) as String
        stt = createSTTController(
                this,
                STTType.valueOf(sttType),
                STTCallbackImpl())

        // Strategy.
        val conversationType = PetApplication.getSP().getString(
                Constants.KEY_CONVERSATION_TYPE,
                ConversationType.USER_DEF.toString()) as String
        itx = createConversationStrategy(
                this,
                ConversationType.valueOf(conversationType))

        val keywords = itx?.getFilterKeywords()
        if (keywords != null) {
            stt?.registerKeywords(keywords)
        }

        // Start.
        stt?.prepare()
        stt?.startRecog()

        // Character.
        val charaTypeString = PetApplication.getSP().getString(
                Constants.KEY_CHARACTER_TYPE,
                CharacterType.KATCHY_DOG.toString()) as String
        val charaType = CharacterType.valueOf(charaTypeString)
        chara = createCharacter(this, charaType)
        chara?.initialize()
        chara?.addToOverlayWindow()

        // External triggers.
//        faceTrigger = FaceTrigger(this)
        faceTrigger?.resume()
        faceTrigger?.setCallback(FaceTriggerCallback())

        if (IS_DEBUG) debugLog("OverlayService3.startKatchy() : X")
    }

    private fun stopKatchy() {
        if (IS_DEBUG) debugLog("OverlayService3.stopKatchy() : E")

        if (chara?.isOnOverlay == true) {
            chara?.removeFromOverlayWindow()
        }
        chara?.release()
        chara = null

        faceTrigger?.setCallback(null)
        faceTrigger?.pause()
        faceTrigger?.release()
        faceTrigger = null

        tts?.release()
        tts = null

        stt?.stopRecog()
        stt?.release()
        stt = null

        itx?.release()
        itx = null

        stopSelf()

        PetApplication.isKatchy3Active = false

        if (IS_DEBUG) debugLog("OverlayService3.stopKatchy() : X")
    }

    private inner class TTSCallbackImpl : TTSController.Callback {
        override fun onSpeechStarted() {
            // Katchy speaking voice is also recognized. So, stop recognition during speaking.
            stt?.pauseRecog()

            chara?.startSpeak()
        }

        override fun onSpeechDone(isSucceeded: Boolean) {
            chara?.stopSpeak()

            // During speaking, voice recognition is stopped.
            stt?.resumeRecog()
        }
    }

    private inner class STTCallbackImpl : STTController.Callback {
        override fun onDetecting(detecting: String) {
            chara?.updateDebugMsg(detecting)
        }

        override fun onDetected(sentence: String, keywords: List<String>) {
            if (tts?.isSpeaking ?: return) {
                // NOP. Now on speaking.
            } else {
                itx?.asyncConversate(
                        sentence,
                        keywords,
                        ConversationCallbackImpl(),
                        null)
            }
        }

        private inner class ConversationCallbackImpl : ConversationStrategy.Callback {
            override fun onCompleted(response: String) {
                tts?.speak(response)
            }
        }

        override fun onSoundRecStarted() {
            chara?.changeVoiceLevelToRec()
        }

        override fun onSoundRecStopped() {
            chara?.changeVoiceLevelToIdle()
        }

        override fun onSoundLevelChanged(level: Int, min: Int, max: Int) {
            chara?.updateVoiceLevel(level, min, max)
        }

    }

    var lastFaceDetectedTimeMillis : Long = 0
    val FACE_DETECTION_TIMEOUT_MILLIS : Long = 60 * 1000

    private inner class FaceTriggerCallback : FaceTrigger.Callback {
        override fun onFaceDetected() {
            val now = System.currentTimeMillis()
            if (now - lastFaceDetectedTimeMillis > FACE_DETECTION_TIMEOUT_MILLIS) {
                lastFaceDetectedTimeMillis = now

                tts?.speak(this@OverlayService3.getString(R.string.face_trigger_msg))

            }
        }
    }

}
