package io.snabble.sdk;

public enum BarcodeFormat {
    CODE_39,
    CODE_128,
    EAN_8,
    EAN_13,
    ITF,
    QR_CODE,
    DATA_MATRIX;

    public static BarcodeFormat parse(String str) {
        switch (str) {
            case "code39":
                return CODE_39;
            case "code128":
                return CODE_128;
            case "ean8":
                return EAN_8;
            case "ean13":
                return EAN_13;
            case "itf14":
                return ITF;
            case "datamatrix":
                return DATA_MATRIX;
            case "qr":
                return QR_CODE;
        }

        return null;
    }
}
