package io.snabble.sdk.ui.scanner;

public enum BarcodeFormat {
    CODE_128(com.google.zxing.BarcodeFormat.CODE_128),
    EAN_8(com.google.zxing.BarcodeFormat.EAN_8),
    EAN_13(com.google.zxing.BarcodeFormat.EAN_13),
    QR_CODE(com.google.zxing.BarcodeFormat.QR_CODE),
    DATA_MATRIX(com.google.zxing.BarcodeFormat.DATA_MATRIX);

    com.google.zxing.BarcodeFormat zxingBarcodeFormat;

    BarcodeFormat(com.google.zxing.BarcodeFormat zxingBarcodeFormat) {
        this.zxingBarcodeFormat = zxingBarcodeFormat;
    }

    com.google.zxing.BarcodeFormat getZxingBarcodeFormat() {
        return zxingBarcodeFormat;
    }

    static BarcodeFormat valueOf(com.google.zxing.BarcodeFormat zxingBarcodeFormat) {
        BarcodeFormat[] values = BarcodeFormat.values();
        for (BarcodeFormat format : values) {
            if (format.getZxingBarcodeFormat() == zxingBarcodeFormat) {
                return format;
            }
        }

        return null;
    }
}
