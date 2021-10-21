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

package github.nisrulz.qreader;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * QREader Singleton.
 */
public class QREader {

    /**
     * The type Builder.
     */
    public static class Builder {

        private boolean autofocusEnabled;

        private BarcodeDetector barcodeDetector;

        private final Context context;

        private int facing;

        private int height;

        private final QRDataListener qrDataListener;

        private SurfaceView surfaceView;

        private int width;

        /**
         * Instantiates a new Builder.
         *
         * @param context        the context
         * @param qrDataListener the qr data listener
         */
        public Builder(Context context, QRDataListener qrDataListener) {
            this.autofocusEnabled = true;
            this.width = 800;
            this.height = 800;
            this.facing = BACK_CAM;
            this.qrDataListener = qrDataListener;
            this.context = context;
            this.surfaceView = null;
        }

        /**
         * Barcode detector.
         *
         * @param barcodeDetector the barcode detector
         * @return the builder
         */
        public Builder barcodeDetector(BarcodeDetector barcodeDetector) {
            this.barcodeDetector = barcodeDetector;
            return this;
        }

        /**
         * Build QREader
         *
         * @return the QREader
         */
        public QREader build() {
            return new QREader(this);
        }

        /**
         * Enable autofocus builder.
         *
         * @param autofocusEnabled the autofocus enabled
         * @return the builder
         */
        public Builder enableAutofocus(boolean autofocusEnabled) {
            this.autofocusEnabled = autofocusEnabled;
            return this;
        }

        /**
         * Facing builder.
         *
         * @param facing the facing
         * @return the builder
         */
        public Builder facing(int facing) {
            this.facing = facing;
            return this;
        }

        /**
         * Height builder.
         *
         * @param height the height
         * @return the builder
         */
        public Builder height(int height) {
            if (height != 0) {
                this.height = height;
            }
            return this;
        }

        public Builder surfaceView(SurfaceView surfaceView) {
            if (surfaceView != null) {
                this.surfaceView = surfaceView;
            }
            return this;
        }

        /**
         * Width builder.
         *
         * @param width the width
         * @return the builder
         */
        public Builder width(int width) {
            if (width != 0) {
                this.width = width;
            }
            return this;
        }
    }

    /**
     * The constant FRONT_CAM.
     */
    public static final int FRONT_CAM = CameraSource.CAMERA_FACING_FRONT;

    /**
     * The constant BACK_CAM.
     */
    public static final int BACK_CAM = CameraSource.CAMERA_FACING_BACK;

    private final String LOGTAG = getClass().getSimpleName();

    private boolean autoFocusEnabled;

    private BarcodeDetector barcodeDetector = null;

    private boolean cameraRunning = false;

    private CameraSource cameraSource = null;

    private final Context context;

    private final int facing;

    private final int height;

    private final QRDataListener qrDataListener;

    private boolean surfaceCreated = false;

    private final SurfaceView surfaceView;

    private boolean flashOn = false;

