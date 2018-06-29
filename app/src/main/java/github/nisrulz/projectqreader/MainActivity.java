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

package github.nisrulz.projectqreader;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;
import github.nisrulz.qreader.QRDataListener;
import github.nisrulz.qreader.QREader;

public class MainActivity extends AppCompatActivity {


    private static final String cameraPerm = Manifest.permission.CAMERA;

    boolean hasCameraPermission = false;

    private Menu menu;

    // QREader
    private SurfaceView mySurfaceView;

    private QREader qrEader;

    // UI
    private TextView text;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hasCameraPermission = RuntimePermissionUtil.checkPermissonGranted(this, cameraPerm);

        text = findViewById(R.id.code_info);

        // Setup SurfaceView
        // -----------------
        mySurfaceView = findViewById(R.id.camera_view);

        if (hasCameraPermission) {
            // Setup QREader
            setupQREader();

            //readQRCodeFromDrawable(R.drawable.img_qrcode);

        } else {
            RuntimePermissionUtil.requestPermission(MainActivity.this, cameraPerm, 100);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hasCameraPermission) {

            // Init and Start with SurfaceView
            // -------------------------------
            qrEader.initAndStart(mySurfaceView);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (hasCameraPermission) {

            // Cleanup in onPause()
            // --------------------
            qrEader.releaseAndCleanup();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (requestCode == 100) {
            RuntimePermissionUtil.onRequestPermissionsResult(grantResults, new RPResultListener() {
                @Override
                public void onPermissionDenied() {
                    // do nothing
                }

                @Override
                public void onPermissionGranted() {
                    if (RuntimePermissionUtil.checkPermissonGranted(MainActivity.this, cameraPerm)) {
                        restartActivity();
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_start_pause_preview:
                if (qrEader.isCameraRunning()) {
                    qrEader.stop();
                    menu.findItem(item.getItemId()).setIcon(R.drawable.ic_action_start);
                } else {
                    qrEader.start();
                    menu.findItem(item.getItemId()).setIcon(R.drawable.ic_action_pause);
                }
                break;

            case R.id.menu_restart:
                restartActivity();
                break;

            case R.id.menu_flash:
                if (qrEader.flashMode) {
                    qrEader.turnFlashOff();
                    menu.findItem(item.getItemId()).setIcon(R.drawable.ic_action_flash_on);
                } else {
                    qrEader.turnFlashOn();
                    menu.findItem(item.getItemId()).setIcon(R.drawable.ic_action_flash_off);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    void readQRCodeFromDrawable(int resID) {
        qrEader = new QREader.Builder(this, new QRDataListener() {
            @Override
            public void onDetected(final String data) {
                Log.d("QREader", "Value : " + data);
                text.post(new Runnable() {
                    @Override
                    public void run() {
                        text.setText(data);
                    }
                });
            }

            @Override
            public void onReadQrError(final Exception exception) {
                Toast.makeText(MainActivity.this, exception.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }).build();

        Bitmap bitmap = qrEader.getBitmapFromDrawable(resID);
        qrEader.readFromBitmap(bitmap);
    }

    void setupQREader() {
        // Init QREader
        // ------------
        qrEader = new QREader.Builder(this, new QRDataListener() {
            @Override
            public void onDetected(final String data) {
                Log.d("QREader", "Value : " + data);
                text.post(new Runnable() {
                    @Override
                    public void run() {
                        text.setText(data);
                    }
                });
            }

            @Override
            public void onReadQrError(final Exception exception) {
                Toast.makeText(MainActivity.this, "Cannot open camera", Toast.LENGTH_LONG).show();

            }
        }).facing(QREader.BACK_CAM)
            .enableAutofocus(true)
            .height(mySurfaceView.getHeight())
            .width(mySurfaceView.getWidth())
            .surfaceView(mySurfaceView)
            .build();
    }

    private void restartActivity() {
        startActivity(new Intent(MainActivity.this, MainActivity.class));
        finish();
    }
}
