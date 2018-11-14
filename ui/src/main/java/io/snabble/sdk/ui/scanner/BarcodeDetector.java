package io.snabble.sdk.ui.scanner;

import android.graphics.Rect;

import java.util.List;

import io.snabble.sdk.BarcodeFormat;

public interface BarcodeDetector {
    void setup(List<BarcodeFormat> barcodeFormats);
    void reset();
    Barcode detect(byte[] data, int width, int height, int bitsPerPixel, Rect detectionRect, int displayOrientation);
}