    private final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            //we can start barcode after after creating
            surfaceCreated = true;
            try {
                startCameraView(context, cameraSource, surfaceView);
            } catch (Exception e) {
                surfaceCreated = false;
                stop();
                qrDataListener.onReadQrError(e);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            surfaceCreated = false;
            stop();
            surfaceHolder.removeCallback(this);
        }
    };

    private final Utils utils = new Utils();

    private final int width;

    /**
     * Instantiates a new QREader.
     *
     * @param builder the builder
     */
    private QREader(final Builder builder) {
        this.autoFocusEnabled = builder.autofocusEnabled;
        this.width = builder.width;
        this.height = builder.height;
        this.facing = builder.facing;
        this.qrDataListener = builder.qrDataListener;
        this.context = builder.context;
        this.surfaceView = builder.surfaceView;
        //for better performance we should use one detector for all Reader, if builder not specify it
        if (builder.barcodeDetector == null) {
            this.barcodeDetector = BarcodeDetectorHolder.getBarcodeDetector(context);
        } else {
            this.barcodeDetector = builder.barcodeDetector;
        }
    }

    public Bitmap getBitmapFromDrawable(int resId) {
        return BitmapFactory.decodeResource(context.getResources(), resId);
    }

    public void initAndStart(final SurfaceView surfaceView) {
        surfaceView.getViewTreeObserver()
            .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    init();
                    start();
                    utils.removeOnGlobalLayoutListener(surfaceView, this);
                }
            });
    }

    /**
     * Is camera running boolean.
     *
     * @return the boolean
     */
    public boolean isCameraRunning() {
        return cameraRunning;
    }

    public void readFromBitmap(Bitmap bitmap) {
        if (!barcodeDetector.isOperational()) {
            Log.d(LOGTAG, "Could not set up the detector!");
            return;
        } else {
            try {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<Barcode> barcodes = barcodeDetector.detect(frame);
                if (barcodes.size() != 0 && qrDataListener != null) {
                    qrDataListener.onDetected(barcodes.valueAt(0).rawValue);
                }
            } catch (Exception e) {
                qrDataListener.onReadQrError(e);
            }
        }

    }

    /**
     * Release and cleanup QREader.
     */
    public void releaseAndCleanup() {
        stop();
        if (cameraSource != null) {
            //release camera and barcode detector(will invoke inside) resources
            cameraSource.release();
            cameraSource = null;
        }
    }

    /**
     * Start scanning qr codes.
     */
    public void start() {
        if (surfaceView != null && surfaceHolderCallback != null) {
            //if surface already created, we can start camera
            if (surfaceCreated) {
                startCameraView(context, cameraSource, surfaceView);
            } else {
                //startCameraView will be invoke in void surfaceCreated
                surfaceView.getHolder().addCallback(surfaceHolderCallback);
            }
        }
    }

    /**
     * Stop camera
     */
    public void stop() {
        try {
            if (cameraRunning && cameraSource != null) {
                cameraSource.stop();
                cameraRunning = false;
            }
        } catch (Exception ie) {
            Log.e(LOGTAG, ie.getMessage());
            ie.printStackTrace();
        }
    }

    private CameraSource getCameraSource() {
        if (cameraSource == null) {
            cameraSource =
                new CameraSource.Builder(context, barcodeDetector).setAutoFocusEnabled(autoFocusEnabled)
                    .setFacing(facing)
                    .setRequestedPreviewSize(width, height)
                    .build();
        }
        return cameraSource;
    }

    /**
     * Init.
     */
    private void init() {
        if (!utils.hasAutofocus(context)) {
            Log.e(LOGTAG, "Do not have autofocus feature, disabling autofocus feature in the library!");
            autoFocusEnabled = false;
        }

        if (!utils.hasCameraHardware(context)) {
            Log.e(LOGTAG, "Does not have camera hardware!");
            return;
        }
        if (!utils.checkCameraPermission(context)) {
            Log.e(LOGTAG, "Do not have camera permission!");
            return;
        }

        if (barcodeDetector.isOperational()) {
            barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
                @Override
                public void receiveDetections(Detector.Detections<Barcode> detections) {
                    final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                    if (barcodes.size() != 0 && qrDataListener != null) {
                        qrDataListener.onDetected(barcodes.valueAt(0).displayValue);
                    }
                }

                @Override
                public void release() {
                    // Handled via public method
                }
            });

            cameraSource = getCameraSource();
        } else {
            Log.e(LOGTAG, "Barcode recognition libs are not downloaded and are not operational");
        }
    }

    private void startCameraView(Context context, CameraSource cameraSource,
                                 SurfaceView surfaceView) {
        if (cameraRunning) {
            throw new IllegalStateException("Camera already started!");
        }
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(LOGTAG, "Permission not granted!");
            } else if (!cameraRunning && cameraSource != null && surfaceView != null) {
                cameraSource.start(surfaceView.getHolder());
                cameraRunning = true;
            }
        } catch (IOException ie) {
            Log.e(LOGTAG, ie.getMessage());
            ie.printStackTrace();
        }
    }

    /**
     * Turn Flash On/Off
     */
    public void toggleFlash() {
        if (cameraSource == null)
            return;

        final Camera camera = getCamera(cameraSource);
        if (camera != null) {
            try {
                final Camera.Parameters param = camera.getParameters();
                if (flashOn) {
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                } else {
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
                camera.setParameters(param);
                flashOn = !flashOn;
            } catch (final RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isFlashOn() {
        return this.flashOn;
    }

    private static Camera getCamera(CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        return camera;
                    }
                    return null;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return null;
    }
}

