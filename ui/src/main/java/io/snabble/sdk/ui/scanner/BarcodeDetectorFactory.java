package io.snabble.sdk.ui.scanner;

public abstract class BarcodeDetectorFactory {
    public abstract BarcodeDetector create();

    private static BarcodeDetectorFactory defaultBarcodeDetectorFactory = new BarcodeDetectorFactory() {
        @Override
        public BarcodeDetector create() {
            return new ZXingBarcodeDetector();
        }
    };

    public static void setDefaultBarcodeDetectorFactory(BarcodeDetectorFactory barcodeDetectorFactory) {
        defaultBarcodeDetectorFactory = barcodeDetectorFactory;
    }

    public static BarcodeDetectorFactory getDefaultBarcodeDetectorFactory() {
        return defaultBarcodeDetectorFactory;
    }
}
