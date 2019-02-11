package io.snabble.sdk.firebase;

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;

import io.snabble.sdk.BarcodeFormat;

class FirebaseBarcodeHelper {
    public static int toFirebaseFormat(BarcodeFormat barcodeFormat) {
        switch(barcodeFormat) {
            case EAN_8:
                return FirebaseVisionBarcode.FORMAT_EAN_8;
            case EAN_13:
                return FirebaseVisionBarcode.FORMAT_EAN_13;
            case CODE_128:
                return FirebaseVisionBarcode.FORMAT_CODE_128;
            case CODE_39:
                return FirebaseVisionBarcode.FORMAT_CODE_39;
            case ITF_14:
                return FirebaseVisionBarcode.FORMAT_ITF;
            case DATA_MATRIX:
                return FirebaseVisionBarcode.FORMAT_DATA_MATRIX;
            case QR_CODE:
                return FirebaseVisionBarcode.FORMAT_QR_CODE;
        }

        return FirebaseVisionBarcode.FORMAT_UNKNOWN;
    }

    public static BarcodeFormat fromFirebaseFormat(int firebaseVisionBarcode) {
        switch (firebaseVisionBarcode) {
            case FirebaseVisionBarcode.FORMAT_EAN_8:
                return BarcodeFormat.EAN_8;
            case FirebaseVisionBarcode.FORMAT_EAN_13:
            case FirebaseVisionBarcode.FORMAT_UPC_A:
                return BarcodeFormat.EAN_13;
            case FirebaseVisionBarcode.FORMAT_CODE_128:
                return BarcodeFormat.CODE_128;
            case FirebaseVisionBarcode.FORMAT_CODE_39:
                return BarcodeFormat.CODE_39;
            case FirebaseVisionBarcode.FORMAT_ITF:
                return BarcodeFormat.ITF_14;
            case FirebaseVisionBarcode.FORMAT_DATA_MATRIX:
                return BarcodeFormat.DATA_MATRIX;
            case FirebaseVisionBarcode.FORMAT_QR_CODE:
                return BarcodeFormat.QR_CODE;
        }

        return null;
    }
}
