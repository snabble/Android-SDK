package io.snabble.sdk;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public enum PaymentMethod {
    @SerializedName("qrCodePOS")
    QRCODE_POS("qrCodePOS", false, false, false, false),
    @SerializedName("qrCodeOffline")
    QRCODE_OFFLINE("qrCodeOffline", true, false, false, false),
    @SerializedName("deDirectDebit")
    DE_DIRECT_DEBIT("deDirectDebit", false, true, false, true),
    @SerializedName("creditCardVisa")
    VISA("creditCardVisa", false, true, false, true),
    @SerializedName("creditCardMastercard")
    MASTERCARD("creditCardMastercard", false, true, false, true),
    @SerializedName("creditCardAmericanExpress")
    AMEX("creditCardAmericanExpress", false, true, false, true),
    @SerializedName("externalBilling")
    TEGUT_EMPLOYEE_CARD("externalBilling", false, true, true, true),
    @SerializedName("externalBilling")
    LEINWEBER_CUSTOMER_ID("externalBilling", false, true, true, true),
    @SerializedName("customerCardPOS")
    CUSTOMERCARD_POS("customerCardPOS", false, false, false, false),
    @SerializedName("gatekeeperTerminal")
    GATEKEEPER_TERMINAL("gatekeeperTerminal", false, false, false, false),
    @SerializedName("paydirektOneKlick")
    PAYDIREKT("paydirektOneKlick", false, true, false, true),
    @SerializedName("postFinanceCard")
    POST_FINANCE_CARD("postFinanceCard", false, true, false, true),
    @SerializedName("twint")
    TWINT("twint", false, true, false, true),
    @SerializedName("googlePay")
    GOOGLE_PAY("googlePay", false, false, false, false);

    private final String id;
    private final boolean requiresCredentials;
    private final boolean isOfflineMethod;
    private final boolean showOnlyIfCredentialsArePresent;
    private final boolean needsAbortConfirmation;

    PaymentMethod(String id,
                  boolean isOfflineMethod,
                  boolean requiresCredentials,
                  boolean showOnlyIfCredentialsArePresent,
                  boolean needsAbortConfirmation) {
        this.id = id;
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
            if (pm.id.equals(value)) {
                return pm;
            }
        }

        return null;
    }
    public static PaymentMethod fromIdAndOrigin(String id, List<String> origin) {
        PaymentMethod[] values = values();
        for (PaymentMethod pm : values) {
            if(pm.id.equals(id) && pm.id.equals(TEGUT_EMPLOYEE_CARD.id)) {
                switch(origin.get(0)) {
                    case "tegutEmployeeID":
                        return TEGUT_EMPLOYEE_CARD;
                    case "leinweberCustomerID":
                        return LEINWEBER_CUSTOMER_ID;
                }
            } else if (pm.id.equals(id)) {
                return pm;
            }
        }

        return null;
    }

    public String getId() {
        return id;
    }
}