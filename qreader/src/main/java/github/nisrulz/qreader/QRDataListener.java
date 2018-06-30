package github.nisrulz.qreader;

import android.graphics.Rect;
import com.google.android.gms.vision.barcode.Barcode;

public abstract class QRDataListener implements QRBarcodeListener {

    //Added Rect to limit the reading area
    private Rect readingArea;

    //Added Rect that allows to get the read data area
    private Rect readData;

    // Called from not main thread. Be careful
    public abstract void onDetected(final String data);

    @Override
    public void onDetected(Barcode data) {
        readData = data.getBoundingBox();
        if (readingArea == null) {
            onDetected(data.displayValue);
        } else {
            if (readingArea.contains(readData)) {
                onDetected(data.displayValue);
            }
        }
    }

    //readData is readOnly
    public Rect getReadingArea() {
        return readData;
    }

    //readingData is set only
    public void setReadingArea(Rect readingArea) {
        this.readingArea = readingArea;
    }
}