package io.snabble.sdk;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

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
         * Request a payment authorization token.
         *
         * For example a Google Pay payment token that needs to get sent back to
         * the snabble Backend.
         */
        REQUEST_PAYMENT_AUTHORIZATION_TOKEN,
        /**
         * Payment was received by the backend and we are waiting for confirmation of the payment provider
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

    private final Project project;
    private final CheckoutApi checkoutApi;
    private final ShoppingCart shoppingCart;

    private CheckoutApi.SignedCheckoutInfo signedCheckoutInfo;
    private CheckoutApi.CheckoutProcessResponse checkoutProcess;
    private String rawCheckoutProcess;
    private PaymentMethod paymentMethod;
    private int priceToPay;

    private final List<OnCheckoutStateChangedListener> checkoutStateListeners = new CopyOnWriteArrayList<>();
    private final List<OnFulfillmentUpdateListener> fulfillmentUpdateListeners = new CopyOnWriteArrayList<>();

    private State lastState = Checkout.State.NONE;
    private State state = Checkout.State.NONE;

    private final List<String> codes = new ArrayList<>();
    private PaymentMethod[] clientAcceptedPaymentMethods;
    private Shop shop;
    private List<Product> invalidProducts;
    private final CheckoutRetryer checkoutRetryer;
    private Future<?> currentPollFuture;
    private final PaymentOriginCandidateHelper paymentOriginCandidateHelper;
    private CheckoutApi.AuthorizePaymentRequest storedAuthorizePaymentRequest;
    private boolean authorizePaymentRequestFailed;

    Checkout(Project project) {
        this.project = project;
        this.shoppingCart = project.getShoppingCart();
        this.checkoutApi = new CheckoutApi(project);
        this.checkoutRetryer = new CheckoutRetryer(project, getFallbackPaymentMethod());
        this.paymentOriginCandidateHelper = new PaymentOriginCandidateHelper(project);
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
        cancelOutstandingCalls();

        if (state != Checkout.State.PAYMENT_APPROVED
                && state != Checkout.State.DENIED_BY_PAYMENT_PROVIDER
                && state != Checkout.State.DENIED_BY_SUPERVISOR
                && checkoutProcess != null) {
            checkoutApi.abort(checkoutProcess, new CheckoutApi.PaymentAbortResult() {
                @Override
                public void success() {
                    synchronized (Checkout.this) {
                        if (error) {
                            notifyStateChanged(Checkout.State.PAYMENT_PROCESSING_ERROR);
                        } else {
                            notifyStateChanged(Checkout.State.PAYMENT_ABORTED);
                        }

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
                        notifyStateChanged(Checkout.State.PAYMENT_ABORT_FAILED);
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
        stopPolling();
    }

    /**
     * Cancels outstanding http calls and sets the checkout to its initial state.
     * <p>
     * Does NOT notify the backend that the checkout was cancelled.
     */
    public void reset() {
        cancelOutstandingCalls();
        notifyStateChanged(Checkout.State.NONE);

        invalidProducts = null;
        paymentMethod = null;
        shoppingCart.generateNewUUID();
        shop = null;
    }

    public void resume() {
        if (lastState == Checkout.State.WAIT_FOR_APPROVAL || lastState == State.REQUEST_PAYMENT_AUTHORIZATION_TOKEN) {
            notifyStateChanged(Checkout.State.WAIT_FOR_APPROVAL);
            pollForResult();
        }
    }

    public boolean isAvailable() {
        return project.getCheckoutUrl() != null && project.isCheckoutAvailable();
    }

    private PaymentMethod getFallbackPaymentMethod() {
        PaymentMethod[] paymentMethods = project.getAvailablePaymentMethods();
        for (PaymentMethod pm : paymentMethods) {
            if (pm.isOfflineMethod()) {
                return pm;
            }
        }

        return null;
    }

    public void checkout() {
        checkout(-1);
    }

    /**
     * Starts the checkout process.
     * <p>
     * Requires a shop to be set with {@link Project#setCheckedInShop(Shop)}.
     * <p>
     * If successful and there is more then 1 payment method
     * the checkout state will be {@link State#REQUEST_PAYMENT_METHOD}.
     * You then need to sometime after call @link Checkout#pay(PaymentMethod)}
     * to pay with that payment method.
     */
    public void checkout(long timeout) {
        checkoutProcess = null;
        rawCheckoutProcess = null;
        signedCheckoutInfo = null;
        paymentMethod = null;
        priceToPay = 0;
        invalidProducts = null;
        storedAuthorizePaymentRequest = null;
        shop = project.getCheckedInShop();
        paymentOriginCandidateHelper.reset();

        notifyStateChanged(Checkout.State.HANDSHAKING);

        if (shop == null) {
            notifyStateChanged(Checkout.State.NO_SHOP);
            return;
        }

        final ShoppingCart.BackendCart backendCart = shoppingCart.toBackendCart();

        checkoutApi.createCheckoutInfo(backendCart,
                clientAcceptedPaymentMethods,
                new CheckoutApi.CheckoutInfoResult() {
            @Override
            public void success(CheckoutApi.SignedCheckoutInfo checkoutInfo,
                                int onlinePrice,
                                CheckoutApi.PaymentMethodInfo[] availablePaymentMethods) {
                signedCheckoutInfo = checkoutInfo;
                priceToPay = shoppingCart.getTotalPrice();

                if (availablePaymentMethods.length == 1) {
                    PaymentMethod paymentMethod = PaymentMethod.fromString(availablePaymentMethods[0].id);
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
                if(fallback != null) {
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
                    paymentMethod, paymentCredentials, false, null, new CheckoutApi.PaymentProcessResult() {
                @Override
                public void success(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse, String rawResponse) {
                    synchronized (Checkout.this) {
                        checkoutProcess = checkoutProcessResponse;
                        rawCheckoutProcess = rawResponse;

                        if (!handleProcessResponse()) {
                            boolean allChecksOk = runChecks(checkoutProcessResponse);

                            if (allChecksOk) {
                                if (checkoutProcessResponse.paymentState == CheckoutApi.State.PROCESSING) {
                                    Logger.d("Processing payment...");
                                    notifyStateChanged(Checkout.State.PAYMENT_PROCESSING);
                                } else {
                                    Logger.d("Waiting for approval...");
                                    notifyStateChanged(Checkout.State.WAIT_FOR_APPROVAL);
                                }
                            }

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
        CheckoutApi.AuthorizePaymentRequest authorizePaymentRequest = new CheckoutApi.AuthorizePaymentRequest();
        authorizePaymentRequest.encryptedOrigin = encryptedOrigin;
        storedAuthorizePaymentRequest = authorizePaymentRequest;

        checkoutApi.authorizePayment(checkoutProcess,
                authorizePaymentRequest,
                new CheckoutApi.AuthorizePaymentResult() {
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

    private boolean runChecks(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse) {
        boolean allChecksOk = true;

        if (checkoutProcessResponse.checks != null) {
            for (CheckoutApi.Check check : checkoutProcessResponse.checks) {
                if (check.type == null || check.state == null) {
                    continue;
                }

                if (check.state == CheckoutApi.State.FAILED) {
                    return false;
                }

                if (check.performedBy == CheckoutApi.Performer.APP) {
                    if (check.type == CheckoutApi.CheckType.MIN_AGE) {
                        Logger.d("Verifying age...");
                        switch (check.state) {
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
        }

        return allChecksOk;
    }

    private boolean areAllFulfillmentsClosed() {
        if (checkoutProcess == null || checkoutProcess.fulfillments == null) {
            return true;
        }

        boolean ok = true;

        for (CheckoutApi.Fulfillment fulfillment : checkoutProcess.fulfillments)  {
            if (fulfillment.state.isOpen()) {
                ok = false;
                break;
            }
        }

        return ok;
    }

    private boolean hasAnyFulfillmentFailed() {
        if (checkoutProcess == null || checkoutProcess.fulfillments == null) {
            return false;
        }

        boolean fail = false;

        for (CheckoutApi.Fulfillment fulfillment : checkoutProcess.fulfillments)  {
            if (fulfillment.state.isFailure()) {
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

        checkoutApi.updatePaymentProcess(checkoutProcess, new CheckoutApi.PaymentProcessResult() {
            @Override
            public void success(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse, String rawResponse) {
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
                || state == State.REQUEST_PAYMENT_AUTHORIZATION_TOKEN
                || state == Checkout.State.PAYMENT_PROCESSING
                || (state == State.PAYMENT_APPROVED && !areAllFulfillmentsClosed())) {
            scheduleNextPoll();
        }
    }

    private boolean handleProcessResponse() {
        if (checkoutProcess.aborted) {
            Logger.d("Payment aborted");
            notifyStateChanged(Checkout.State.PAYMENT_ABORTED);
            return true;
        }

        paymentOriginCandidateHelper.startPollingIfLinkIsAvailable(checkoutProcess);

        String authorizePaymentUrl = checkoutProcess.getAuthorizePaymentLink();
        if (authorizePaymentUrl != null) {
            if (authorizePaymentRequestFailed) {
                authorizePaymentRequestFailed = false;
                authorizePayment(storedAuthorizePaymentRequest.encryptedOrigin);
            } else {
                if (storedAuthorizePaymentRequest != null) {
                    authorizePayment(storedAuthorizePaymentRequest.encryptedOrigin);
                } else {
                    notifyStateChanged(State.REQUEST_PAYMENT_AUTHORIZATION_TOKEN);
                }
            }

            return false;
        }

        if (checkoutProcess.paymentState == CheckoutApi.State.SUCCESSFUL) {
            if (checkoutProcess.exitToken != null
                    && (checkoutProcess.exitToken.format == null || checkoutProcess.exitToken.format.equals("")
                    || checkoutProcess.exitToken.value == null || checkoutProcess.exitToken.value.equals(""))) {
                return false;
            } else {
                approve();

                if (areAllFulfillmentsClosed()) {
                    if (checkoutProcess.fulfillments != null) {
                        notifyFulfillmentDone();
                    }
                    return true;
                } else {
                    notifyFulfillmentUpdate();
                    return false;
                }
            }
        } else if (checkoutProcess.paymentState == CheckoutApi.State.PENDING
                || checkoutProcess.paymentState == CheckoutApi.State.UNAUTHORIZED) {
            if (hasAnyFulfillmentFailed()) {
                checkoutApi.abort(checkoutProcess, null);
                notifyStateChanged(State.PAYMENT_ABORTED);
                notifyFulfillmentDone();
                return true;
            }

            if (!runChecks(checkoutProcess)) {
                Logger.d("Payment denied by supervisor");
                shoppingCart.generateNewUUID();
                notifyStateChanged(Checkout.State.DENIED_BY_SUPERVISOR);
            }

            if (checkoutProcess.supervisorApproval != null && !checkoutProcess.supervisorApproval) {
                Logger.d("Payment denied by supervisor");
                shoppingCart.generateNewUUID();
                notifyStateChanged(Checkout.State.DENIED_BY_SUPERVISOR);
                return true;
            } else if (checkoutProcess.paymentApproval != null && !checkoutProcess.paymentApproval) {
                Logger.d("Payment denied by payment provider");
                shoppingCart.generateNewUUID();
                notifyStateChanged(Checkout.State.DENIED_BY_PAYMENT_PROVIDER);
                return true;
            }
        } else if (checkoutProcess.paymentState == CheckoutApi.State.PROCESSING) {
            notifyStateChanged(State.PAYMENT_PROCESSING);
        } else if (checkoutProcess.paymentState == CheckoutApi.State.FAILED) {
            if (checkoutProcess.paymentResult != null
                    && checkoutProcess.paymentResult.failureCause != null
                    && checkoutProcess.paymentResult.failureCause.equals("terminalAbort")) {
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

            if (paymentMethod.isOfflineMethod()) {
                shoppingCart.backup();
            }

            shoppingCart.invalidate();
            clearCodes();
            notifyStateChanged(Checkout.State.PAYMENT_APPROVED);

            Snabble.getInstance().getUsers().update();
        }
    }

    public void approveOfflineMethod() {
        if (paymentMethod != null && paymentMethod.isOfflineMethod()
         || paymentMethod == PaymentMethod.CUSTOMERCARD_POS) {
            approve();
        }
    }

    public String getOrderId() {
        return checkoutProcess != null ? checkoutProcess.orderId : null;
    }

    public void setClientAcceptedPaymentMethods(PaymentMethod[] acceptedPaymentMethods) {
        clientAcceptedPaymentMethods = acceptedPaymentMethods;
    }

    public PaymentMethod[] getClientAcceptedPaymentMethods() {
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
     * Deprecated. Use {@link Project#getCheckedInShop()} instead.
     */
    @Deprecated
    public Shop getShop() {
        return project.getCheckedInShop();
    }

    /**
     * Deprecated. Use {@link Project#setCheckedInShop(Shop shop)} instead.
     */
    @Deprecated
    public void setShop(Shop shop) {
        project.setCheckedInShop(shop);
    }

    /**
     * Gets the currently selected payment method, or null if currently none is selected.
     */
    public PaymentMethod getSelectedPaymentMethod() {
        return paymentMethod;
    }

    public PaymentMethod getPaymentMethodForPayment() {
        if (checkoutProcess != null) {
            return checkoutProcess.paymentMethod;
        }

        return null;
    }

    public int getPriceToPay() {
        return priceToPay;
    }

    public int getVerifiedOnlinePrice() {
        try {
            if (checkoutProcess != null) {
                return checkoutProcess.checkoutInfo.get("price")
                        .getAsJsonObject()
                        .get("price")
                        .getAsInt();
            }
        } catch (Exception e) {
            return -1;
        }

        return -1;
    }

    public List<Product> getInvalidProducts() {
        return invalidProducts;
    }

    public PaymentOriginCandidateHelper getPaymentOriginCandidateHelper() {
        return paymentOriginCandidateHelper;
    }

    /**
     * Gets all available payment methods, callable after {@link State#REQUEST_PAYMENT_METHOD}.
     */
    public PaymentMethod[] getAvailablePaymentMethods() {
        if (signedCheckoutInfo != null) {
            CheckoutApi.PaymentMethodInfo[] paymentMethodInfos =  signedCheckoutInfo.getAvailablePaymentMethods(clientAcceptedPaymentMethods);
            List<PaymentMethod> paymentMethods = new ArrayList<>();
            for (CheckoutApi.PaymentMethodInfo info : paymentMethodInfos) {
                PaymentMethod pm = PaymentMethod.fromString(info.id);
                if (pm != null) {
                    paymentMethods.add(pm);
                }
            }

            return paymentMethods.toArray(new PaymentMethod[paymentMethods.size()]);
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
            if (checkoutProcess.paymentInformation != null) {
                return checkoutProcess.paymentInformation.qrCodeContent;
            }
        }

        return null;
    }

    public CheckoutApi.CheckoutProcessResponse getCheckoutProcess() {
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
        notifyStateChanged(state, false);
    }

    private void notifyStateChanged(final State state, boolean repeat) {
        synchronized (Checkout.this) {
            if (this.state != state || repeat) {
                this.lastState = this.state;
                this.state = state;

                Dispatch.mainThread(() -> {
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
        });
    }

    private void notifyFulfillmentDone() {
        Dispatch.mainThread(() -> {
            for (OnFulfillmentUpdateListener checkoutStateListener : fulfillmentUpdateListeners) {
                checkoutStateListener.onFulfillmentDone();
            }
        });
    }
}
