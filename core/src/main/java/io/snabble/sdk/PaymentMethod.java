package io.snabble.sdk;

import com.google.gson.annotations.SerializedName;

public enum PaymentMethod {
    @SerializedName("qrCodePOS")
    QRCODE_POS(false, false, false, false),
    @SerializedName("qrCodeOffline")
    QRCODE_OFFLINE(true, false, false, false),
    @SerializedName("deDirectDebit")
    DE_DIRECT_DEBIT(false, true, false, true),
    @SerializedName("creditCardVisa")
    VISA(false, true, false, true),
    @SerializedName("creditCardMastercard")
    MASTERCARD(false, true, false, true),
    @SerializedName("creditCardAmericanExpress")
    AMEX(false, true, false, true),
    @SerializedName("externalBilling")
    TEGUT_EMPLOYEE_CARD(false, true, true, true),
    @SerializedName("customerCardPOS")
    CUSTOMERCARD_POS(false, false, false, false),
    @SerializedName("gatekeeperTerminal")
    GATEKEEPER_TERMINAL(false, false, false, false),
    @SerializedName("paydirektOneKlick")
    PAYDIREKT(false, true, false, true),
    @SerializedName("datatrans")
    DATATRANS(false, true, false, true);

    private boolean requiresCredentials;
    private boolean isOfflineMethod;
    private boolean showOnlyIfCredentialsArePresent;
    private boolean needsAbortConfirmation;

    PaymentMethod(boolean isOfflineMethod,
                  boolean requiresCredentials,
                  boolean showOnlyIfCredentialsArePresent,
                  boolean needsAbortConfirmation) {
        this.isOfflineMethod = isOfflineMethod;
        this.requiresCredentials = requiresCredentials;
        this.showOnlyIfCredentialsArePresent = showOnlyIfCredentialsArePresent;
        this.needsAbortConfirmation = needsAbortConfirmation;
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

    public boolean needsAbortConfirmation() {
        return needsAbortConfirmation;
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
