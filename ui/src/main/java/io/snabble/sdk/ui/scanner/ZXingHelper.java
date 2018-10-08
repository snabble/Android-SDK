package io.snabble.sdk.ui.scanner;

import io.snabble.sdk.BarcodeFormat;

class ZXingHelper {
    public static com.google.zxing.BarcodeFormat toZXingFormat(BarcodeFormat barcodeFormat) {
        switch(barcodeFormat) {
            case EAN_8:
                return com.google.zxing.BarcodeFormat.EAN_8;
            case EAN_13:
                return com.google.zxing.BarcodeFormat.EAN_13;
            case CODE_128:
                return com.google.zxing.BarcodeFormat.CODE_128;
            case ITF:
                return com.google.zxing.BarcodeFormat.ITF;
            case DATA_MATRIX:
                return com.google.zxing.BarcodeFormat.DATA_MATRIX;
            case QR_CODE:
                return com.google.zxing.BarcodeFormat.QR_CODE;
        }

        return null;
    }

    public static BarcodeFormat fromZXingFormat(com.google.zxing.BarcodeFormat barcodeFormat) {
        switch (barcodeFormat) {
            case EAN_8:
                return BarcodeFormat.EAN_8;
            case EAN_13:
                return BarcodeFormat.EAN_13;
            case CODE_128:
                return BarcodeFormat.CODE_128;
            case ITF:
                return BarcodeFormat.ITF;
            case DATA_MATRIX:
                return BarcodeFormat.DATA_MATRIX;
            case QR_CODE:
                return BarcodeFormat.QR_CODE;
        }

        return null;
    }
}
