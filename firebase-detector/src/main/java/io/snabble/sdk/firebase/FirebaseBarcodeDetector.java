package io.snabble.sdk.firebase;

import android.graphics.Rect;

import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;


import java.util.ArrayList;
import java.util.List;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.ui.scanner.BarcodeDetector;
import io.snabble.sdk.ui.scanner.FalsePositiveFilter;
import io.snabble.sdk.utils.Logger;

@KeepName
public class FirebaseBarcodeDetector implements BarcodeDetector {
    private byte[] cropBuffer = null;
    private final FalsePositiveFilter falsePositiveFilter = new FalsePositiveFilter(3);
    private BarcodeScanner detector;

    public FirebaseBarcodeDetector() {

    }

    @Override
    public void setup(List<BarcodeFormat> barcodeFormats) {
        List<Integer> formats = new ArrayList<>();

        for (int i = 0; i<barcodeFormats.size(); i++) {
            int format = FirebaseBarcodeHelper.toFirebaseFormat(barcodeFormats.get(i));
            formats.add(format);

            // EAN13 with 0 prefixes are detected as UPC-A
            if (format == Barcode.FORMAT_EAN_13) {
                formats.add(Barcode.FORMAT_UPC_A);
            }
        }

        int[] ints = new int[formats.size()];
        for (int i=0; i<formats.size(); i++) {
            ints[i] = formats.get(i);
        }

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(ints[0], ints)
                .build();

        detector = BarcodeScanning.getClient(options);
    }

    @Override
    public void reset() {
        falsePositiveFilter.reset();
    }

    @Override
    public io.snabble.sdk.ui.scanner.Barcode detect(byte[] data, int width, int height, int bitsPerPixel, Rect detectionRect, int displayOrientation) {
        byte[] buf = crop(data, width, height, bitsPerPixel, detectionRect);

        InputImage inputImage = InputImage.fromByteArray(buf,
                detectionRect.width(),
                detectionRect.height(),
                displayOrientation,
                InputImage.IMAGE_FORMAT_NV21);

        Task<List<Barcode>> result = detector.process(inputImage);

        try {
            Tasks.await(result);
            List<Barcode> firebaseBarcodes = result.getResult();
            if(firebaseBarcodes != null && firebaseBarcodes.size() > 0) {
                Barcode firebaseVisionBarcode = firebaseBarcodes.get(0);
                String rawValue = firebaseVisionBarcode.getRawValue();

                if (rawValue == null) {
                    return null;
                }

                // MLKit decodes all ITF lengths, but we only care about ITF14.
                if (firebaseVisionBarcode.getFormat() == Barcode.FORMAT_ITF) {
                    if (rawValue.length() != 14) {
                        return null;
                    }
                }

                if (firebaseVisionBarcode.getFormat() == Barcode.FORMAT_UPC_A) {
                    if (rawValue.length() > 13) {
                        return null;
                    }

                    Logger.d("Detected UPC-A with length %d, converting to EAN13", rawValue.length());

                    int diff = 13 - rawValue.length();
                    StringBuilder sb = new StringBuilder();
                    for (int i=0; i<diff; i++) {
                        sb.append('0');
                    }
                    sb.append(rawValue);

                    rawValue = sb.toString();
                }

                BarcodeFormat format = FirebaseBarcodeHelper.fromFirebaseFormat(firebaseVisionBarcode.getFormat());
                if (format == null) {
                    return null;
                }

                io.snabble.sdk.ui.scanner.Barcode barcode = new io.snabble.sdk.ui.scanner.Barcode(format, rawValue, System.currentTimeMillis());

                io.snabble.sdk.ui.scanner.Barcode filtered = falsePositiveFilter.filter(barcode);
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
        int size = detectionRect.width() * detectionRect.height() * bitsPerPixel / 8 * 3;
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
