@file:Suppress(
        "PrivatePropertyName",
        "SimplifyBooleanWithConstants",
        "ConstantConditionIf")

package com.demo.pet.petapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout

import kotlinx.android.synthetic.main.overlay_root_view.view.*

import com.demo.pet.petapp.conversations.ConversationStrategy
import com.demo.pet.petapp.conversations.ConversationType
import com.demo.pet.petapp.conversations.createConversationStrategy
import com.demo.pet.petapp.stt.STTController
import com.demo.pet.petapp.stt.STTType
import com.demo.pet.petapp.stt.createSTTController
import com.demo.pet.petapp.tts.TTSController
import com.demo.pet.petapp.tts.TTSType
import com.demo.pet.petapp.tts.createTTSController
import com.demo.pet.petapp.activespeak.FaceTrigger

class OverlayRootView : RelativeLayout {

    private val IS_DEBUG = Log.IS_DEBUG || false

    private val winMng: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val winParams: WindowManager.LayoutParams

    private val ttsCtrl: TTSController
    private val sttCtrl: STTController

    private val conversation: ConversationStrategy

    private val soundPool: SoundPool
    private val soundWan: Int
    private val soundKuun: Int

    companion object {
        private var isOverlayActive = false

        fun isActive(): Boolean {
            return isOverlayActive
        }
    }

    private lateinit var pet: Pet
    private lateinit var renderer: RenderingTask

    private val uiHandler: Handler

    private var faceTrigger: FaceTrigger? = null

    init {
        var overlayType: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        // Immersive mode.
        systemUiVisibility = (SYSTEM_UI_FLAG_FULLSCREEN
                or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_IMMERSIVE)

        winParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT)
        winParams.gravity = Gravity.BOTTOM
        winParams.y = 0

        // UI thread.
        uiHandler = Handler()

        // TTS
        val ttsType = PetApplication.getSP().getString(
                Constants.KEY_TTS_TYPE,
                TTSType.ANDROID.toString()) as String
        ttsCtrl = createTTSController(
                context,
                TTSType.valueOf(ttsType),
                TTSCallbackImpl())

        // STT
        val sttType = PetApplication.getSP().getString(
                Constants.KEY_STT_TYPE,
                STTType.GOOGLE_WEB_API.toString()) as String
        sttCtrl = createSTTController(
                context,
                STTType.valueOf(sttType),
                STTCallbackImpl())

        // Strategy.
        val convType = when(STTType.valueOf(sttType)) {
            STTType.GOOGLE_WEB_API -> {
                ConversationType.USER_DEF
            }
            STTType.ANDROID_SPEECH_RECOGNIZER, STTType.POCKET_SPHINX -> {
                ConversationType.OHAYOU_KATCHY
            }
        }
        conversation = createConversationStrategy(context, convType)

        sttCtrl.registerKeywords(conversation.getFilterKeywords())

        // Start.
        sttCtrl.prepare()
        sttCtrl.startRecog()

        // Sound.
        val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        soundPool = SoundPool.Builder()
                .setAudioAttributes(audioAttr)
                .setMaxStreams(2)
                .build()
        soundPool.setOnLoadCompleteListener { _, sampleId, _ ->
            if (IS_DEBUG) debugLog("SoundPool.onLoadComplete() : ID=$sampleId")
        }

