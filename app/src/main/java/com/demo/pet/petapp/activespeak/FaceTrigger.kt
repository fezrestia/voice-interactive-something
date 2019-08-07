package com.demo.pet.petapp.activespeak

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.media.ImageReader
import android.view.Surface
import com.demo.pet.petapp.Log
import com.demo.pet.petapp.debugLog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Suppress("ConstantConditionIf")
class FaceTrigger(val context: Context) {

    private var callback: Callback? = null

    interface Callback {
        fun onFaceDetected()
    }

    private val isCameraSupported: Boolean

    private val backHandler: Handler
    private val callbackHandler: Handler

    private val cameraMng: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var previewStream: ImageReader? = null
    private var captureSession: CameraCaptureSession? = null

    init {
        val backThread = HandlerThread("back-worker")
        backThread.start()
        backHandler = Handler(backThread.looper)

        val callbackThread = HandlerThread("callback-worker")
        callbackThread.start()
        callbackHandler = Handler(callbackThread.looper)

        cameraMng = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val ids: Array<String> = cameraMng.cameraIdList
        isCameraSupported = ids.isNotEmpty()
    }

    fun release() {
        backHandler.looper.quitSafely()
        callbackHandler.looper.quitSafely()
    }

    fun resume() {
        if (!isCameraSupported) return

        backHandler.post(OpenTask())
        backHandler.post(StartFaceDetectionTask())

    }

    fun pause() {
        if (!isCameraSupported) return

        backHandler.post(StopFaceDetectionTask())
        backHandler.post(CloseTask())
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    private inner class OpenTask : Runnable {
        val done = CountDownLatch(1)

        @SuppressLint("MissingPermission")
        override fun run() {
            if (Log.IS_DEBUG) debugLog("FaceTrigger.OpenTask : E")

            // Detect front camera.
            val ids: Array<String> = cameraMng.cameraIdList
            var frontCamId = ""
            for (id in ids) {
                val camChars = cameraMng.getCameraCharacteristics(id)
                if (camChars.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT) {
                    // Front.
                    frontCamId = id
                    break
                }
            }
            if (frontCamId.isEmpty()) {
                if (Log.IS_DEBUG) debugLog("Front Camera is not available.")
                frontCamId = ids.first()
            }

            // Request open.
            cameraMng.openCamera(frontCamId, StateCallback(), callbackHandler)

            done.await(5000, TimeUnit.MILLISECONDS)

            if (Log.IS_DEBUG) debugLog("FaceTrigger.OpenTask : X")
        }

        private inner class StateCallback : CameraDevice.StateCallback() {
            override fun onDisconnected(camera: CameraDevice?) {
                if (Log.IS_DEBUG) debugLog("FaceTrigger.OpenTask.onDisconnected()")
            }

            override fun onError(camera: CameraDevice?, error: Int) {
                if (Log.IS_DEBUG) debugLog("FaceTrigger.OpenTask.onError()")
            }

            override fun onOpened(camera: CameraDevice?) {
                if (Log.IS_DEBUG) debugLog("FaceTrigger.OpenTask.onOpened() : E")

                cameraDevice = camera

                done.countDown()

                if (Log.IS_DEBUG) debugLog("FaceTrigger.OpenTask.onOpened() : X")
            }
        }
    }

    private inner class StartFaceDetectionTask : Runnable {
        override fun run() {
            if (Log.IS_DEBUG) debugLog("FaceTrigger.StartFaceDetectionTask : E")

            // Stream.
            previewStream = ImageReader.newInstance(
                    640,
                    480,
                    ImageFormat.PRIVATE,
                    4)
            previewStream?.setOnImageAvailableListener(
                    ImageAvailableCallback(),
                    callbackHandler)

            val streams: List<Surface?> = listOf(previewStream?.surface)

            cameraDevice?.createCaptureSession(
                    streams,
                    CaptureSessionCallback(),
                    callbackHandler)

            if (Log.IS_DEBUG) debugLog("FaceTrigger.StartFaceDetectionTask : X")
        }

        private inner class ImageAvailableCallback : ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(reader: ImageReader?) {
                if (Log.IS_DEBUG) debugLog("FaceTrigger.ImageAvailableCallback")

                val image = reader?.acquireLatestImage()
                image?.close()
            }
        }

        private inner class CaptureSessionCallback : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession?) {

            }

            override fun onConfigured(session: CameraCaptureSession?) {
                val device = cameraDevice ?: throw RuntimeException("cameraDevice is null")
                val ps = previewStream ?: throw RuntimeException("PreviewStream is null")

                captureSession = session

                // Request.
                val builder: CaptureRequest.Builder = device.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW)
                builder.addTarget(ps.surface)
                builder.set(
                        CaptureRequest.CONTROL_MODE,
                        CaptureRequest.CONTROL_MODE_AUTO)
                builder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                builder.set(
                        CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                        CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE)

                val request = builder.build()

                session?.setRepeatingRequest(
                        request,
                        CaptureCallback(),
                        callbackHandler)
            }
        }

        private inner class CaptureCallback : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                    session: CameraCaptureSession?,
                    request: CaptureRequest?,
                    result: TotalCaptureResult?) {
                if (Log.IS_DEBUG) debugLog("FaceTrigger.CaptureCallback.onCaptureCompleted() : E")
                val s = session ?: throw RuntimeException("session is null")
                val req = request ?: throw RuntimeException("request is null")
                val res = result ?: throw RuntimeException("result is null")

                super.onCaptureCompleted(s, req, res)

                val faces = res.get(CaptureResult.STATISTICS_FACES)

                if (Log.IS_DEBUG) debugLog("FaceTrigger DETECTED FACES = " + faces?.size)

                if (faces != null) {
                    if (faces.isNotEmpty()) {
                        callback?.onFaceDetected()
                    }
                }

            }


        }

    }

    private inner class StopFaceDetectionTask : Runnable {
        override fun run() {
            if (Log.IS_DEBUG) debugLog("FaceTrigger.StopFaceDetectionTask : E")

            captureSession?.stopRepeating()

            if (Log.IS_DEBUG) debugLog("FaceTrigger.StopFaceDetectionTask : X")
        }
    }

    private inner class CloseTask : Runnable {
        override fun run() {
            if (Log.IS_DEBUG) debugLog("FaceTrigger.CloseTask : E")

            cameraDevice?.close()
            cameraDevice = null

            if (Log.IS_DEBUG) debugLog("FaceTrigger.CloseTask : X")
        }
    }


}
