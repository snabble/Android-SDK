package io.snabble.sdk.checkout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import io.snabble.sdk.Coupon;
import io.snabble.sdk.FulfillmentState;
import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.PaymentMethodDescriptor;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.Shop;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.utils.Age;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.Logger;

public class Checkout {
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
         * Age needs to be verified.
         */
        REQUEST_VERIFY_AGE,
        /**
         * Ask the user for the taxation method.
         */
        REQUEST_TAXATION,
        /**
         * Request a payment authorization token.
         *
         * For example a Google Pay payment token that needs to get sent back to
         * the snabble Backend.
         */
        REQUEST_PAYMENT_AUTHORIZATION_TOKEN,
        /**
         * Checkout was received and we wait for confirmation by the supervisor
         */
        WAIT_FOR_SUPERVISOR,
        /**
         * Checkout was received and we wait for confirmation by the gatekeeper
         */
        WAIT_FOR_GATEKEEPER,
        /**
         * Payment was received by the backend and we are waiting for confirmation by the payment provider
         */
        WAIT_FOR_APPROVAL,
        /**
         * Payment was approved and is currently processing.
         */
        PAYMENT_PROCESSING,
        /**
         * The payment was approved. We are done.
         */
        PAYMENT_APPROVED,
        /**
         * Age is too young.
         */
        DENIED_TOO_YOUNG,
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
         * The payment could not be aborted.
         */
        PAYMENT_ABORT_FAILED,
        /**
         * There was a unrecoverable payment processing error.
         */
        PAYMENT_PROCESSING_ERROR,
        /**
         * There was a unrecoverable connection error.
         */
        CONNECTION_ERROR,
        /**
         * Invalid products detected. For example if a sale stop was issued.
         */
        INVALID_PRODUCTS,
        /**
         * No payment method available.
         */
        NO_PAYMENT_METHOD_AVAILABLE,
        /**
         * No shop was selected.
         */
        NO_SHOP
    }
    
    public static final int INVALID_PRICE = -1;
    
    private final Project project;
    private final CheckoutApi checkoutApi;
    private final ShoppingCart shoppingCart;

    private SignedCheckoutInfo signedCheckoutInfo;
    private CheckoutProcessResponse checkoutProcess;
    private String rawCheckoutProcess;
    private PaymentMethod paymentMethod;
    private int priceToPay;

    private final List<OnCheckoutStateChangedListener> checkoutStateListeners = new CopyOnWriteArrayList<>();
    private final List<OnFulfillmentUpdateListener> fulfillmentUpdateListeners = new CopyOnWriteArrayList<>();

    private final MutableLiveData<Checkout.State> checkoutState = new MutableLiveData<>(State.NONE);
    private final MutableLiveData<List<Fulfillment>> fulfillmentState = new MutableLiveData<>();

    private State lastState = Checkout.State.NONE;
    private State state = Checkout.State.NONE;

    private final List<String> codes = new ArrayList<>();
    private List<PaymentMethod> clientAcceptedPaymentMethods;
    private Shop shop;
    private List<Product> invalidProducts;
    private final CheckoutRetryer checkoutRetryer;
    private Future<?> currentPollFuture;
    private AuthorizePaymentRequest storedAuthorizePaymentRequest;
    private boolean authorizePaymentRequestFailed;
    private List<Coupon> redeemedCoupons;

    public Checkout(Project project, ShoppingCart shoppingCart) {
        this(project, shoppingCart, new DefaultCheckoutApi(project, shoppingCart));
    }

    public Checkout(Project project, ShoppingCart shoppingCart, CheckoutApi checkoutApi) {
        this.project = project;
        this.shoppingCart = shoppingCart;
        this.checkoutApi = checkoutApi;
        this.checkoutRetryer = new CheckoutRetryer(project, getFallbackPaymentMethod());
    }

    public void abortError() {
        abort(true);
    }

    public void abort() {
        abort(false);
    }

    /**
     * Aborts outstanding http calls and notifies the backend that the checkout process
     * was cancelled
     */
    public void abort(boolean error) {
        if (state != Checkout.State.PAYMENT_APPROVED
                && state != Checkout.State.DENIED_BY_PAYMENT_PROVIDER
                && state != Checkout.State.DENIED_BY_SUPERVISOR
                && checkoutProcess != null) {
            checkoutApi.abort(checkoutProcess, new PaymentAbortResult() {
                @Override
                public void success() {
                    cancelOutstandingCalls();

                    synchronized (Checkout.this) {
                        if (error) {
                            notifyStateChanged(Checkout.State.PAYMENT_PROCESSING_ERROR);
                        } else {
                            notifyStateChanged(Checkout.State.PAYMENT_ABORTED);
                        }

                        stopPolling();
                        invalidProducts = null;
                        paymentMethod = null;

                        shoppingCart.generateNewUUID();
                        shop = null;
                    }
                }

                @Override
                public void error() {
                    if (error) {
                        notifyStateChanged(Checkout.State.PAYMENT_PROCESSING_ERROR);
                    } else {
                        if (state != State.PAYMENT_PROCESSING && state != State.PAYMENT_APPROVED) {
                            notifyStateChanged(Checkout.State.PAYMENT_ABORT_FAILED);
                        }
                    }
                }
            });

            project.getShoppingCart().updatePrices(false);
        } else {
            notifyStateChanged(Checkout.State.NONE);
        }
    }

    /**
     * Aborts outstanding http calls and notifies the backend that the checkout process
     * was cancelled, but does not notify listeners
     */
    public void abortSilently() {
        if (state != Checkout.State.PAYMENT_APPROVED
                && state != Checkout.State.DENIED_BY_PAYMENT_PROVIDER
                && state != Checkout.State.DENIED_BY_SUPERVISOR
                && checkoutProcess != null) {
            checkoutApi.abort(checkoutProcess, null);
        }

        reset();
    }

    public void cancelOutstandingCalls() {
        checkoutApi.cancel();
    }

    /**
     * Cancels outstanding http calls and sets the checkout to its initial state.
     * <p>
     * Does NOT notify the backend that the checkout was cancelled.
     */
    public void reset() {
        cancelOutstandingCalls();
        stopPolling();
        notifyStateChanged(Checkout.State.NONE);

        invalidProducts = null;
        paymentMethod = null;
        shoppingCart.generateNewUUID();
        shop = null;
    }

    public boolean isAvailable() {
        return project.getCheckoutUrl() != null && project.isCheckoutAvailable();
    }

    private PaymentMethod getFallbackPaymentMethod() {
        List<PaymentMethodDescriptor> paymentMethods = project.getPaymentMethodDescriptors();
        for (PaymentMethodDescriptor descriptor : paymentMethods) {
            PaymentMethod pm = descriptor.getPaymentMethod();
            if (pm.isOfflineMethod()) {
                return pm;
            }
        }

        return null;
    }

    public void checkout() {
        checkout(-1, false);
    }

    /**
     * Starts the checkout process.
     * <p>
     * Requires a shop to be set with {@link Snabble#setCheckedInShop(Shop)}.
     * <p>
     * If successful and there is more then 1 payment method
     * the checkout state will be {@link State#REQUEST_PAYMENT_METHOD}.
     * You then need to sometime after call @link Checkout#pay(PaymentMethod)}
     * to pay with that payment method.
     */
    public void checkout(long timeout, boolean allowFallbackAfterTimeout) {
        checkoutProcess = null;
        rawCheckoutProcess = null;
        signedCheckoutInfo = null;
        paymentMethod = null;
        priceToPay = 0;
        invalidProducts = null;
        storedAuthorizePaymentRequest = null;
        shop = Snabble.getInstance().getCheckedInShop();
        redeemedCoupons = null;
        fulfillmentState.setValue(null);

        notifyStateChanged(Checkout.State.HANDSHAKING);

        if (shop == null) {
            notifyStateChanged(Checkout.State.NO_SHOP);
            return;
        }

        final ShoppingCart.BackendCart backendCart = shoppingCart.toBackendCart();

        checkoutApi.createCheckoutInfo(backendCart,
                clientAcceptedPaymentMethods,
                new CheckoutInfoResult() {
            @Override
            public void success(SignedCheckoutInfo checkoutInfo,
                                int onlinePrice,
                                List<PaymentMethodInfo> availablePaymentMethods) {
                signedCheckoutInfo = checkoutInfo;

                if (signedCheckoutInfo.isRequiringTaxation()) {
                    Logger.d("Taxation requested");
                    notifyStateChanged(State.REQUEST_TAXATION);
                    return;
                }

                priceToPay = shoppingCart.getTotalPrice();

                if (availablePaymentMethods.size() == 1) {
                    PaymentMethod paymentMethod = PaymentMethod.fromString(availablePaymentMethods.get(0).getId());
                    if (paymentMethod != null && !paymentMethod.isRequiringCredentials()) {
                        pay(paymentMethod, null);
                    } else {
                        notifyStateChanged(Checkout.State.REQUEST_PAYMENT_METHOD);
                        Logger.d("Payment method requested");
                    }
                } else {
                    notifyStateChanged(Checkout.State.REQUEST_PAYMENT_METHOD);
                    Logger.d("Payment method requested");
                }
            }

            @Override
            public void noShop() {
                notifyStateChanged(Checkout.State.NO_SHOP);
            }

            @Override
            public void invalidProducts(List<Product> products) {
                invalidProducts = products;
                notifyStateChanged(Checkout.State.INVALID_PRODUCTS);
            }

            @Override
            public void noAvailablePaymentMethod() {
                notifyStateChanged(Checkout.State.NO_PAYMENT_METHOD_AVAILABLE);
            }

            @Override
            public void invalidDepositReturnVoucher() {
                notifyStateChanged(Checkout.State.CONNECTION_ERROR);
            }

            @Override
            public void unknownError() {
                notifyStateChanged(Checkout.State.CONNECTION_ERROR);
            }

            @Override
            public void connectionError() {
                PaymentMethod fallback = getFallbackPaymentMethod();
                if(fallback != null && allowFallbackAfterTimeout) {
                    paymentMethod = fallback;
                    priceToPay = shoppingCart.getTotalPrice();
                    checkoutRetryer.add(backendCart);
                    notifyStateChanged(Checkout.State.WAIT_FOR_APPROVAL);
                } else {
                    notifyStateChanged(Checkout.State.CONNECTION_ERROR);
                }
            }
        }, timeout);
    }

    /**
     * Needs to be called when the checkout is state {@link State#REQUEST_PAYMENT_METHOD}.
     * <p>
     * When successful the state will switch to {@link State#WAIT_FOR_APPROVAL} which
     * waits for a event of the backend that the payment has been confirmed or denied.
     * <p>
     * Possible states after receiving the event:
     * <p>
     * {@link State#PAYMENT_APPROVED}
     * {@link State#PAYMENT_ABORTED}
     * {@link State#DENIED_BY_PAYMENT_PROVIDER}
     * {@link State#DENIED_BY_SUPERVISOR}
     *
     * @param paymentMethod the payment method to pay with
     * @param paymentCredentials may be null if the payment method requires no payment credentials
     */
    public void pay(final PaymentMethod paymentMethod, final PaymentCredentials paymentCredentials) {
        if (signedCheckoutInfo != null) {
            this.paymentMethod = paymentMethod;

            notifyStateChanged(Checkout.State.VERIFYING_PAYMENT_METHOD);

            checkoutApi.createPaymentProcess(shoppingCart.getUUID(), signedCheckoutInfo,
                    paymentMethod, paymentCredentials, false, null, new PaymentProcessResult() {
                @Override
                public void success(CheckoutProcessResponse checkoutProcessResponse, String rawResponse) {
                    synchronized (Checkout.this) {
                        checkoutProcess = checkoutProcessResponse;
                        rawCheckoutProcess = rawResponse;

                        if (!handleProcessResponse()) {
                            if (!paymentMethod.isOfflineMethod()) {
                                scheduleNextPoll();
                            }
                        }
                    }
                }

                @Override
                public void error() {
                    Logger.e("Connection error while creating checkout process");
                    notifyStateChanged(Checkout.State.CONNECTION_ERROR);
                }
            });
        } else {
            Logger.e("Invalid checkout state");
            notifyStateChanged(Checkout.State.CONNECTION_ERROR);
        }
    }

    public void authorizePayment(String encryptedOrigin) {
        AuthorizePaymentRequest authorizePaymentRequest = new AuthorizePaymentRequest();
        authorizePaymentRequest.setEncryptedOrigin(encryptedOrigin);
        storedAuthorizePaymentRequest = authorizePaymentRequest;

        checkoutApi.authorizePayment(checkoutProcess,
                authorizePaymentRequest,
                new AuthorizePaymentResult() {
                    @Override
            public void success() {
                // ignore
            }

            @Override
            public void error() {
                authorizePaymentRequestFailed = true;
            }
        });
    }

    private boolean areAllChecksSucceeded(CheckoutProcessResponse checkoutProcessResponse) {
        boolean allChecksOk = true;

        for (Check check : checkoutProcessResponse.getChecks()) {
            if (check.getState() != CheckState.SUCCESSFUL) {
                return false;
            }

            if (check.getType() == null) {
                continue;
            }

            if (check.getPerformedBy() == Performer.APP) {
                if (check.getType() == CheckType.MIN_AGE) {
                    Logger.d("Verifying age...");
                    switch (check.getState()) {
                        case PENDING:
                            Logger.d("Age check pending...");
                            notifyStateChanged(State.REQUEST_VERIFY_AGE);
                            abortSilently();
                            allChecksOk = false;
                            break;
                        case FAILED:
                            Logger.d("Age check failed...");
                            notifyStateChanged(State.DENIED_TOO_YOUNG);
                            abortSilently();
                            allChecksOk = false;
                            break;
                        case SUCCESSFUL:
                            Logger.d("Age check successful");
                            break;
                    }
                }
            }
        }

        return allChecksOk;
    }

    private boolean hasAnyCheckFailed(CheckoutProcessResponse checkoutProcessResponse) {
        for (Check check : checkoutProcessResponse.getChecks()) {
            if (check.getState() == CheckState.FAILED) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAnyFulfillmentAllocationFailed() {
        for (Fulfillment fulfillment : checkoutProcess.getFulfillments()) {
            if (fulfillment.getState() == FulfillmentState.ALLOCATION_FAILED ||
                fulfillment.getState() == FulfillmentState.ALLOCATION_TIMED_OUT) {
                return true;
            }
        }

        return false;
    }

    private boolean hasStillPendingChecks(CheckoutProcessResponse checkoutProcessResponse) {
        for (Check check : checkoutProcessResponse.getChecks()) {
            if (check.getState() == CheckState.PENDING) {
                return true;
            }
        }

        return false;
    }

    private boolean areAllFulfillmentsClosed() {
        if (checkoutProcess == null) {
            return true;
        } else {
            checkoutProcess.getFulfillments();
        }

        boolean ok = true;

        for (Fulfillment fulfillment : checkoutProcess.getFulfillments())  {
            if (fulfillment.getState() != null && fulfillment.getState().isOpen()) {
                ok = false;
                break;
            }
        }

        return ok;
    }

    private boolean hasAnyFulfillmentFailed() {
        if (checkoutProcess == null) {
            return false;
        }

        boolean fail = false;

        for (Fulfillment fulfillment : checkoutProcess.getFulfillments())  {
            if (fulfillment.getState() != null && fulfillment.getState().isFailure()) {
                fail = true;
                break;
            }
        }

        return fail;
    }

    private int getUserAge() {
        Date date = Snabble.getInstance().getUserPreferences().getBirthday();
        if (date == null) {
            return -1;
        }

        Age age = Age.calculateAge(date);
        return age.years;
    }

    private void scheduleNextPoll() {
        currentPollFuture = Dispatch.background(this::pollForResult, 2000);
    }

    private void stopPolling() {
        Logger.d("Stop polling");

        if (currentPollFuture != null) {
            currentPollFuture.cancel(true);
        }
    }

    private void pollForResult() {
        if (checkoutProcess == null) {
            notifyStateChanged(Checkout.State.PAYMENT_ABORTED);
            return;
        }

        Logger.d("Polling for approval state...");

        Logger.d("RoutingTarget = " + getRoutingTarget());

        checkoutApi.updatePaymentProcess(checkoutProcess, new PaymentProcessResult() {
            @Override
            public void success(CheckoutProcessResponse checkoutProcessResponse, String rawResponse) {
                synchronized (Checkout.this) {
                    checkoutProcess = checkoutProcessResponse;
                    rawCheckoutProcess = rawResponse;

                    if (handleProcessResponse()) {
                        stopPolling();
                    }
                }
            }

            @Override
            public void error() {

            }
        });

        if (state == Checkout.State.WAIT_FOR_APPROVAL
                || state == State.WAIT_FOR_GATEKEEPER
                || state == State.WAIT_FOR_SUPERVISOR
                || state == State.VERIFYING_PAYMENT_METHOD
                || state == State.REQUEST_PAYMENT_AUTHORIZATION_TOKEN
                || state == Checkout.State.PAYMENT_PROCESSING
                || (state == State.PAYMENT_APPROVED && !areAllFulfillmentsClosed())) {
            scheduleNextPoll();
        }
    }

    private boolean handleProcessResponse() {
        if (checkoutProcess.getAborted()) {
            Logger.d("Payment aborted");
            if (hasAnyFulfillmentFailed()) {
                notifyStateChanged(State.PAYMENT_PROCESSING_ERROR);
            } else {
                notifyStateChanged(Checkout.State.PAYMENT_ABORTED);
            }
            return true;
        }

        if (state == State.VERIFYING_PAYMENT_METHOD) {
            if (checkoutProcess.getRoutingTarget() == RoutingTarget.SUPERVISOR) {
                notifyStateChanged(State.WAIT_FOR_SUPERVISOR);
            } else if (checkoutProcess.getRoutingTarget() == RoutingTarget.GATEKEEPER) {
                notifyStateChanged(State.WAIT_FOR_GATEKEEPER);
            } else {
                notifyStateChanged(State.WAIT_FOR_APPROVAL);
            }

            return false;
        }

        String authorizePaymentUrl = checkoutProcess.getAuthorizePaymentLink();
        if (authorizePaymentUrl != null) {
            if (authorizePaymentRequestFailed) {
                authorizePaymentRequestFailed = false;
                authorizePayment(storedAuthorizePaymentRequest.getEncryptedOrigin());
            } else {
                if (storedAuthorizePaymentRequest != null) {
                    authorizePayment(storedAuthorizePaymentRequest.getEncryptedOrigin());
                } else {
                    notifyStateChanged(State.REQUEST_PAYMENT_AUTHORIZATION_TOKEN);
                }
            }

            return false;
        }

        if (checkoutProcess.getPaymentState() == CheckState.SUCCESSFUL) {
            if (checkoutProcess.getExitToken() != null
                    && (checkoutProcess.getExitToken().getFormat() == null || checkoutProcess.getExitToken().getFormat().equals("")
                    || checkoutProcess.getExitToken().getValue() == null || checkoutProcess.getExitToken().getValue().equals(""))) {
                return false;
            } else {
                approve();

                if (areAllFulfillmentsClosed()) {
                    notifyFulfillmentDone();
                    return true;
                } else {
                    notifyFulfillmentUpdate();
                    return false;
                }
            }
        } else if (checkoutProcess.getPaymentState() == CheckState.PENDING
                || checkoutProcess.getPaymentState() == CheckState.UNAUTHORIZED) {
            if (hasAnyFulfillmentFailed()) {
                checkoutApi.abort(checkoutProcess, null);
                notifyStateChanged(State.PAYMENT_PROCESSING);
                notifyFulfillmentDone();
                return false;
            }

            if (hasAnyFulfillmentAllocationFailed()) {
                notifyStateChanged(State.PAYMENT_ABORTED);
                return true;
            }

            if (hasAnyCheckFailed(checkoutProcess)) {
                Logger.d("Payment denied by supervisor");
                shoppingCart.generateNewUUID();
                notifyStateChanged(Checkout.State.DENIED_BY_SUPERVISOR);
            }
        } else if (checkoutProcess.getPaymentState() == CheckState.PROCESSING) {
            notifyStateChanged(State.PAYMENT_PROCESSING);
        } else if (checkoutProcess.getPaymentState() == CheckState.FAILED) {
            if (checkoutProcess.getPaymentResult() != null
                    && checkoutProcess.getPaymentResult().getFailureCause() != null
                    && checkoutProcess.getPaymentResult().getFailureCause().equals("terminalAbort")) {
                Logger.d("Payment aborted by terminal");
                notifyStateChanged(Checkout.State.PAYMENT_ABORTED);
            } else {
                Logger.d("Payment denied by payment provider");
                notifyStateChanged(Checkout.State.DENIED_BY_PAYMENT_PROVIDER);
            }
            shoppingCart.generateNewUUID();
            return true;
        }

        return false;
    }

    private void approve() {
        if (state != Checkout.State.PAYMENT_APPROVED) {
            Logger.d("Payment approved");

            if (paymentMethod != null && paymentMethod.isOfflineMethod()) {
                shoppingCart.backup();
            }

            if (signedCheckoutInfo != null) {
                List<Coupon> coupons = project.getCoupons().get();
                if (coupons != null) {
                    redeemedCoupons = signedCheckoutInfo.getRedeemedCoupons(coupons);
                } else {
                    redeemedCoupons = null;
                }
            }
            shoppingCart.invalidate();
            clearCodes();
            notifyStateChanged(Checkout.State.PAYMENT_APPROVED);

            Snabble.getInstance().getUsers().update();
        }
    }

    @NonNull
    public List<Coupon> getRedeemedCoupons() {
        if (redeemedCoupons == null) {
            return new ArrayList<>();
        }

        return redeemedCoupons;
    }

    public void approveOfflineMethod() {
        if (paymentMethod != null && paymentMethod.isOfflineMethod()
         || paymentMethod == PaymentMethod.CUSTOMERCARD_POS) {
            shoppingCart.generateNewUUID();
            approve();
        }
    }

    public String getOrderId() {
        return checkoutProcess != null ? checkoutProcess.getOrderId() : null;
    }

    public void setClientAcceptedPaymentMethods(List<PaymentMethod> acceptedPaymentMethods) {
        clientAcceptedPaymentMethods = acceptedPaymentMethods;
    }

    public List<PaymentMethod> getClientAcceptedPaymentMethods() {
        return clientAcceptedPaymentMethods;
    }

    public void addCode(String code) {
        codes.add(code);
    }

    public void removeCode(String code) {
        codes.remove(code);
    }

    public Collection<String> getCodes() {
        return Collections.unmodifiableCollection(codes);
    }

    public void clearCodes() {
        codes.clear();
    }

    /**
     * Gets the currently selected payment method, or null if currently none is selected.
     */
    public PaymentMethod getSelectedPaymentMethod() {
        return paymentMethod;
    }

    public PaymentMethod getPaymentMethodForPayment() {
        if (checkoutProcess != null) {
            return checkoutProcess.getPaymentMethod();
        }

        return null;
    }

    public int getPriceToPay() {
        return priceToPay;
    }

    public int getVerifiedOnlinePrice() {
        try {
            if (checkoutProcess != null) {
                return checkoutProcess.getPricing().getPrice().getPrice();
            }
        } catch (Exception e) {
            return -1;
        }

        return -1;
    }

    public RoutingTarget getRoutingTarget() {
        if (checkoutProcess != null && checkoutProcess.getRoutingTarget() != null) {
            return checkoutProcess.getRoutingTarget();
        }

        return RoutingTarget.NONE;
    }

    public List<Product> getInvalidProducts() {
        return invalidProducts;
    }

    /**
     * Gets all available payment methods, callable after {@link State#REQUEST_PAYMENT_METHOD}.
     */
    public PaymentMethod[] getAvailablePaymentMethods() {
        if (signedCheckoutInfo != null) {
            List<PaymentMethodInfo> paymentMethodInfos =  signedCheckoutInfo.getAvailablePaymentMethods(clientAcceptedPaymentMethods);
            List<PaymentMethod> paymentMethods = new ArrayList<>();
            for (PaymentMethodInfo info : paymentMethodInfos) {
                PaymentMethod pm = PaymentMethod.fromString(info.getId());
                if (pm != null) {
                    paymentMethods.add(pm);
                }
            }

            return paymentMethods.toArray(new PaymentMethod[0]);
        }

        return new PaymentMethod[0];
    }

    /**
     * Gets the unique identifier of the checkout.
     * <p>
     * This id can be used for identification in the supervisor app.
     */
    public String getId() {
        if (checkoutProcess != null) {
            String selfLink = checkoutProcess.getSelfLink();
            if (selfLink == null) {
                return null;
            }

            return selfLink.substring(selfLink.lastIndexOf('/') + 1);
        }

        return null;
    }

    /**
     * Gets the content of the qrcode that needs to be displayed,
     * or null if no qrcode needs to be displayed.
     */
    public String getQRCodePOSContent() {
        if (checkoutProcess != null) {
            if (checkoutProcess.getPaymentInformation() != null) {
                return checkoutProcess.getPaymentInformation().getQrCodeContent();
            }
        }

        return null;
    }

    @Nullable
    public CheckoutProcessResponse getCheckoutProcess() {
        return checkoutProcess;
    }

    public String getRawCheckoutProcessJson() {
        return rawCheckoutProcess;
    }

    public void processPendingCheckouts() {
        checkoutRetryer.processPendingCheckouts();
    }

    /**
     * Gets the current state of the Checkout.
     * <p>
     * See {@link State}.
     *
     * @return the state
     */
    public State getState() {
        return state;
    }

    public interface OnCheckoutStateChangedListener {
        void onStateChanged(State state);
    }

    public LiveData<State> getCheckoutState() {
        return checkoutState;
    }

    public LiveData<List<Fulfillment>> getFulfillmentState() {
        return fulfillmentState;
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
        notifyStateChanged(state, false);
    }

    private void notifyStateChanged(final State state, boolean repeat) {
        synchronized (Checkout.this) {
            if (this.state != state || repeat) {
                this.lastState = this.state;
                this.state = state;

                Dispatch.mainThread(() -> {
                    checkoutState.setValue(state);

                    for (OnCheckoutStateChangedListener checkoutStateListener : checkoutStateListeners) {
                        checkoutStateListener.onStateChanged(state);
                    }
                });
            }
        }
    }

    public interface OnFulfillmentUpdateListener {
        void onFulfillmentUpdated();
        void onFulfillmentDone();
    }

    public void addOnFulfillmentListener(OnFulfillmentUpdateListener listener) {
        if (!fulfillmentUpdateListeners.contains(listener)) {
            fulfillmentUpdateListeners.add(listener);
        }
    }

    public void removeOnFulfillmentListener(OnFulfillmentUpdateListener listener) {
        fulfillmentUpdateListeners.remove(listener);
    }

    private void notifyFulfillmentUpdate() {
        Dispatch.mainThread(() -> {
            for (OnFulfillmentUpdateListener checkoutStateListener : fulfillmentUpdateListeners) {
                checkoutStateListener.onFulfillmentUpdated();
            }

            if (checkoutProcess != null) {
                fulfillmentState.setValue(checkoutProcess.getFulfillments());
            } else {
                fulfillmentState.setValue(null);
            }
        });
    }

    private void notifyFulfillmentDone() {
        Dispatch.mainThread(() -> {
            for (OnFulfillmentUpdateListener checkoutStateListener : fulfillmentUpdateListeners) {
                checkoutStateListener.onFulfillmentDone();
            }

            if (checkoutProcess != null) {
                fulfillmentState.setValue(checkoutProcess.getFulfillments());
            } else {
                fulfillmentState.setValue(null);
            }
        });
    }
}