        soundWan = soundPool.load(context, R.raw.wan_wan, 1)
        soundKuun = soundPool.load(context, R.raw.wan_kuun, 1)
    }

    constructor(context: Context) : super(context) {
        if (IS_DEBUG) debugLog("OverlayRootView.CONSTRUCTOR_1")
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        if (IS_DEBUG) debugLog("OverlayRootView.CONSTRUCTOR_2")
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        if (IS_DEBUG) debugLog("OverlayRootView.CONSTRUCTOR_3")
    }

    fun initialize() {
        // Cache instances.
        pet = Pet(pet_icon)
        pet.sit()

        // Renderer.
        renderer = RenderingTask(pet, uiHandler)
        renderer.start()

    }

    fun release() {
        renderer.stop()
        ttsCtrl.release()
        conversation.release()
        sttCtrl.stopRecog()
        sttCtrl.release()
        soundPool.release()
    }

    fun addToOverlayWindow() {
        isOverlayActive = true
        winMng.addView(this, winParams)
    }

    fun removeFromOverlayWindow() {
        winMng.removeView(this)
        isOverlayActive = false
    }

    fun setFaceTrigger(trigger: FaceTrigger?) {
        if (trigger == null) {
            faceTrigger?.setCallback(null)
        } else {
            trigger.setCallback(FaceTriggerCallback())
        }
        faceTrigger = trigger
    }

    var lastFaceDetectedTimeMillis : Long = 0
    val faceDetectionTimeoutMillis : Long = 60 * 1000

    private inner class FaceTriggerCallback : FaceTrigger.Callback {
        override fun onFaceDetected() {

            val now = System.currentTimeMillis()
            if (now - lastFaceDetectedTimeMillis > faceDetectionTimeoutMillis) {


                lastFaceDetectedTimeMillis = now


                ttsCtrl.speak(context.getString(R.string.key_a))


            }


        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN) {
            if (IS_DEBUG) debugLog("onKeyDown() : KEYCODE = ${event.displayLabel}")

            when (event.keyCode) {
                KeyEvent.KEYCODE_Q -> { ttsCtrl.speak(context.getString(R.string.key_q)) }
                KeyEvent.KEYCODE_W -> { ttsCtrl.speak(context.getString(R.string.key_w)) }
                KeyEvent.KEYCODE_E -> { ttsCtrl.speak(context.getString(R.string.key_e)) }
                KeyEvent.KEYCODE_R -> { ttsCtrl.speak(context.getString(R.string.key_r)) }
                KeyEvent.KEYCODE_T -> { ttsCtrl.speak(context.getString(R.string.key_t)) }

                KeyEvent.KEYCODE_A -> { ttsCtrl.speak(context.getString(R.string.key_a)) }
                KeyEvent.KEYCODE_S -> { ttsCtrl.speak(context.getString(R.string.key_s)) }
                KeyEvent.KEYCODE_D -> { ttsCtrl.speak(context.getString(R.string.key_d)) }
                KeyEvent.KEYCODE_F -> { ttsCtrl.speak(context.getString(R.string.key_f)) }
                KeyEvent.KEYCODE_G -> { ttsCtrl.speak(context.getString(R.string.key_g)) }
                KeyEvent.KEYCODE_H -> { ttsCtrl.speak(context.getString(R.string.key_h)) }
                KeyEvent.KEYCODE_J -> { ttsCtrl.speak(context.getString(R.string.key_j)) }
                KeyEvent.KEYCODE_K -> { ttsCtrl.speak(context.getString(R.string.key_k)) }
                KeyEvent.KEYCODE_L -> { ttsCtrl.speak(context.getString(R.string.key_l)) }

                KeyEvent.KEYCODE_Z -> { ttsCtrl.speak(context.getString(R.string.key_z)) }

                KeyEvent.KEYCODE_1 -> { soundPool.play(soundWan, 1.0f, 1.0f, 0, 0, 1.0f) }
                KeyEvent.KEYCODE_2 -> { soundPool.play(soundKuun, 1.0f, 1.0f, 0, 0, 1.0f) }

                KeyEvent.KEYCODE_5 -> { ttsCtrl.speak(context.getString(R.string.key_5)) }

                // Life-Cycle.
                KeyEvent.KEYCODE_0 -> { MainActivity.togglePet(false, context) }
                KeyEvent.KEYCODE_8 -> {
                    val intent = Intent(context, BlackScreenActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(intent)
                }
                KeyEvent.KEYCODE_9 -> { BlackScreenActivity.finishAll() }
                KeyEvent.KEYCODE_7 -> {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(intent)
                }

                else -> {
                    if (IS_DEBUG) debugLog("onKeyDown() : OTHER")
                    return false
                }
            }
        }

        return true
    }

    private inner class TTSCallbackImpl : TTSController.Callback {
        override fun onSpeechStarted() {
            pet.startSpeak()
        }

        override fun onSpeechDone(isSucceeded: Boolean) {
            pet.stopSpeak()
        }
    }

    private inner class STTCallbackImpl : STTController.Callback {
        override fun onDetecting(detecting: String) {
            debug_msg.text = detecting
        }

        override fun onDetected(sentence: String, keywords: List<String>) {
            val outText = conversation.conversate(sentence, keywords)
            ttsCtrl.speak(outText)
        }

        override fun onSoundRecStarted() {
            voice_level.setBackgroundColor(Color.RED)
        }

        override fun onSoundRecStopped() {
            voice_level.setBackgroundColor(Color.WHITE)
        }

        override fun onSoundLevelChanged(level: Int, min: Int, max: Int) {
            val rate = level.toFloat() / (max.toFloat() - min.toFloat())
            voice_level.pivotX = 0.0f
            voice_level.scaleX = rate
        }

    }

    private class Pet(val targetView: ImageView) {
        val standDrawable = R.drawable.dog_stand_4
        val sitDrawable = R.drawable.dog_sit
        val sitSpeakDrawable = R.drawable.dog_sit_speaking

        var currentDrawable = sitDrawable

        var isSpeaking = false

        init {
            targetView.setImageResource(currentDrawable)
        }

        private fun draw() {
            targetView.setImageResource(currentDrawable)
        }

//        fun stand() {
//            if (currentDrawable != standDrawable) {
//                currentDrawable = standDrawable
//                draw()
//            }
//        }

        fun sit() {
            if (currentDrawable != sitDrawable) {
                currentDrawable = sitDrawable
                draw()
            }
        }

        fun sitSpeak() {
            if (currentDrawable != sitSpeakDrawable) {
                currentDrawable = sitSpeakDrawable
                draw()
            }
        }

        fun render(count: Long) {
            if (isSpeaking) {
                if ((count / 5L) % 2 == 0L) {
                    sit()
                } else {
                    sitSpeak()
                }
            } else {
                sit()
            }

//            if ((count / 30) % 2 == 0L) {
//                if (IS_DEBUG) debugLog("pet.sit()")
//                pet.sit()
//            } else {
//                if (IS_DEBUG) debugLog("pet.stand()")
//                pet.stand()
//            }

        }

        fun startSpeak() {
            isSpeaking = true
        }

        fun stopSpeak() {
            isSpeaking = false
        }
    }

    private inner class RenderingTask(val pet: Pet, val handler: Handler) : Runnable {
        private var isActive = true
        private val INTERVAL = 1000L / 30
        private var count: Long = 0

        fun start() {
            handler.postDelayed(this, INTERVAL)
        }

        fun stop() {
            isActive = false
            handler.removeCallbacks(this)
        }

        override fun run() {
            if (IS_DEBUG) debugLog("renderer.run() : E")

            pet.render(count)

            ++count

            if (isActive) {
                handler.postDelayed(this, INTERVAL)
            }
            if (IS_DEBUG) debugLog("renderer.run() : X")
        }

    }

}
