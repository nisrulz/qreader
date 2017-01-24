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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import java.io.IOException;

/**
 * QREader Singleton.
 */
public class QREader {
  private final String LOGTAG = getClass().getSimpleName();
  private CameraSource cameraSource = null;
  private BarcodeDetector barcodeDetector = null;

  /**
   * The constant FRONT_CAM.
   */
  public static final int FRONT_CAM = CameraSource.CAMERA_FACING_FRONT;
  /**
   * The constant BACK_CAM.
   */
  public static final int BACK_CAM = CameraSource.CAMERA_FACING_BACK;

  private final int width;
  private final int height;
  private final int facing;
  //Changed the qrDataListener from QRDataListenr to QrBarcodeListener
  private final QRBarcodeListener qrDataListener;
  private final Context context;
  private final SurfaceView surfaceView;
  private boolean autoFocusEnabled;

  private boolean cameraRunning = false;

  private boolean surfaceCreated = false;

  public void initAndStart(final SurfaceView surfaceView) {

    surfaceView.getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            init();
            start();
            removeOnGlobalLayoutListener(surfaceView, this);
          }
        });
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private static void removeOnGlobalLayoutListener(View v,
      ViewTreeObserver.OnGlobalLayoutListener listener) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
      v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
    }
    else {
      v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
    }
  }

  private final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
      //we can start barcode after after creating
      surfaceCreated = true;
      startCameraView(context, cameraSource, surfaceView);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
      surfaceCreated = false;
      stop();
      surfaceHolder.removeCallback(this);
    }
  };

  /**
   * Instantiates a new QREader.
   *
   * @param builder
   *     the builder
   */
/*
   * Instantiates a new QREader
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
    }
    else {
      this.barcodeDetector = builder.barcodeDetector;
    }
  }

  /**
   * Is camera running boolean.
   *
   * @return the boolean
   */
  public boolean isCameraRunning() {
    return cameraRunning;
  }

  /**
   * Init.
   */
  private void init() {
    if (!hasAutofocus(context)) {
      Log.e(LOGTAG, "Do not have autofocus feature, disabling autofocus feature in the library!");
      autoFocusEnabled = false;
    }

    if (!hasCameraHardware(context)) {
      Log.e(LOGTAG, "Does not have camera hardware!");
      return;
    }
    if (!checkCameraPermission(context)) {
      Log.e(LOGTAG, "Do not have camera permission!");
      return;
    }

    if (barcodeDetector.isOperational()) {
      barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
        @Override
        public void release() {
          // Handled via public method
        }

        @Override
        public void receiveDetections(Detector.Detections<Barcode> detections) {
          final SparseArray<Barcode> barcodes = detections.getDetectedItems();
          if (barcodes.size() != 0 && qrDataListener != null) {
            //Instead of just sending the string, the complete Barcode object is sent
            qrDataListener.onDetected(barcodes.valueAt(0));
          }
        }
      });

      cameraSource =
          new CameraSource.Builder(context, barcodeDetector).setAutoFocusEnabled(autoFocusEnabled)
              .setFacing(facing)
              .setRequestedPreviewSize(width, height)
              .build();
    }
    else {
      Log.e(LOGTAG, "Barcode recognition libs are not downloaded and are not operational");
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
      }
      else {
        //startCameraView will be invoke in void surfaceCreated
        surfaceView.getHolder().addCallback(surfaceHolderCallback);
      }
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
      }
      else if (!cameraRunning && cameraSource != null && surfaceView != null) {
        cameraSource.start(surfaceView.getHolder());
        cameraRunning = true;
      }
    } catch (IOException ie) {
      Log.e(LOGTAG, ie.getMessage());
      ie.printStackTrace();
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

  private boolean checkCameraPermission(Context context) {
    String permission = Manifest.permission.CAMERA;
    int res = context.checkCallingOrSelfPermission(permission);
    return res == PackageManager.PERMISSION_GRANTED;
  }

  private boolean hasCameraHardware(Context context) {
    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
  }

  private boolean hasAutofocus(Context context) {
    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
  }

  /**
   * The type Builder.
   */
  public static class Builder {
    private boolean autofocusEnabled;
    private int width;
    private int height;
    private int facing;
    //Changed the qrDataListener from QRDataListenr to QrBarcodeListener
    private final QRBarcodeListener qrDataListener;
    private final Context context;
    private final SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;

    /**
     * Instantiates a new Builder.
     *
     * @param context
     *     the context
     * @param surfaceView
     *     the surface view
     * @param qrDataListener
     *     the qr data listener
     */
    //Changed the qrDataListener from QRDataListenr to QrBarcodeListener
    public Builder(Context context, SurfaceView surfaceView, QRBarcodeListener qrDataListener) {
      this.autofocusEnabled = true;
      this.width = 800;
      this.height = 800;
      this.facing = BACK_CAM;
      this.qrDataListener = qrDataListener;
      this.context = context;
      this.surfaceView = surfaceView;
    }

    /**
     * Enable autofocus builder.
     *
     * @param autofocusEnabled
     *     the autofocus enabled
     * @return the builder
     */
    public Builder enableAutofocus(boolean autofocusEnabled) {
      this.autofocusEnabled = autofocusEnabled;
      return this;
    }

    /**
     * Width builder.
     *
     * @param width
     *     the width
     * @return the builder
     */
    public Builder width(int width) {
      if (width != 0) {
        this.width = width;
      }
      return this;
    }

    /**
     * Height builder.
     *
     * @param height
     *     the height
     * @return the builder
     */
    public Builder height(int height) {
      if (height != 0) {
        this.height = height;
      }
      return this;
    }

    /**
     * Facing builder.
     *
     * @param facing
     *     the facing
     * @return the builder
     */
    public Builder facing(int facing) {
      this.facing = facing;
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
     * Barcode detector.
     *
     * @param barcodeDetector
     *     the barcode detector
     */
    public void barcodeDetector(BarcodeDetector barcodeDetector) {
      this.barcodeDetector = barcodeDetector;
    }
  }
}

