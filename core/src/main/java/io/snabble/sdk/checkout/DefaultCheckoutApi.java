package io.snabble.sdk.checkout;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Snabble;
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

public class DefaultCheckoutApi implements CheckoutApi {
    private static final MediaType JSON = MediaType.parse("application/json");

    private final Project project;
    private final ShoppingCart shoppingCart;
    private final OkHttpClient okHttpClient;
    private Call call;

    public DefaultCheckoutApi(Project project, ShoppingCart shoppingCart) {
        this.project = project;
        this.shoppingCart = shoppingCart;
        this.okHttpClient = project.getOkHttpClient();
    }

    @Override
    public void cancel() {
        if (call != null) {
            call.cancel();
            call = null;
        }
    }

    @Override
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

    @Override
    public void createCheckoutInfo(final ShoppingCart.BackendCart backendCart,
                                   final List<? extends PaymentMethod> clientAcceptedPaymentMethods,
                                   final CheckoutInfoResult checkoutInfoResult,
                                   final long timeout) {
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

        OkHttpClient okClient = okHttpClient;

        if (timeout > 0) {
            okClient = okClient.newBuilder()
                    .callTimeout(timeout, TimeUnit.MILLISECONDS)
                    .build();
        }

        call = okClient.newCall(request);
        call.enqueue(new JsonCallback<SignedCheckoutInfo, JsonObject>(SignedCheckoutInfo.class, JsonObject.class) {
            @Override
            public void success(SignedCheckoutInfo signedCheckoutInfo) {
                int price;

                if (signedCheckoutInfo.getCheckoutInfo().has("price")
                        && signedCheckoutInfo.getCheckoutInfo().get("price").getAsJsonObject().has("price")) {
                    price = signedCheckoutInfo.getCheckoutInfo()
                            .get("price")
                            .getAsJsonObject()
                            .get("price")
                            .getAsInt();
                } else {
                    price = shoppingCart.getTotalPrice();
                }

                List<PaymentMethodInfo> availablePaymentMethods = signedCheckoutInfo.getAvailablePaymentMethods(clientAcceptedPaymentMethods);
                if (!availablePaymentMethods.isEmpty()) {
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
                            for (int i=0; i<shoppingCart.size(); i++) {
                                Product product = shoppingCart.get(i).getProduct();
                                if (product != null) {
                                    if (invalidSkus.contains(product.getSku())) {
                                        invalidProducts.add(product);
                                    }
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
                        case "invalid_deposit_return_voucher":
                            checkoutInfoResult.invalidDepositReturnVoucher();
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

    @Override
    public void updatePaymentProcess(final String url,
                                     final PaymentProcessResult paymentProcessResult) {
        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .get()
                .build();

        cancel();

        call = okHttpClient.newCall(request);
        call.enqueue(new SimpleJsonCallback<CheckoutProcessResponse>(CheckoutProcessResponse.class) {
            @Override
            public void success(CheckoutProcessResponse checkoutProcessResponse) {
                paymentProcessResult.success(checkoutProcessResponse, rawResponse());
                DefaultCheckoutApi.this.call = null;
            }

            @Override
            public void error(Throwable t) {
                paymentProcessResult.error();
            }
        });
    }

    @Override
    public void updatePaymentProcess(final CheckoutProcessResponse checkoutProcessResponse,
                                     final PaymentProcessResult paymentProcessResult) {
        String url = checkoutProcessResponse.getSelfLink();
        if (url == null) {
            paymentProcessResult.error();
            return;
        }

        updatePaymentProcess(url, paymentProcessResult);
    }

    @Override
    public void createPaymentProcess(final String id,
                                     final SignedCheckoutInfo signedCheckoutInfo,
                                     final PaymentMethod paymentMethod,
                                     final PaymentCredentials paymentCredentials,
                                     final boolean processedOffline,
                                     final Date finalizedAt,
                                     final PaymentProcessResult paymentProcessResult) {
        CheckoutProcessRequest checkoutProcessRequest = new CheckoutProcessRequest();
        checkoutProcessRequest.setPaymentMethod(paymentMethod);
        checkoutProcessRequest.setSignedCheckoutInfo(signedCheckoutInfo);

        if (processedOffline) {
            checkoutProcessRequest.setProcessedOffline(true);

            if (finalizedAt != null) {
                checkoutProcessRequest.setFinalizedAt(DateUtils.toRFC3339(finalizedAt));
            }
        }

        if (paymentCredentials != null) {
            checkoutProcessRequest.setPaymentInformation(new PaymentInformation());

            String data = paymentCredentials.getEncryptedData();
            if (data == null) {
                paymentProcessResult.error();
                return;
            }

            checkoutProcessRequest.getPaymentInformation().setOriginType(paymentCredentials.getType().getOriginType());
            checkoutProcessRequest.getPaymentInformation().setEncryptedOrigin(data);

            if (paymentCredentials.getType() == PaymentCredentials.Type.CREDIT_CARD_PSD2) {
                Date date = new Date(paymentCredentials.getValidTo());
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
                checkoutProcessRequest.getPaymentInformation().setValidUntil(simpleDateFormat.format(date));
                checkoutProcessRequest.getPaymentInformation().setCardNumber(paymentCredentials.getObfuscatedId());
            } else if (paymentCredentials.getType() == PaymentCredentials.Type.PAYDIREKT) {
                Map<String, String> additionalData = paymentCredentials.getAdditionalData();
                if (additionalData != null) {
                    checkoutProcessRequest.getPaymentInformation().setDeviceID(additionalData.get("deviceID"));
                    checkoutProcessRequest.getPaymentInformation().setDeviceName(additionalData.get("deviceName"));
                    checkoutProcessRequest.getPaymentInformation().setDeviceFingerprint(additionalData.get("deviceFingerprint"));
                    checkoutProcessRequest.getPaymentInformation().setDeviceIPAddress(additionalData.get("deviceIPAddress"));
                }
            }
        }

        String url = signedCheckoutInfo.getCheckoutProcessLink();
        if (url == null) {
            paymentProcessResult.error();
            return;
        }

        url = url + "/" + id;
        String json = GsonHolder.get().toJson(checkoutProcessRequest);
        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .put(RequestBody.create(JSON, json))
                .build();

        cancel();

        call = okHttpClient.newCall(request);

        String finalUrl = url;
        call.enqueue(new SimpleJsonCallback<CheckoutProcessResponse>(CheckoutProcessResponse.class) {
            @Override
            public void success(CheckoutProcessResponse checkoutProcess) {
                paymentProcessResult.success(checkoutProcess, rawResponse());
            }

            @Override
            public void error(Throwable t) {
                if (responseCode() == 403) {
                    updatePaymentProcess(finalUrl, paymentProcessResult);
                } else {
                    paymentProcessResult.error();
                }
            }
        });
    }

    @Override
    public void authorizePayment(final CheckoutProcessResponse checkoutProcessResponse,
                                 final AuthorizePaymentRequest authorizePaymentRequest,
                                 final AuthorizePaymentResult authorizePaymentResult) {
        String url = checkoutProcessResponse.getAuthorizePaymentLink();
        if (url == null) {
            authorizePaymentResult.error();
            return;
        }

        String json = GsonHolder.get().toJson(authorizePaymentRequest);
        final Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .post(RequestBody.create(JSON, json))
                .build();

        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                authorizePaymentResult.error();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    authorizePaymentResult.success();
                } else {
                    authorizePaymentResult.error();
                }
            }
        });
    }
}
