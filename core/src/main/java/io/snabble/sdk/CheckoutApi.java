package io.snabble.sdk;

import android.os.Build;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.utils.DateUtils;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.JsonCallback;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class CheckoutApi {
    private static MediaType JSON = MediaType.parse("application/json");

    /*
     * Data structures as defined here:
     *
     * https://github.com/snabble/docs/blob/master/api_checkout.md
     */
    public static class Href {
        public String href;
    }

    public static class SignedCheckoutInfo {
        public JsonObject checkoutInfo;
        public String signature;
        public Map<String, Href> links;

        public String getCheckoutProcessLink() {
            Href checkoutProcess = links.get("checkoutProcess");
            if (checkoutProcess != null && checkoutProcess.href != null) {
                return checkoutProcess.href;
            }
            return null;
        }

        public PaymentMethodInfo[] getAvailablePaymentMethods(PaymentMethod[] clientAcceptedPaymentMethods) {
            if (checkoutInfo != null && checkoutInfo.has("paymentMethods")) {
                JsonArray jsonArray = checkoutInfo.getAsJsonArray("paymentMethods");
                if (jsonArray != null) {
                    List<PaymentMethodInfo> paymentMethods = new Gson().fromJson(jsonArray,
                            new TypeToken<List<PaymentMethodInfo>>() {}.getType());

                    if (clientAcceptedPaymentMethods == null) {
                        clientAcceptedPaymentMethods = PaymentMethod.values();
                    }

                    List<PaymentMethodInfo> result = new ArrayList<>();

                    for (PaymentMethod clientPaymentMethod : clientAcceptedPaymentMethods) {
                        for (PaymentMethodInfo paymentMethodInfo : paymentMethods) {
                            PaymentMethod pm = PaymentMethod.fromString(paymentMethodInfo.id);
                            if (pm != null) {
                                if (pm.equals(clientPaymentMethod)) {
                                    result.add(paymentMethodInfo);
                                }
                            }
                        }
                    }

                    return result.toArray(new PaymentMethodInfo[result.size()]);
                }
            }

            return new PaymentMethodInfo[0];
        }
    }

    public static class CheckoutInfo {
        Price price;
        LineItem[] lineItems;
    }

    public static class LineItem {
        String id;
        String refersTo;
        String sku;
        String name;
        int amount;
        int price;
        int totalPrice;
        LineItemType type;
    }

    public enum LineItemType {
        @SerializedName("default")
        DEFAULT,
        @SerializedName("deposit")
        DEPOSIT,
        @SerializedName("discount")
        DISCOUNT,
        @SerializedName("giveaway")
        GIVEAWAY,
    }

    public static class Price {
        int price;
    }

    public static class PaymentInformation {
        public String qrCodeContent;
        public String encryptedOrigin;
        public String originType;
    }

    public static class CheckoutProcessRequest {
        public SignedCheckoutInfo signedCheckoutInfo;
        public PaymentMethod paymentMethod;
        public PaymentInformation paymentInformation;
        public String finalizedAt;
        public Boolean processedOffline;
    }

    public static class PaymentMethodInfo {
        public String id;
        public String[] acceptedOriginTypes;
    }

    public enum PaymentState {
        @SerializedName("pending")
        PENDING,
        @SerializedName("processing")
        PROCESSING,
        @SerializedName("successful")
        SUCCESSFUL,
        @SerializedName("failed")
        FAILED,
    }

    public static class PaymentResult {
        public String ehiTechDaysIban;
        public String ehiTechDaysName;
    }

    public static class CheckoutProcessResponse {
        public Map<String, Href> links;
        public Boolean supervisorApproval;
        public Boolean paymentApproval;
        @SerializedName("orderID")
        public String orderId;
        public boolean aborted;
        public JsonObject checkoutInfo;
        public PaymentMethod paymentMethod;
        public boolean modified;
        public PaymentInformation paymentInformation;
        public PaymentState paymentState;
        public PaymentResult paymentResult;

        public String getSelfLink() {
            Href link = links.get("self");
            if (link != null && link.href != null) {
                return link.href;
            }
            return null;
        }

        public String getReceiptLink() {
            Href link = links.get("receipt");
            if (link != null && link.href != null) {
                return link.href;
            }
            return null;
        }
    }

    public interface CheckoutInfoResult {
        void success(SignedCheckoutInfo signedCheckoutInfo, int onlinePrice, PaymentMethodInfo[] availablePaymentMethods);
        void noShop();
        void invalidProducts(List<Product> products);
        void noAvailablePaymentMethod();
        void unknownError();
        void connectionError();
    }

    public interface PaymentProcessResult {
        void success(CheckoutProcessResponse checkoutProcessResponse);
        void error();
    }

    public interface PaymentAbortResult {
        void success();
        void error();
    }

    private Project project;
    private OkHttpClient okHttpClient;
    private Call call;

    public CheckoutApi(Project project) {
        this.project = project;
        this.okHttpClient = project.getOkHttpClient();
    }

    public void cancel() {
        if (call != null) {
            call.cancel();
            call = null;
        }
    }

    public void abort(final CheckoutProcessResponse checkoutProcessResponse, final PaymentAbortResult paymentAbortResult) {
        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(checkoutProcessResponse.getSelfLink()))
                .patch(RequestBody.create(JSON, "{\"aborted\":true}"))
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Logger.d("Payment aborted");
                    if (paymentAbortResult != null) {
                        paymentAbortResult.success();
                    }
                } else {
                    Logger.e("Error while aborting payment");
                    if (paymentAbortResult != null) {
                        paymentAbortResult.error();
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e("Error while aborting payment");
                if (paymentAbortResult != null) {
                    paymentAbortResult.error();
                }
            }
        });
    }

    public void createCheckoutInfo(final ShoppingCart.BackendCart backendCart,
                                   final PaymentMethod[] clientAcceptedPaymentMethods,
                                   final CheckoutInfoResult checkoutInfoResult) {
        String checkoutUrl = project.getCheckoutUrl();
        if (checkoutUrl == null) {
            Logger.e("Could not checkout, no checkout url provided in metadata");
            checkoutInfoResult.connectionError();
            return;
        }

        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(checkoutUrl))
                .post(RequestBody.create(JSON, GsonHolder.get().toJson(backendCart)))
                .build();

        cancel();

        call = okHttpClient.newCall(request);
        call.enqueue(new JsonCallback<SignedCheckoutInfo, JsonObject>(SignedCheckoutInfo.class, JsonObject.class) {
            @Override
            public void success(SignedCheckoutInfo signedCheckoutInfo) {
                int price;

                if (signedCheckoutInfo.checkoutInfo.has("price")
                        && signedCheckoutInfo.checkoutInfo.get("price").getAsJsonObject().has("price")) {
                    price = signedCheckoutInfo.checkoutInfo
                            .get("price")
                            .getAsJsonObject()
                            .get("price")
                            .getAsInt();
                } else {
                    price = project.getShoppingCart().getTotalPrice();
                }

                PaymentMethodInfo[] availablePaymentMethods = signedCheckoutInfo.getAvailablePaymentMethods(clientAcceptedPaymentMethods);
                if (availablePaymentMethods != null && availablePaymentMethods.length > 0) {
                    checkoutInfoResult.success(signedCheckoutInfo, price, availablePaymentMethods);
                } else {
                    checkoutInfoResult.connectionError();
                }
            }

            @Override
            public void failure(JsonObject obj) {
                try {
                    JsonObject error = obj.get("error").getAsJsonObject();
                    String type = error.get("type").getAsString();

                    switch (type) {
                        case "invalid_cart_item":
                            List<String> invalidSkus = new ArrayList<>();
                            JsonArray arr = error.get("details").getAsJsonArray();

                            for (int i=0; i<arr.size(); i++) {
                                String sku = arr.get(0).getAsJsonObject().get("sku").getAsString();
                                invalidSkus.add(sku);
                            }

                            List<Product> invalidProducts = new ArrayList<>();
                            ShoppingCart cart = project.getShoppingCart();
                            for (int i=0; i<cart.size(); i++) {
                                Product product = cart.get(i).getProduct();
                                if (invalidSkus.contains(product.getSku())) {
                                    invalidProducts.add(product);
                                }
                            }

                            Logger.e("Invalid products");
                            checkoutInfoResult.invalidProducts(invalidProducts);
                            break;
                        case "no_available_method":
                            checkoutInfoResult.noAvailablePaymentMethod();
                            break;
                        case "bad_shop_id":
                        case "shop_not_found":
                            checkoutInfoResult.noShop();
                            break;
                        default:
                            checkoutInfoResult.unknownError();
                            break;
                    }
                } catch (Exception e) {
                    error(e);
                }
            }

            @Override
            public void error(Throwable t) {
                Logger.e("Error creating checkout info: " + t.getMessage());
                checkoutInfoResult.connectionError();
            }
        });
    }


    public void updatePaymentProcess(final CheckoutProcessResponse checkoutProcessResponse,
                                     final PaymentProcessResult paymentProcessResult) {
        String url = checkoutProcessResponse.getSelfLink();
        if (url == null) {
            paymentProcessResult.error();
            return;
        }

        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .get()
                .build();

        cancel();

        call = okHttpClient.newCall(request);
        call.enqueue(new SimpleJsonCallback<CheckoutProcessResponse>(CheckoutProcessResponse.class) {
            @Override
            public void success(CheckoutProcessResponse checkoutProcessResponse) {
                paymentProcessResult.success(checkoutProcessResponse);
                CheckoutApi.this.call = null;
            }

            @Override
            public void error(Throwable t) {
                paymentProcessResult.error();
            }
        });
    }

    public void createPaymentProcess(final SignedCheckoutInfo signedCheckoutInfo,
                                     final PaymentMethod paymentMethod,
                                     final PaymentCredentials paymentCredentials,
                                     final boolean processedOffline,
                                     final Date finalizedAt,
                                     final PaymentProcessResult paymentProcessResult) {
        CheckoutProcessRequest checkoutProcessRequest = new CheckoutProcessRequest();
        checkoutProcessRequest.paymentMethod = paymentMethod;
        checkoutProcessRequest.signedCheckoutInfo = signedCheckoutInfo;

        if (processedOffline) {
            checkoutProcessRequest.processedOffline = true;

            if (finalizedAt != null) {
                checkoutProcessRequest.finalizedAt = DateUtils.toRFC3339(finalizedAt);
            }
        }

        if (paymentCredentials != null) {
            checkoutProcessRequest.paymentInformation = new PaymentInformation();

            String data = paymentCredentials.getEncryptedData();
            if (data == null) {
                paymentProcessResult.error();
                return;
            }

            checkoutProcessRequest.paymentInformation.originType = paymentCredentials.getType().getOriginType();
            checkoutProcessRequest.paymentInformation.encryptedOrigin = data;
        }

        String url = signedCheckoutInfo.getCheckoutProcessLink();
        if (url == null) {
            paymentProcessResult.error();
            return;
        }

        String json = GsonHolder.get().toJson(checkoutProcessRequest);
        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .post(RequestBody.create(JSON, json))
                .build();

        cancel();

        call = okHttpClient.newCall(request);
        call.enqueue(new SimpleJsonCallback<CheckoutProcessResponse>(CheckoutProcessResponse.class) {
            @Override
            public void success(CheckoutProcessResponse checkoutProcess) {
                paymentProcessResult.success(checkoutProcess);
            }

            @Override
            public void error(Throwable t) {
                paymentProcessResult.error();
            }
        });
    }
}
