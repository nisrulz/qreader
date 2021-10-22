# Usage Docs

## Steps

1. Add a `SurfaceView` to your layout

    ```xml
    <SurfaceView
      android:id="@+id/camera_view"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      />
    ```

1. Setup `SurfaceView` and `QREader` in `onCreate()`

    ```java
    // QREader
    private SurfaceView mySurfaceView;
    private QREader qrEader;
    ..

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      ..
      ..

      // Setup SurfaceView
      // -----------------
      mySurfaceView = (SurfaceView) findViewById(R.id.camera_view);

      // Init QREader
      // ------------
      qrEader = new QREader.Builder(this, mySurfaceView, new QRDataListener() {
            @Override
            public void onDetected(final String data) {
                Log.d("QREader", "Value : " + data);
            }

            @Override
            public void onReadQrError(final Exception exception) {
                Toast.makeText(MainActivity.this, "Cannot open camera", Toast.LENGTH_LONG).show();
            }
        }).facing(QREader.BACK_CAM)
                .enableAutofocus(true)
                .height(mySurfaceView.getHeight())
                .width(mySurfaceView.getWidth())
                .build();

    }
    ```

1. Initialize and Start in `onResume()`

    ```java
      @Override
      protected void onResume() {
        super.onResume();

        // Init and Start with SurfaceView
        // -------------------------------
        qrEader.initAndStart(mySurfaceView);
      }
    ```
1. Cleanup in `onPause()`

    ```java
      @Override
      protected void onPause() {
        super.onPause();

        // Cleanup in onPause()
        // --------------------
        qrEader.releaseAndCleanup();
      }
    ```

### Some provided utility functions which you can use

+ To check if the camera is running

    ```java
    boolean isCameraRunning = qrEader.isCameraRunning()
    ```

+ To stop `QREader`

    ```java
    qrEader.stop();
    ```
+ To start `QREader`

    ```java
    qrEader.start();
    ```

> #### Check the included sample app for a working example.