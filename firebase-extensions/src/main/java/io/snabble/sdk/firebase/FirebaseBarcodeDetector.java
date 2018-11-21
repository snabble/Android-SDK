package io.snabble.sdk.firebase;

import android.graphics.Rect;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.util.List;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.ui.scanner.Barcode;
import io.snabble.sdk.ui.scanner.BarcodeDetector;
import io.snabble.sdk.ui.scanner.FalsePositiveFilter;
import io.snabble.sdk.utils.Logger;

public class FirebaseBarcodeDetector implements BarcodeDetector {
    private byte[] cropBuffer = null;
    private FalsePositiveFilter falsePositiveFilter = new FalsePositiveFilter(3);
    private FirebaseVisionBarcodeDetector detector;

    public FirebaseBarcodeDetector() {

    }

    @Override
    public void setup(List<BarcodeFormat> barcodeFormats) {
        int[] formats = new int[barcodeFormats.size()];
        for (int i = 0; i<formats.length; i++) {
            formats[i] = FirebaseBarcodeHelper.toFirebaseFormat(barcodeFormats.get(i));
        }

        FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(formats[0], formats)
                .build();

        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
    }

    @Override
    public void reset() {
        falsePositiveFilter.reset();
    }

    @Override
    public Barcode detect(byte[] data, int width, int height, int bitsPerPixel, Rect detectionRect, int displayOrientation) {
        byte[] buf = crop(data, width, height, bitsPerPixel, detectionRect);

        int firebaseRotation;
        switch (displayOrientation) {
            case 0:
                firebaseRotation = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                firebaseRotation = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                firebaseRotation = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                firebaseRotation = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                firebaseRotation = FirebaseVisionImageMetadata.ROTATION_0;
        }

        FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(buf, new FirebaseVisionImageMetadata.Builder()
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(firebaseRotation)
                .setWidth(detectionRect.width())
                .setHeight(detectionRect.height())
                .build());


        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image);

        try {
            Tasks.await(result);
            List<FirebaseVisionBarcode> firebaseBarcodes = result.getResult();
            if(firebaseBarcodes != null && firebaseBarcodes.size() > 0) {
                FirebaseVisionBarcode firebaseVisionBarcode = firebaseBarcodes.get(0);

                // MLKit decodes all ITF lengths, but we only care about ITF14.
                if (firebaseVisionBarcode.getFormat() == FirebaseVisionBarcode.FORMAT_ITF) {
                    if (firebaseVisionBarcode.getRawValue().length() != 14) {
                        return null;
                    }
                }

                Barcode barcode = new Barcode(FirebaseBarcodeHelper.fromFirebaseFormat(firebaseVisionBarcode.getFormat()),
                        firebaseVisionBarcode.getRawValue(),
                        System.currentTimeMillis());

                Barcode filtered = falsePositiveFilter.filter(barcode);
                if (filtered != null) {
                    Logger.d("Detected barcode: " + barcode.toString());
                    return filtered;
                }
            }
        } catch (Exception e) {

        }

        return null;
    }

    private byte[] crop(byte[] data, int width, int height, int bitsPerPixel, Rect detectionRect) {
        int size = detectionRect.width() * detectionRect.height() * bitsPerPixel / 8;
        if (cropBuffer == null || cropBuffer.length != size) {
            cropBuffer = new byte[size];
        }

        byte[] buf = cropBuffer;

        int left = detectionRect.left;
        int top = detectionRect.top;
        int right = detectionRect.right;
        int bottom = detectionRect.bottom;

        int tWidth = detectionRect.width();
        int tHeight = detectionRect.height();

        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                buf[(x - left) + (y - top) * tWidth] = data[x + y * width];
            }
        }

        return buf;
    }

}
