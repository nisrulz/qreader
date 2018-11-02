package github.nisrulz.qreader;

import android.graphics.Bitmap;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions.Builder;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

public class MLKitReader {

    private FirebaseVisionBarcodeDetectorOptions options = new Builder()
            .setBarcodeFormats(
                    FirebaseVisionBarcode.FORMAT_QR_CODE, FirebaseVisionBarcode.FORMAT_AZTEC,
                    FirebaseVisionBarcode.FORMAT_DATA_MATRIX)
            .build();

    private FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);


    String getDataFromBitmap(Bitmap bitmap) {
        FirebaseVisionImage visionImage = FirebaseVisionImage.fromBitmap(bitmap);

        detector.detectInImage(image)
                .addOnSuccessListener {
            // Task succeeded!
            for (barcode in it) {
                // Do something with barcode
            }
        }
    .addOnFailureListener {
            // Task failed with an exception
        }
    }

}
