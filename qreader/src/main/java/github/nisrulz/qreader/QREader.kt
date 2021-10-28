/*
 * Copyright (C) 2016 Nishant Srivastava
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package github.nisrulz.qreader

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import github.nisrulz.qreader.BarcodeDetectorHolder.getBarcodeDetector
import java.io.IOException

/**
 * QREader Singleton.
 */
class QREader private constructor(builder: Builder) {
    /**
     * The type Builder.
     */
    class Builder(ctx: Context, qrDataListener: QRDataListener) {
        var autofocusEnabled = true
        var barcodeDetector: BarcodeDetector? = null
        val context: Context
        var facing: Int
        var height = 800
        val qrDataListener: QRDataListener
        var surfaceView: SurfaceView?
        var width = 800

        /**
         * Barcode detector.
         *
         * @param barcodeDetector the barcode detector
         * @return the builder
         */
        fun barcodeDetector(barcodeDetector: BarcodeDetector?): Builder {
            this.barcodeDetector = barcodeDetector
            return this
        }

        /**
         * Build QREader
         *
         * @return the QREader
         */
        fun build(): QREader {
            return QREader(this)
        }

        /**
         * Enable autofocus builder.
         *
         * @param autofocusEnabled the autofocus enabled
         * @return the builder
         */
        fun enableAutofocus(autofocusEnabled: Boolean): Builder {
            this.autofocusEnabled = autofocusEnabled
            return this
        }

        /**
         * Facing builder.
         *
         * @param facing the facing
         * @return the builder
         */
        fun facing(facing: Int): Builder {
            this.facing = facing
            return this
        }

        /**
         * Height builder.
         *
         * @param height the height
         * @return the builder
         */
        fun height(height: Int): Builder {
            if (height != 0) {
                this.height = height
            }
            return this
        }

        fun surfaceView(surfaceView: SurfaceView?): Builder {
            if (surfaceView != null) {
                this.surfaceView = surfaceView
            }
            return this
        }

        /**
         * Width builder.
         *
         * @param width the width
         * @return the builder
         */
        fun width(width: Int): Builder {
            if (width != 0) {
                this.width = width
            }
            return this
        }

        /**
         * Instantiates a new Builder.
         *
         * @param context        the context
         * @param qrDataListener the qr data listener
         */
        init {
            facing = BACK_CAM
            this.qrDataListener = qrDataListener
            this.context = ctx
            surfaceView = null
        }
    }

    private val LOGTAG = javaClass.simpleName
    private var autoFocusEnabled: Boolean
    private var barcodeDetector: BarcodeDetector? = null

    /**
     * Is camera running boolean.
     *
     * @return the boolean
     */
    var isCameraRunning = false
        private set
    private var cameraSource: CameraSource? = null
    private var context: Context = builder.context
    private val facing: Int
    private val height: Int
    private var qrDataListener: QRDataListener? = builder.qrDataListener
    private var surfaceCreated = false
    private var surfaceView: SurfaceView? = null
    var isFlashOn = false
        private set

