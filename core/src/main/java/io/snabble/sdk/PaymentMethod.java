package io.snabble.sdk;

import com.google.gson.annotations.SerializedName;

public enum PaymentMethod {
    @SerializedName("qrCodePOS")
    QRCODE_POS(false, false, false),
    @SerializedName("qrCodeOffline")
    QRCODE_OFFLINE(true, false, false),
    @SerializedName("deDirectDebit")
    DE_DIRECT_DEBIT(false, true, false),
    @SerializedName("creditCardVisa")
    VISA(false, true, false),
    @SerializedName("creditCardMastercard")
    MASTERCARD(false, true, false),
    @SerializedName("externalBilling")
    TEGUT_EMPLOYEE_CARD(false, true, true),
    @SerializedName("gatekeeperTerminal")
    GATEKEEPER_TERMINAL(false, false, false);

    private boolean requiresCredentials;
    private boolean isOfflineMethod;
    private boolean showOnlyIfCredentialsArePresent;

    PaymentMethod(boolean isOfflineMethod,
                  boolean requiresCredentials,
                  boolean showOnlyIfCredentialsArePresent) {
        this.isOfflineMethod = isOfflineMethod;
        this.requiresCredentials = requiresCredentials;
        this.showOnlyIfCredentialsArePresent = showOnlyIfCredentialsArePresent;
    }

    public boolean isOfflineMethod() {
        return isOfflineMethod;
    }

    public boolean isRequiringCredentials() {
        return requiresCredentials;
    }

    public boolean isShowOnlyIfCredentialsArePresent() {
        return showOnlyIfCredentialsArePresent;
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
