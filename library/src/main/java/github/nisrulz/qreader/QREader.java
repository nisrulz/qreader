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

  private static final String TAG = "QREader";
  private CameraSource cameraSource = null;
  private BarcodeDetector barcodeDetector = null;

  private boolean autofocus_enabled;
  private int width;
  private int height;
  private int facing;
  private boolean cameraRunning = false;
  private QRDataListener qrDataListener;
  private Context context;
  private SurfaceView surfaceView;

  private static QREader INSTANCE;

  private QREader() {

  }

  /**
   * Gets instance.
   *
   * @return the instance
   */
  public static QREader getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new QREader();
    }
    return INSTANCE;
  }

  /**
   * Sets up config.
   *
   * @param qrDataListener the qr data listener
   */
  public void setUpConfig(final QRDataListener qrDataListener) {
    setUpConfig(true, 800, 800, CameraSource.CAMERA_FACING_BACK, qrDataListener);
  }

  /**
   * Sets up config.
   *
   * @param autofocus_enabled the autofocus enabled
   * @param facing the facing
   * @param qrDataListener the qr data listener
   */
  public void setUpConfig(boolean autofocus_enabled, int facing,
      final QRDataListener qrDataListener) {
    setUpConfig(autofocus_enabled, 800, 800, facing, qrDataListener);
  }

  /**
   * Sets up config.
   *
   * @param autofocus_enabled the autofocus enabled
   * @param width the width
   * @param height the height
   * @param facing the facing
   * @param qrDataListener the qr data listener
   */
  public void setUpConfig(boolean autofocus_enabled, int width, int height, int facing,
      final QRDataListener qrDataListener) {
    this.autofocus_enabled = autofocus_enabled;
    this.width = width;
    this.height = height;
    this.facing = facing;
    this.qrDataListener = qrDataListener;
  }

  /**
   * Init.
   *
   * @param context the context
   * @param surfaceView the surface view
   */
  public void init(final Context context, final SurfaceView surfaceView) {
    this.context = context;
    this.surfaceView = surfaceView;

    if (barcodeDetector == null) {
      barcodeDetector =
          new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.QR_CODE).build();
    }

    if (cameraSource == null) {
      cameraSource =
          new CameraSource.Builder(context, barcodeDetector).setAutoFocusEnabled(autofocus_enabled)
              .setFacing(facing)
              .setRequestedPreviewSize(width, height)
              .build();
    }
  }

  /**
   * Start.
   */
  public void start() {
    if (surfaceView != null && surfaceHolderCallback != null) {
      surfaceView.getHolder().addCallback(surfaceHolderCallback);
    }
  }

  private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
    @Override public void surfaceCreated(SurfaceHolder surfaceHolder) {
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
      }

      startCameraView(context, cameraSource, surfaceView);
    }

    @Override public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
  };


  private void startCameraView(Context context, CameraSource cameraSource,
      SurfaceView surfaceView) {
    try {

      if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
          != PackageManager.PERMISSION_GRANTED) {
        Log.e(TAG, "Permission not granted!");
        return;
      } else if (!cameraRunning && cameraSource != null && surfaceView != null) {
        cameraSource.start(surfaceView.getHolder());
        cameraRunning = true;
      }
    } catch (IOException ie) {
      Log.e(TAG, ie.getMessage());
      ie.printStackTrace();
    }
  }

  /**
   * Stop camera
   */
  public void stop() {
    try {
      if (surfaceView != null && surfaceHolderCallback != null) {
        surfaceView.getHolder().removeCallback(surfaceHolderCallback);
      }
      if (cameraRunning && cameraSource != null) {
        cameraSource.stop();
        cameraRunning = false;
      }
    } catch (Exception ie) {
      Log.e(TAG, ie.getMessage());
      ie.printStackTrace();
    }
  }

  /**
   * Release and cleanup qr eader.
   */
  public void releaseAndCleanup() {
    stop();
    if (cameraSource != null) {
      cameraSource.release();
      cameraSource = null;
    }
  }
}