    private val surfaceHolderCallback: SurfaceHolder.Callback? = object : SurfaceHolder.Callback {
        override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {}
        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
            //we can start barcode after after creating
            surfaceCreated = true
            try {
                startCameraView(context, cameraSource, surfaceView)
            } catch (e: Exception) {
                surfaceCreated = false
                stop()
                qrDataListener?.onReadQrError(e)
            }
        }

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
            surfaceCreated = false
            stop()
            surfaceHolder.removeCallback(this)
        }
    }
    private val utils = Utils()
    private val width: Int
    fun getBitmapFromDrawable(resId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resId)
    }

    fun initAndStart(surfaceView: SurfaceView) {
        surfaceView.viewTreeObserver
            .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    init()
                    start()
                    utils.removeOnGlobalLayoutListener(surfaceView, this)
                }
            })
    }

    fun readFromBitmap(bitmap: Bitmap?) {
        if (barcodeDetector?.isOperational?.not() == true) {
            Log.d(LOGTAG, "Could not set up the detector!")
            return
        } else {
            try {
                val frame = Frame.Builder().setBitmap(bitmap).build()
                val barcodes = barcodeDetector?.detect(frame)
                if (barcodes?.size() != 0 && qrDataListener != null) {
                    qrDataListener?.onDetected(barcodes?.valueAt(0)?.rawValue)
                }
            } catch (e: Exception) {
                qrDataListener?.onReadQrError(e)
            }
        }
    }

    /**
     * Release and cleanup QREader.
     */
    fun releaseAndCleanup() {
        stop()
        if (cameraSource != null) {
            //release camera and barcode detector(will invoke inside) resources
            cameraSource?.release()
            cameraSource = null
        }
    }

    /**
     * Start scanning qr codes.
     */
    fun start() {
        if (surfaceView != null && surfaceHolderCallback != null) {
            //if surface already created, we can start camera
            if (surfaceCreated) {
                startCameraView(context, cameraSource, surfaceView)
            } else {
                //startCameraView will be invoke in void surfaceCreated
                surfaceView?.holder?.addCallback(surfaceHolderCallback)
            }
        }
    }

    /**
     * Stop camera
     */
    fun stop() {
        try {
            if (isCameraRunning && cameraSource != null) {
                cameraSource?.stop()
                isCameraRunning = false
            }
        } catch (ie: Exception) {
            ie.message?.let { Log.e(LOGTAG, it) }
            ie.printStackTrace()
        }
    }

    private fun getCameraSource(): CameraSource? {
        if (cameraSource == null) {
            cameraSource =
                CameraSource.Builder(context, barcodeDetector).setAutoFocusEnabled(autoFocusEnabled)
                    .setFacing(facing)
                    .setRequestedPreviewSize(width, height)
                    .build()
        }
        return cameraSource
    }

    /**
     * Init.
     */
    private fun init() {
        if (!utils.hasAutofocus(context)) {
            Log.e(
                LOGTAG,
                "Do not have autofocus feature, disabling autofocus feature in the library!"
            )
            autoFocusEnabled = false
        }
        if (!utils.hasCameraHardware(context)) {
            Log.e(LOGTAG, "Does not have camera hardware!")
            return
        }
        if (!utils.checkCameraPermission(context)) {
            Log.e(LOGTAG, "Do not have camera permission!")
            return
        }
        if (barcodeDetector?.isOperational == true) {
            barcodeDetector?.setProcessor(object : Detector.Processor<Barcode> {
                override fun receiveDetections(detections: Detections<Barcode>) {
                    val barcodes = detections.detectedItems
                    if (barcodes.size() != 0 && qrDataListener != null) {
                        qrDataListener?.onDetected(barcodes.valueAt(0).displayValue)
                    }
                }

                override fun release() {
                    // Handled via public method
                }
            })
            cameraSource = getCameraSource()
        } else {
            Log.e(LOGTAG, "Barcode recognition libs are not downloaded and are not operational")
        }
    }

    private fun startCameraView(
        context: Context, cameraSource: CameraSource?,
        surfaceView: SurfaceView?
    ) {
        check(!isCameraRunning) { "Camera already started!" }
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(LOGTAG, "Permission not granted!")
            } else if (!isCameraRunning && cameraSource != null && surfaceView != null) {
                cameraSource.start(surfaceView.holder)
                isCameraRunning = true
            }
        } catch (ie: IOException) {
            ie.message?.let { Log.e(LOGTAG, it) }
            ie.printStackTrace()
        }
    }

    /**
     * Turn Flash On/Off
     */
    fun toggleFlash() {
        cameraSource?.let {
            val camera = getCamera(it)
            if (camera != null) {
                try {
                    val param = camera.parameters
                    if (isFlashOn) {
                        param.flashMode = Camera.Parameters.FLASH_MODE_OFF
                    } else {
                        param.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                    }
                    camera.parameters = param
                    isFlashOn = !isFlashOn
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        /**
         * The constant FRONT_CAM.
         */
        const val FRONT_CAM = CameraSource.CAMERA_FACING_FRONT

        /**
         * The constant BACK_CAM.
         */
        const val BACK_CAM = CameraSource.CAMERA_FACING_BACK
        private fun getCamera(cameraSource: CameraSource): Camera? {
            val declaredFields = CameraSource::class.java.declaredFields
            for (field in declaredFields) {
                if (field.type == Camera::class.java) {
                    field.isAccessible = true
                    try {
                        val camera = field[cameraSource] as Camera
                        return if (camera != null) {
                            camera
                        } else null
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    }
                    break
                }
            }
            return null
        }
    }

    /**
     * Instantiates a new QREader.
     *
     * @param builder the builder
     */
    init {
        autoFocusEnabled = builder.autofocusEnabled
        width = builder.width
        height = builder.height
        facing = builder.facing
        qrDataListener = builder.qrDataListener
        context = builder.context
        surfaceView = builder.surfaceView
        //for better performance we should use one detector for all Reader, if builder not specify it
        if (builder.barcodeDetector == null) {
            barcodeDetector = getBarcodeDetector(context)
        } else {
            barcodeDetector = builder.barcodeDetector
        }
    }
}