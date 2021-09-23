package io.snabble.sdk.firebase;


import com.google.mlkit.vision.barcode.Barcode;

import io.snabble.sdk.BarcodeFormat;

class FirebaseBarcodeHelper {
    public static int toFirebaseFormat(BarcodeFormat barcodeFormat) {
        switch(barcodeFormat) {
            case EAN_8:
                return Barcode.FORMAT_EAN_8;
            case EAN_13:
                return Barcode.FORMAT_EAN_13;
            case CODE_128:
                return Barcode.FORMAT_CODE_128;
            case CODE_39:
                return Barcode.FORMAT_CODE_39;
            case ITF_14:
                return Barcode.FORMAT_ITF;
            case DATA_MATRIX:
                return Barcode.FORMAT_DATA_MATRIX;
            case PDF_417:
                return Barcode.FORMAT_PDF417;
            case QR_CODE:
                return Barcode.FORMAT_QR_CODE;
        }

        return Barcode.FORMAT_UNKNOWN;
    }

    public static BarcodeFormat fromFirebaseFormat(int firebaseVisionBarcode) {
        switch (firebaseVisionBarcode) {
            case Barcode.FORMAT_EAN_8:
                return BarcodeFormat.EAN_8;
            case Barcode.FORMAT_EAN_13:
            case Barcode.FORMAT_UPC_A:
                return BarcodeFormat.EAN_13;
            case Barcode.FORMAT_CODE_128:
                return BarcodeFormat.CODE_128;
            case Barcode.FORMAT_CODE_39:
                return BarcodeFormat.CODE_39;
            case Barcode.FORMAT_ITF:
                return BarcodeFormat.ITF_14;
            case Barcode.FORMAT_DATA_MATRIX:
                return BarcodeFormat.DATA_MATRIX;
            case Barcode.FORMAT_PDF417:
                return BarcodeFormat.PDF_417;
            case Barcode.FORMAT_QR_CODE:
                return BarcodeFormat.QR_CODE;
        }

        return null;
    }
}
