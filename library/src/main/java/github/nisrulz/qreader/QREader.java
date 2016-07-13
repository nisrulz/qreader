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
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

/**
 * QREader Singleton
 */
public class QREader {
  public static final int FRONT_CAM = CameraSource.CAMERA_FACING_FRONT;
  public static final int BACK_CAM = CameraSource.CAMERA_FACING_BACK;
  private static final String LOGTAG = "QREader";
  private final int width;
  private final int height;
  private final int facing;
  private final QRDataListener qrDataListener;
  private final Context context;
  private final SurfaceView surfaceView;
  private CameraSource cameraSource = null;
  private BarcodeDetector barcodeDetector = null;
  private boolean autoFocusEnabled;
  private boolean cameraRunning = false;
  private boolean created = false;

  private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
    @Override public void surfaceCreated(SurfaceHolder surfaceHolder) {
      created = true;
      startCameraView(context, cameraSource, surfaceView);
    }


    @Override public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }


    @Override public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
      created = false;
      stop();
      surfaceHolder.removeCallback(this);
    }
  };

  public QREader(Builder builder) {
    this.autoFocusEnabled = builder.autofocus_enabled;
    this.width = builder.width;
    this.height = builder.height;
    this.facing = builder.facing;
    this.qrDataListener = builder.qrDataListener;
    this.context = builder.context;
    this.surfaceView = builder.surfaceView;
  }

  public boolean isCameraRunning() {
    return cameraRunning;
  }

  /**
   * Init.
   */
  public void init() {
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

    // Setup Barcodedetector
    barcodeDetector =
        new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.QR_CODE).build();

    if (barcodeDetector.isOperational()) {
      barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
        @Override public void release() {

        }

        @Override public void receiveDetections(Detector.Detections<Barcode> detections) {
          final SparseArray<Barcode> barcodes = detections.getDetectedItems();
          if (barcodes.size() != 0 && qrDataListener != null) {
            qrDataListener.onDetected(barcodes.valueAt(0).displayValue);
          }
        }
      });

      cameraSource =
          new CameraSource.Builder(context, barcodeDetector)
              .setAutoFocusEnabled(autoFocusEnabled)
              .setFacing(facing)
              .setRequestedFps(18)
              .setRequestedPreviewSize(width, height)
              .build();
    } else {
      Log.e(LOGTAG, "Barcode recognition libs are not downloaded and are not operational");
    }
  }

  /**
   * Start scanning qr codes.
   */
  public void start() {
    if (surfaceView != null && surfaceHolderCallback != null) {
      if (created) {
        startCameraView(context, cameraSource, surfaceView);
      } else {
        surfaceView.getHolder().addCallback(surfaceHolderCallback);
      }
    }
  }

  private void startCameraView(Context context, CameraSource cameraSource,
                               SurfaceView surfaceView) {
    if (cameraRunning)
      return;
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
   * Stop camera and
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
   * Release and cleanup qreader.
   */
  public void releaseAndCleanup() {
    stop();
    if (cameraSource != null) {
      cameraSource.release();
      cameraSource = null;
    }
  }

  private boolean checkCameraPermission(Context context) {
    String permission = Manifest.permission.CAMERA;
    int res = context.checkCallingOrSelfPermission(permission);
    return (res == PackageManager.PERMISSION_GRANTED);
  }

  private boolean hasCameraHardware(Context context) {
    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
  }

  private boolean hasAutofocus(Context context) {
    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
  }

  public static class Builder {
    private boolean autofocus_enabled;
    private int width;
    private int height;
    private int facing;
    private QRDataListener qrDataListener;
    private Context context;
    private SurfaceView surfaceView;

    public Builder(Context context, SurfaceView surfaceView, QRDataListener qrDataListener) {
      this.autofocus_enabled = true;
      this.width = 800;
      this.height = 800;
      this.facing = BACK_CAM;
      this.qrDataListener = qrDataListener;
      this.context = context;
      this.surfaceView = surfaceView;
    }

    public Builder enableAutofocus(boolean autocofucEnabled) {
      this.autofocus_enabled = autocofucEnabled;
      return this;
    }

    public Builder width(int width) {
      this.width = width;
      return this;
    }

    public Builder height(int height) {
      this.height = height;
      return this;
    }

    public Builder facing(int facing) {
      this.facing = facing;
      return this;
    }

    public QREader build() {
      return new QREader(this);
    }
  }


}

