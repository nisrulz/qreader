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

import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import github.nisrulz.qreader.QRDataListener;
import github.nisrulz.qreader.QREader;

public class MainActivity extends AppCompatActivity implements QRDataListener {
  private SurfaceView surfaceView;
  private TextView text;
  private Button stateBtn;
  private QREader qrEader;


  @Override protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    surfaceView = (SurfaceView) findViewById(R.id.camera_view);
    text = (TextView) findViewById(R.id.code_info);
    stateBtn = (Button) findViewById(R.id.state_btn);

    surfaceView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override public void onGlobalLayout() {
        //to get surfaceView size
        initAndStartQrReader(surfaceView.getWidth(), surfaceView.getHeight());
        surfaceView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
      }
    });
  }

  private void initAndStartQrReader(int previewWidth, int previewHeight) {
    qrEader = new QREader.Builder(MainActivity.this, surfaceView, MainActivity.this)
        .facing(QREader.BACK_CAM)
        .enableAutofocus(true)
        .height(previewHeight)
        .width(previewWidth)
        .build();
    qrEader.init();
    qrEader.start();

    // change of reader state in dynamic
     stateBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (qrEader.isCameraRunning()) {
          qrEader.stop();
        } else {
          qrEader.start();
        }
      }
    });
    stateBtn.setVisibility(View.VISIBLE);
  }

  @Override protected void onResume() {
    super.onResume();
    if (qrEader != null)
      qrEader.start();
  }

  @Override protected void onPause() {
    super.onPause();
    if (qrEader != null)
      qrEader.stop();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (qrEader != null)
      qrEader.releaseAndCleanup();
  }

  @Override public void onDetected(final String data) {
    Log.d("QREader", "Value : " + data);
    text.post(new Runnable() {
      @Override
      public void run() {
        text.setText(data);
      }
    });
  }
}
