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

package github.nisrulz.projectqreader

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import github.nisrulz.projectqreader.databinding.ActivityMainBinding
import github.nisrulz.qreader.QRDataListener
import github.nisrulz.qreader.QREader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var hasCameraPermission = false

    private var menu: Menu? = null

    // QREader
    private var qrEader: QREader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.apply {
            setContentView(root)
            wireupUserInterface(this)
        }
    }

    private fun wireupUserInterface(binding: ActivityMainBinding) {
        hasCameraPermission = RuntimePermissionUtil.checkPermissonGranted(this, cameraPerm)


        // Setup SurfaceView
        // -----------------

        if (hasCameraPermission) {
            // Setup QREader
            setupQREader()

            //readQRCodeFromDrawable(R.drawable.img_qrcode);

        } else {
            RuntimePermissionUtil.requestPermission(this, cameraPerm, 100)
        }
    }

    override fun onResume() {
        super.onResume()

        if (hasCameraPermission) {

            // Init and Start with SurfaceView
            // -------------------------------
            qrEader?.initAndStart(binding.surfaceViewCamera)
        }
    }

    override fun onPause() {
        super.onPause()

        if (hasCameraPermission) {

            // Cleanup in onPause()
            // --------------------
            qrEader?.releaseAndCleanup()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100) {
            RuntimePermissionUtil.onRequestPermissionsResult(
                grantResults,
                object : RPResultListener {
                    override fun onPermissionDenied() {
                        // do nothing
                    }

                    override fun onPermissionGranted() {
                        if (RuntimePermissionUtil.checkPermissonGranted(
                                this@MainActivity,
                                cameraPerm
                            )
                        ) {
                            restartActivity()
                        }
                    }
                })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menu_start_pause_preview -> if (qrEader?.isCameraRunning == true) {
                qrEader?.stop()
                menu?.findItem(item.itemId)?.setIcon(R.drawable.ic_action_start)
            } else {
                qrEader?.start()
                menu?.findItem(item.itemId)?.setIcon(R.drawable.ic_action_pause)
            }

            R.id.menu_restart -> restartActivity()

            R.id.menu_flash -> {
                when {
                    qrEader?.isFlashOn == true -> menu?.findItem(item.itemId)
                        ?.setIcon(R.drawable.ic_action_flash_on)
                    else -> menu?.findItem(item.itemId)?.setIcon(R.drawable.ic_action_flash_off)
                }
                qrEader?.toggleFlash()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    internal fun readQRCodeFromDrawable(resID: Int) {
        qrEader = QREader.Builder(this, object : QRDataListener {
            override fun onDetected(data: String) {
                Log.d("QREader", "Value : $data")
                binding.txtQrCodeInfo.post { binding.txtQrCodeInfo.text = data }
            }

            override fun onReadQrError(exception: Exception) {
                Toast.makeText(this@MainActivity, exception.localizedMessage, Toast.LENGTH_LONG)
                    .show()
            }
        }).build()

        val bitmap = qrEader?.getBitmapFromDrawable(resID)
        qrEader?.readFromBitmap(bitmap)
    }

    internal fun setupQREader() {
        // Init QREader
        // ------------
        qrEader = QREader.Builder(this, object : QRDataListener {
            override fun onDetected(data: String) {
                Log.d("QREader", "Value : $data")
                binding.txtQrCodeInfo.post { binding.txtQrCodeInfo.text = data }
            }

            override fun onReadQrError(exception: Exception) {
                Toast.makeText(this@MainActivity, "Cannot open camera", Toast.LENGTH_LONG).show()

            }
        }).facing(QREader.BACK_CAM)
            .enableAutofocus(true)
            .height(binding.surfaceViewCamera.height)
            .width(binding.surfaceViewCamera.width)
            .surfaceView(binding.surfaceViewCamera)
            .build()
    }

    private fun restartActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private val cameraPerm = Manifest.permission.CAMERA
    }
}
