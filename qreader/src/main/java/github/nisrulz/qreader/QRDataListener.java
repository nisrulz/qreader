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

import android.graphics.Rect;

import com.google.android.gms.vision.barcode.Barcode;

/**
 * The interface Qr data listener.
 */
public abstract class QRDataListener implements QRBarcodeListener {

    //Added Rect to limit the reading area
    private Rect readingArea;
    //Added Rect that allows to get the read data area
    private Rect readData;

    /**
     * On detected.
     *
     * @param data
     *     the data
     */
    // Called from not main thread. Be careful
    public abstract void onDetected(final String data);

    @Override
    public void onDetected(Barcode data) {
        readData = data.getBoundingBox();
        if(readingArea == null) {
            onDetected(data.displayValue);
        }
        else {
            if(readingArea.contains(readData)) {
                onDetected(data.displayValue);
            }
        }


    }

    //readData is readOnly
    public Rect getReadData() {
      return readData;
    }

    //readingData is set only

    public void setReadingArea(Rect readingArea) {
      this.readingArea = readingArea;
    }
}
