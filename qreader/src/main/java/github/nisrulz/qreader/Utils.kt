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
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener

internal class Utils {
    fun checkCameraPermission(context: Context): Boolean {
        val permission = Manifest.permission.CAMERA
        val res = context.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    fun hasAutofocus(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)
    }

    fun hasCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN)
    fun removeOnGlobalLayoutListener(
        v: View,
        listener: OnGlobalLayoutListener?
    ) {
        v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }
}