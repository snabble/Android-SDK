package io.snabble.sdk;

import com.google.gson.annotations.SerializedName;

public enum PaymentMethod {
    @SerializedName("qrCodePOS")
    QRCODE_POS(false, false),
    @SerializedName("encodedCodes")
    ENCODED_CODES(true, false),
    @SerializedName("encodedCodesCSV")
    ENCODED_CODES_CSV(true, false),
    @SerializedName("teleCashDeDirectDebit")
    TELECASH_DIRECT_DEBIT(false, true);

    private boolean requiresCredentials;
    private boolean isOfflineMethod;

    PaymentMethod(boolean isOfflineMethod, boolean requiresCredentials) {
        this.isOfflineMethod = isOfflineMethod;
        this.requiresCredentials = requiresCredentials;
    }

    public boolean isOfflineMethod() {
        return isOfflineMethod;
    }

    public boolean isRequiringCredentials() {
        return requiresCredentials;
    }
}
