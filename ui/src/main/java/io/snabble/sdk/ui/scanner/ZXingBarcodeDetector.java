package io.snabble.sdk.ui.scanner;

import android.graphics.Rect;

import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.snabble.sdk.BarcodeFormat;
import io.snabble.sdk.utils.Logger;

public class ZXingBarcodeDetector implements BarcodeDetector {
    private byte[] cropBuffer = null;
    private MultiFormatReader multiFormatReader;
    private FalsePositiveFilter falsePositiveFilter = new FalsePositiveFilter();

    @Override
    public void setup(List<BarcodeFormat> barcodeFormats) {
        Map<DecodeHintType, Object> hints = new HashMap<>();
        multiFormatReader = new MultiFormatReader();

        List<com.google.zxing.BarcodeFormat> formats = new ArrayList<>();
        for (BarcodeFormat barcodeFormat : barcodeFormats) {
            formats.add(ZXingHelper.toZXingFormat(barcodeFormat));
        }

        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        //hints.put(DecodeHintType.TRY_HARDER, true);
        multiFormatReader.setHints(hints);
    }

    @Override
    public void reset() {
        falsePositiveFilter.reset();
    }

    @Override
    public Barcode detect(byte[] data, int width, int height, int bitsPerPixel, Rect detectionRect, int displayOrientation) {
        Result result = detectInternal(data, width, height, bitsPerPixel, detectionRect, displayOrientation, false);
        if (result == null) {
            result = detectInternal(data, width, height, bitsPerPixel, detectionRect, displayOrientation, true);
        }

        if (result != null) {
            // ZXing decodes all ITF lengths, but we only care about ITF14.
            if (result.getBarcodeFormat() == com.google.zxing.BarcodeFormat.ITF) {
                if (result.getText().length() != 14) {
                    return null;
                }
            }

            Barcode barcode = new Barcode(
                    ZXingHelper.fromZXingFormat(result.getBarcodeFormat()),
                    result.getText(),
                    result.getTimestamp());

            Barcode filtered = falsePositiveFilter.filter(barcode);
            if (filtered != null) {
                Logger.d("Detected barcode: " + barcode.toString());
                return filtered;
            }
        }

        return null;
    }

    private Result detectInternal(byte[] data, int width, int height, int bitsPerPixel, Rect detectionRect, int displayOrientation, boolean rotate) {
        byte[] buf = getRotatedData(data, width, height, bitsPerPixel, detectionRect, displayOrientation, rotate);

        int tWidth = detectionRect.width();
        int tHeight = detectionRect.height();

        if (rotate) {
            int tmp = tWidth;
            tWidth = tHeight;
            tHeight = tmp;
        }

        LuminanceSource luminanceSource = new PlanarYUVLuminanceSource(buf, tWidth, tHeight,
                0, 0, tWidth, tHeight, false);

        Binarizer binarizer = new HybridBinarizer(luminanceSource);
        BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);

        try {
            return multiFormatReader.decodeWithState(binaryBitmap);
        } catch (ReaderException e) {
            // could not detect a barcode, ignore
        } catch (Exception e) {
            // catch any other exceptions that may be thrown by zxing
            Logger.e("Zxing Internal Error: %s", e.toString());
        } finally {
            multiFormatReader.reset();
        }

        return null;
    }

    private byte[] getRotatedData(byte[] data, int width, int height, int bitsPerPixel, Rect detectionRect, int displayOrientation, boolean rotate90deg) {
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

        // special cases for reversed orientations, we don't flip the image
        // because zxing is able to detect flipped images
        // but we need to adjust the camera region
        if (displayOrientation == 180) { // reverse-landscape
            top = height - top - tHeight;
            bottom = top + tHeight;
        } else if (displayOrientation == 270) { // reverse-portrait
            left = width - left - tWidth;
            right = left + tWidth;
        }

        if (rotate90deg) {
            int i = 0;
            for (int x = left; x < right; x++) {
                for (int y = bottom - 1; y >= top; y--) {
                    buf[i] = data[y * width + x];
                    i++;
                }
            }
        } else {
            for (int y = top; y < bottom; y++) {
                for (int x = left; x < right; x++) {
                    buf[(x - left) + (y - top) * tWidth] = data[x + y * width];
                }
            }
        }

        return buf;
    }

}
