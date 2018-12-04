package io.snabble.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

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

        public PaymentMethod[] getAvailablePaymentMethods(PaymentMethod[] clientAcceptedPaymentMethods) {
            if (checkoutInfo != null && checkoutInfo.has("availableMethods")) {
                JsonArray jsonArray = checkoutInfo.getAsJsonArray("availableMethods");
                if (jsonArray != null) {
                    List<PaymentMethod> paymentMethods = new Gson().fromJson(jsonArray,
                            new TypeToken<List<PaymentMethod>>() {
                            }.getType());

                    if (clientAcceptedPaymentMethods != null) {
                        List<PaymentMethod> result = new ArrayList<>();

                        for (PaymentMethod clientPaymentMethod : clientAcceptedPaymentMethods) {
                            if (paymentMethods.contains(clientPaymentMethod)) {
                                result.add(clientPaymentMethod);
                            }
                        }

                        return result.toArray(new PaymentMethod[result.size()]);
                    } else {
                        return paymentMethods.toArray(new PaymentMethod[paymentMethods.size()]);
                    }
                }
            }

            return new PaymentMethod[0];
        }
    }

    public static class PaymentInformation {
        public String qrCodeContent;
        public String encryptedOrigin;
    }

    public static class CheckoutProcessRequest {
        public SignedCheckoutInfo signedCheckoutInfo;
        public PaymentMethod paymentMethod;
        public PaymentInformation paymentInformation;
    }

    public enum PaymentState {
        @SerializedName("pending")
        PENDING,
        @SerializedName("successful")
        SUCCESSFUL,
        @SerializedName("failed")
        FAILED,
    }

    public static class CheckoutProcessResponse {
        public Map<String, Href> links;
        public Boolean supervisorApproval;
        public Boolean paymentApproval;
        public boolean aborted;
        public JsonObject checkoutInfo;
        public PaymentMethod paymentMethod;
        public boolean modified;
        public PaymentInformation paymentInformation;
        public PaymentState paymentState;

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
        void success(SignedCheckoutInfo signedCheckoutInfo, int onlinePrice, PaymentMethod[] availablePaymentMethods);
        void noShop();
        void error();
    }

    public interface PaymentProcessResult {
        void success(CheckoutProcessResponse checkoutProcessResponse);
        void aborted();
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

    public void abort(CheckoutProcessResponse checkoutProcessResponse) {
        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(checkoutProcessResponse.getSelfLink()))
                .patch(RequestBody.create(JSON, "{\"aborted\":true}"))
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    Logger.d("Payment aborted");
                } else {
                    Logger.e("Could not abort payment");
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e("Could not abort payment");
            }
        });
    }

    public void createCheckoutInfo(final Shop shop,
                                   final String cartJson,
                                   final PaymentMethod[] clientAcceptedPaymentMethods,
                                   final CheckoutInfoResult checkoutInfoResult) {
        String checkoutUrl = project.getCheckoutUrl();
        if (checkoutUrl == null) {
            Logger.e("Could not checkout, no checkout url provided in metadata");
            checkoutInfoResult.error();
            return;
        }

        if (shop == null) {
            Logger.e("Could not checkout, no shop selected");
            checkoutInfoResult.noShop();
            return;
        }

        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(checkoutUrl))
                .post(RequestBody.create(JSON, cartJson))
                .build();

        cancel();

        call = okHttpClient.newCall(request);
        call.enqueue(new SimpleJsonCallback<SignedCheckoutInfo>(SignedCheckoutInfo.class) {
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

                if (price != project.getShoppingCart().getTotalPrice()) {
                    Logger.w("Warning local price is different from remotely calculated price! (Local: "
                            + project.getShoppingCart().getTotalPrice() + ", Remote: " + price + ")");
                }

                PaymentMethod[] availablePaymentMethods = signedCheckoutInfo.getAvailablePaymentMethods(clientAcceptedPaymentMethods);
                if (availablePaymentMethods != null && availablePaymentMethods.length > 0) {
                    checkoutInfoResult.success(signedCheckoutInfo, price, availablePaymentMethods);
                } else {
                    checkoutInfoResult.error();
                }
            }

            @Override
            public void error(Throwable t) {
                Logger.e("Error while trying to check out");
                checkoutInfoResult.error();
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
                                     final PaymentProcessResult paymentProcessResult) {
        CheckoutProcessRequest checkoutProcessRequest = new CheckoutProcessRequest();
        checkoutProcessRequest.paymentMethod = paymentMethod;
        checkoutProcessRequest.signedCheckoutInfo = signedCheckoutInfo;

        if (paymentCredentials != null) {
            checkoutProcessRequest.paymentInformation = new PaymentInformation();
            checkoutProcessRequest.paymentInformation.encryptedOrigin = paymentCredentials.getEncryptedData();
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
