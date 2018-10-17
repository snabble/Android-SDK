package io.snabble.sdk;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class CheckoutApi {
    /*
     * Data structures as defined here:
     *
     * https://github.com/snabble/docs/blob/master/api_checkout.md
     */
    private static class Href {
        private String href;
    }

    private static class SignedCheckoutInfo {
        private JsonObject checkoutInfo;
        private String signature;
        private Map<String, Href> links;

        private String getCheckoutProcessLink() {
            Href checkoutProcess = links.get("checkoutProcess");
            if (checkoutProcess != null && checkoutProcess.href != null) {
                return checkoutProcess.href;
            }
            return null;
        }
    }

    private static class PaymentInformation {
        private String qrCodeContent;
    }

    private static class CheckoutProcessRequest {
        private SignedCheckoutInfo signedCheckoutInfo;
        private PaymentMethod paymentMethod;
    }

    private enum PaymentState {
        @SerializedName("pending")
        PENDING,
        @SerializedName("successful")
        SUCCESSFUL,
        @SerializedName("failed")
        FAILED,
    }

    private static class CheckoutProcessResponse {
        private Map<String, Href> links;
        private Boolean supervisorApproval;
        private Boolean paymentApproval;
        private boolean aborted;
        private JsonObject checkoutInfo;
        private PaymentMethod paymentMethod;
        private boolean modified;
        private PaymentInformation paymentInformation;
        private PaymentState paymentState;

        private String getSelfLink() {
            Href link = links.get("self");
            if (link != null && link.href != null) {
                return link.href;
            }
            return null;
        }

        private String getReceiptLink() {
            Href link = links.get("receipt");
            if (link != null && link.href != null) {
                return link.href;
            }
            return null;
        }
    }


}
