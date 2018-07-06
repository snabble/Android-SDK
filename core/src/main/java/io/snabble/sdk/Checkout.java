package io.snabble.sdk;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.utils.Logger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Checkout {
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
        @SerializedName(value = "paymentInformation", alternate = "paymentInformations")
        private PaymentInformation paymentInformation;
        private PaymentState paymentState;

        private String getSelfLink() {
            Href link = links.get("self");
            if (link != null && link.href != null) {
                return link.href;
            }
            return null;
        }
    }

    public enum State {
        /**
         * The initial default state.
         */
        NONE,
        /**
         * A checkout request was started, and we are waiting for the backend to confirm.
         */
        HANDSHAKING,
        /**
         * The checkout request is started and confirmed by the backend. We are waiting for
         * selection of the payment method. Usually done by the user.
         * <p>
         * Gets skipped for projects that have only 1 available payment method,
         * that can be selected without user intervention.
         */
        REQUEST_PAYMENT_METHOD,
        /**
         * Payment method was selected and we are waiting for confirmation of the backend.
         */
        VERIFYING_PAYMENT_METHOD,
        /**
         * Payment was received by the backend and we are waiting for confirmation of the payment provider
         */
        WAIT_FOR_APPROVAL,
        /**
         * The payment was approved. We are done.
         */
        PAYMENT_APPROVED,
        /**
         * The payment was denied by the payment provider.
         */
        DENIED_BY_PAYMENT_PROVIDER,
        /**
         * The payment was denied by the supervisor.
         */
        DENIED_BY_SUPERVISOR,
        /**
         * The payment was aborted.
         */
        PAYMENT_ABORTED,
        /**
         * There was a unrecoverable connection error.
         */
        CONNECTION_ERROR,
        /**
         * No shop was selected.
         */
        NO_SHOP,
    }

    private static MediaType JSON = MediaType.parse("application/json");

    private SnabbleSdk sdkInstance;
    private OkHttpClient okHttpClient;
    private ShoppingCart shoppingCart;
    private Gson gson;
    private SignedCheckoutInfo signedCheckoutInfo;
    private CheckoutProcessResponse checkoutProcess;
    private PaymentMethod paymentMethod;
    private int priceToPay;

    private List<OnCheckoutStateChangedListener> checkoutStateListeners = new CopyOnWriteArrayList<>();

    private Handler handler;
    private Handler uiHandler;

    private State lastState = State.NONE;
    private State state = State.NONE;

    private Call call;
    private Shop shop;

    private List<String> codes = new ArrayList<>();
    private PaymentMethod[] clientAcceptedPaymentMethods;

    private Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollForResult();
        }
    };

    Checkout(SnabbleSdk sdkInstance) {
        this.sdkInstance = sdkInstance;
        this.okHttpClient = sdkInstance.getOkHttpClient();
        this.shoppingCart = sdkInstance.getShoppingCart();

        this.gson = new GsonBuilder().create();

        HandlerThread handlerThread = new HandlerThread("Checkout");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        uiHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Cancels outstanding http calls and notifies the backend that the checkout process
     * was cancelled
     */
    public void cancel() {
        cancelOutstandingCalls();

        if (state != State.PAYMENT_APPROVED
                && state != State.DENIED_BY_PAYMENT_PROVIDER
                && state != State.DENIED_BY_SUPERVISOR
                && checkoutProcess != null) {
            final Request request = new Request.Builder()
                    .url(sdkInstance.absoluteUrl(checkoutProcess.getSelfLink()))
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

            notifyStateChanged(State.PAYMENT_ABORTED);
        } else {
            notifyStateChanged(State.NONE);
        }
    }

    /**
     * Cancels outstanding http calls and sets the checkout to its initial state.
     *
     * Does NOT notify the backend that the checkout was cancelled.
     */
    public void reset() {
        cancelOutstandingCalls();
        notifyStateChanged(State.NONE);
    }

    private void cancelOutstandingCalls(){
        if (call != null) {
            call.cancel();
            call = null;
        }

        handler.removeCallbacks(pollRunnable);
        paymentMethod = null;
    }

    public boolean isAvailable() {
        String checkoutUrl = sdkInstance.getCheckoutUrl();
        return checkoutUrl != null;
    }

    /**
     * Starts the checkout process.
     * <p>
     * Requires a shop to be set with {@link Checkout#setShop(Shop)}.
     * <p>
     * If successful and there is more then 1 payment method
     * the checkout state will be {@link Checkout.State#REQUEST_PAYMENT_METHOD}.
     * You then need to sometime after call @link Checkout#pay(PaymentMethod)}
     * to pay with that payment method.
     */
    public void checkout() {
        String checkoutUrl = sdkInstance.getCheckoutUrl();
        if (checkoutUrl == null) {
            Logger.e("Could not checkout, no checkout url provided in metadata");
            notifyStateChanged(State.CONNECTION_ERROR);
            return;
        }

        if (shop == null) {
            Logger.e("Could not checkout, no shop selected");
            notifyStateChanged(State.NO_SHOP);
            return;
        }

        checkoutProcess = null;
        signedCheckoutInfo = null;
        paymentMethod = null;
        priceToPay = 0;

        notifyStateChanged(State.HANDSHAKING);

        String json = sdkInstance.getEvents().getPayloadCartJson();
        final Request request = new Request.Builder()
                .url(sdkInstance.absoluteUrl(checkoutUrl))
                .post(RequestBody.create(JSON, json))
                .build();

        call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body == null) {
                        scheduleNextPoll();
                        return;
                    }

                    InputStream inputStream = body.byteStream();
                    String json = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
                    signedCheckoutInfo = gson.fromJson(json, SignedCheckoutInfo.class);

                    if(signedCheckoutInfo.checkoutInfo.has("price")
                            && signedCheckoutInfo.checkoutInfo.get("price").getAsJsonObject().has("price")) {
                        priceToPay = signedCheckoutInfo.checkoutInfo
                                .get("price")
                                .getAsJsonObject()
                                .get("price")
                                .getAsInt();
                    } else {
                        priceToPay = shoppingCart.getTotalPrice();
                    }

                    if(priceToPay != shoppingCart.getTotalPrice()){
                        Logger.w("Warning local price is different from remotely calculated price! (Local: "
                                + shoppingCart.getTotalPrice() + ", Remote: " + priceToPay + ")");
                    }

                    PaymentMethod[] availablePaymentMethods = getAvailablePaymentMethods();
                    if (availablePaymentMethods != null && availablePaymentMethods.length > 0) {
                        if (availablePaymentMethods.length == 1) {
                            pay(availablePaymentMethods[0], true);
                        } else {
                            notifyStateChanged(State.REQUEST_PAYMENT_METHOD);
                            Logger.d("Payment method requested");
                        }
                    } else {
                        notifyStateChanged(State.CONNECTION_ERROR);
                    }

                    inputStream.close();
                } else {
                    if (!call.isCanceled()) {
                        Logger.e("Error while trying to check out");
                        notifyStateChanged(State.CONNECTION_ERROR);
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                if (!call.isCanceled()) {
                    Logger.e("Error while trying to check out");
                    notifyStateChanged(State.CONNECTION_ERROR);
                }
            }
        });
    }

    /**
     * Needs to be called when the checkout is state {@link Checkout.State#REQUEST_PAYMENT_METHOD}.
     * <p>
     * When successful the state will switch to {@link Checkout.State#WAIT_FOR_APPROVAL} which
     * waits for a event of the backend that the payment has been confirmed or denied.
     * <p>
     * Possible states after receiving the event:
     * <p>
     * {@link Checkout.State#PAYMENT_APPROVED}
     * {@link Checkout.State#PAYMENT_ABORTED}
     * {@link Checkout.State#DENIED_BY_PAYMENT_PROVIDER}
     * {@link Checkout.State#DENIED_BY_SUPERVISOR}
     */
    public void pay(PaymentMethod paymentMethod) {
        pay(paymentMethod, false);
    }

    private void pay(final PaymentMethod paymentMethod, boolean force) {
        if (signedCheckoutInfo != null) {
            boolean isRequestingPaymentMethod = (state == State.REQUEST_PAYMENT_METHOD);
            boolean wasRequestingPaymentMethod = (lastState == State.REQUEST_PAYMENT_METHOD
                    || lastState == State.VERIFYING_PAYMENT_METHOD);

            if (force || isRequestingPaymentMethod || (state == State.CONNECTION_ERROR && wasRequestingPaymentMethod)) {
                this.paymentMethod = paymentMethod;

                CheckoutProcessRequest checkoutProcessRequest = new CheckoutProcessRequest();
                checkoutProcessRequest.paymentMethod = paymentMethod;
                checkoutProcessRequest.signedCheckoutInfo = signedCheckoutInfo;

                String url = signedCheckoutInfo.getCheckoutProcessLink();
                if (url == null) {
                    return;
                }

                String json = gson.toJson(checkoutProcessRequest);
                final Request request = new Request.Builder()
                        .url(sdkInstance.absoluteUrl(url))
                        .post(RequestBody.create(JSON, json))
                        .build();

                call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            ResponseBody body = response.body();
                            if (body == null) {
                                scheduleNextPoll();
                                return;
                            }

                            InputStream inputStream = body.byteStream();
                            String json = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
                            checkoutProcess = gson.fromJson(json, CheckoutProcessResponse.class);
                            if (!handleProcessResponse(checkoutProcess)) {
                                notifyStateChanged(State.WAIT_FOR_APPROVAL);
                                scheduleNextPoll();
                                Logger.d("Waiting for approval...");
                            }

                            inputStream.close();
                        } else {
                            if (!call.isCanceled()) {
                                if (response.code() == 400) {
                                    Logger.e("Bad request - aborting");
                                    notifyStateChanged(State.PAYMENT_ABORTED);
                                } else {
                                    Logger.e("Connection error while creating checkout process");
                                    notifyStateChanged(State.CONNECTION_ERROR);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call call, IOException e) {
                        if (!call.isCanceled()) {
                            Logger.e("Connection error while creating checkout process");
                            notifyStateChanged(State.CONNECTION_ERROR);
                        }
                    }
                });

                notifyStateChanged(State.VERIFYING_PAYMENT_METHOD);
            }
        }
    }

    private void scheduleNextPoll() {
        handler.postDelayed(pollRunnable, 2000);
    }

    private void pollForResult() {
        if (checkoutProcess == null) {
            notifyStateChanged(State.PAYMENT_ABORTED);
            return;
        }

        Logger.d("Polling for approval state...");

        String url = checkoutProcess.getSelfLink();
        if (url == null) {
            return;
        }

        final Request request = new Request.Builder()
                .url(sdkInstance.absoluteUrl(url))
                .get()
                .build();

        if (call != null) {
            call.cancel();
        }

        call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body == null) {
                        return;
                    }

                    InputStream inputStream = body.byteStream();
                    String json = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
                    CheckoutProcessResponse checkoutProcessResponse = gson.fromJson(json,
                            CheckoutProcessResponse.class);

                    if (handleProcessResponse(checkoutProcessResponse)) {
                        handler.removeCallbacks(pollRunnable);
                    }

                    Checkout.this.call = null;
                    inputStream.close();
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {

            }
        });

        if (state == State.WAIT_FOR_APPROVAL) {
            scheduleNextPoll();
        }
    }

    private boolean handleProcessResponse(CheckoutProcessResponse checkoutProcessResponse) {
        if (checkoutProcessResponse.aborted) {
            Logger.d("Payment aborted");
            notifyStateChanged(State.PAYMENT_ABORTED);
            return true;
        }

        if (checkoutProcessResponse.paymentState == PaymentState.SUCCESSFUL) {
            approve();
            return true;
        } else if (checkoutProcessResponse.paymentState == PaymentState.PENDING) {
            if (checkoutProcessResponse.supervisorApproval != null && !checkoutProcessResponse.supervisorApproval) {
                Logger.d("Payment denied by supervisor");
                notifyStateChanged(State.DENIED_BY_SUPERVISOR);
                return true;
            } else if (checkoutProcessResponse.paymentApproval != null && !checkoutProcessResponse.paymentApproval) {
                Logger.d("Payment denied by payment provider");
                notifyStateChanged(State.DENIED_BY_PAYMENT_PROVIDER);
                return true;
            }
        } else if (checkoutProcessResponse.paymentState == PaymentState.FAILED) {
            Logger.d("Payment denied by payment provider");
            notifyStateChanged(State.DENIED_BY_PAYMENT_PROVIDER);
            return true;
        }

        return false;
    }

    private void approve(){
        Logger.d("Payment approved");
        shoppingCart.invalidate();
        clearCodes();
        notifyStateChanged(State.PAYMENT_APPROVED);
    }

    public void approveOfflineMethod() {
        if(paymentMethod.isOfflineMethod()) {
            approve();
        }
    }

    public void setClientAcceptedPaymentMethods(PaymentMethod[] acceptedPaymentMethods){
        clientAcceptedPaymentMethods = acceptedPaymentMethods;
    }

    public void addCode(String code){
        codes.add(code);
    }

    public void removeCode(String code){
        codes.remove(code);
    }

    public Collection<String> getCodes() {
        return Collections.unmodifiableCollection(codes);
    }

    public void clearCodes(){
        codes.clear();
    }

    public Shop getShop() {
        return shop;
    }

    /**
     * Sets the shop used for identification in the payment process.
     */
    public void setShop(Shop shop) {
        this.shop = shop;
        sdkInstance.getEvents().updateShop(shop);
    }

    /**
     * Gets the currently selected payment method, or null if currently none is selected.
     */
    public PaymentMethod getSelectedPaymentMethod() {
        return paymentMethod;
    }

    public int getPriceToPay() {
        return priceToPay;
    }

    /**
     * Gets all available payment methods, callable after {@link Checkout.State#REQUEST_PAYMENT_METHOD}.
     */
    public PaymentMethod[] getAvailablePaymentMethods() {
        if (signedCheckoutInfo != null
                && signedCheckoutInfo.checkoutInfo != null
                && signedCheckoutInfo.checkoutInfo.has("availableMethods")) {
            JsonArray jsonArray = signedCheckoutInfo.checkoutInfo.getAsJsonArray("availableMethods");
            if (jsonArray != null) {
                List<PaymentMethod> paymentMethods = gson.fromJson(jsonArray,
                        new TypeToken<List<PaymentMethod>>(){}.getType());

                if(clientAcceptedPaymentMethods != null) {
                    List<PaymentMethod> result = new ArrayList<>();

                    for (PaymentMethod clientPaymentMethod : clientAcceptedPaymentMethods) {
                        if(paymentMethods.contains(clientPaymentMethod)){
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

    /**
     * Gets the unique identifier of the checkout.
     * <p>
     * The last 4 digits are used for identification in the supervisor app.
     */
    public String getId() {
        if (checkoutProcess != null) {
            String selfLink = checkoutProcess.getSelfLink();
            if (selfLink == null) {
                return null;
            }

            return selfLink.substring(selfLink.lastIndexOf('/') + 1, selfLink.length());
        }

        return null;
    }

    /**
     * Gets the content of the qrcode that needs to be displayed,
     * or null if no qrcode needs to be displayed.
     */
    public String getQRCodePOSContent() {
        if (checkoutProcess != null) {
            if (checkoutProcess.paymentInformation != null) {
                return checkoutProcess.paymentInformation.qrCodeContent;
            }
        }

        return null;
    }

    /**
     * Gets the current state of the Checkout.
     * <p>
     * See {@link Checkout.State}.
     *
     * @return
     */
    public State getState() {
        return state;
    }


    public interface OnCheckoutStateChangedListener {
        void onStateChanged(State state);
    }

    public void addOnCheckoutStateChangedListener(OnCheckoutStateChangedListener listener) {
        if (!checkoutStateListeners.contains(listener)) {
            checkoutStateListeners.add(listener);
        }
    }

    public void removeOnCheckoutStateChangedListener(OnCheckoutStateChangedListener listener) {
        checkoutStateListeners.remove(listener);
    }

    private void notifyStateChanged(final State state) {
        this.lastState = this.state;
        this.state = state;

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                for (OnCheckoutStateChangedListener checkoutStateListener : checkoutStateListeners) {
                    checkoutStateListener.onStateChanged(state);
                }
            }
        });
    }
}
