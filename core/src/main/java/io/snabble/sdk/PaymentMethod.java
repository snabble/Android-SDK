package io.snabble.sdk;

import com.google.gson.annotations.SerializedName;

public enum PaymentMethod {
    @SerializedName("cash")
    CASH(false),
    @SerializedName("qrCodePOS")
    QRCODE_POS(false),
    @SerializedName("encodedCodes")
    ENCODED_CODES(true);

    private boolean isOfflineMethod;

    PaymentMethod(boolean isOfflineMethod){
        this.isOfflineMethod = isOfflineMethod;
    }

    public boolean isOfflineMethod() {
        return isOfflineMethod;
    }
}
