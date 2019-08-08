package io.snabble.sdk;

import com.google.gson.annotations.SerializedName;

public enum PaymentMethod {
    @SerializedName("qrCodePOS")
    QRCODE_POS(false, false),
    @SerializedName("qrCodeOffline")
    QRCODE_OFFLINE(true, false),
    @SerializedName("deDirectDebit")
    DE_DIRECT_DEBIT(false, true);

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

    public static PaymentMethod fromString(String value) {
        PaymentMethod[] values = values();
        for (PaymentMethod pm : values) {
            try {
                SerializedName serializedName = PaymentMethod.class.getField(pm.name()).getAnnotation(SerializedName.class);
                if (serializedName != null) {
                    String name = serializedName.value();
                    if (name.equals(value)) {
                        return pm;
                    }
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
