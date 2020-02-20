package io.snabble.sdk;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.payment.PaymentCredentials;
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
         * Payment was received by the backend and we are waiting for confirmation of the payment provider
         */
        WAIT_FOR_APPROVAL,
        /**
         * Payment was approved and is currently processing.
         */
        PAYMENT_PROCESSING,
        /**
         * Happens when a user want to save his transaction details when paying over a terminal.
         *
         * To continue call {@link #continuePaymentProcess()}.
         */
        REQUEST_ADD_PAYMENT_ORIGIN,
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
         * The payment could not be aborted.
         */
        PAYMENT_ABORT_FAILED,
        /**
         * There was a unrecoverable connection error.
         */
        CONNECTION_ERROR,
        /**
         * Invalid products detected. For example if a sale ∂stop was issued.
         */
        INVALID_PRODUCTS,
        /**
         * No payment method available.
         */
        NO_PAYMENT_METHOD_AVAILABLE,
        /**
         * No shop was selected.
         */
        NO_SHOP,
    }

    public class PaymentOrigin {
        public final String name;
        public final String iban;

        PaymentOrigin(String name, String iban) {
            this.name = name;
            this.iban = iban;
        }
    }

    private Project project;
    private CheckoutApi checkoutApi;
    private ShoppingCart shoppingCart;

    private CheckoutApi.SignedCheckoutInfo signedCheckoutInfo;
    private CheckoutApi.CheckoutProcessResponse checkoutProcess;
    private PaymentMethod paymentMethod;
    private int priceToPay;

    private List<OnCheckoutStateChangedListener> checkoutStateListeners = new CopyOnWriteArrayList<>();

    private Handler handler;
    private Handler uiHandler;

    private State lastState = State.NONE;
    private State state = State.NONE;

    private List<String> codes = new ArrayList<>();
    private PaymentMethod[] clientAcceptedPaymentMethods;
    private Shop shop;
    private List<Product> invalidProducts;
    private CheckoutApi.PaymentResult paymentResult;
    private boolean paymentResultHandled;

    private Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollForResult();
        }
    };
    private CheckoutRetryer checkoutRetryer;

    Checkout(Project project) {
        this.project = project;
        this.shoppingCart = project.getShoppingCart();
        this.checkoutApi = new CheckoutApi(project);
        this.checkoutRetryer = new CheckoutRetryer(project, getFallbackPaymentMethod());

        HandlerThread handlerThread = new HandlerThread("Checkout");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        uiHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Aborts outstanding http calls and notifies the backend that the checkout process
     * was cancelled
     */
    public void abort() {
        cancelOutstandingCalls();

        if (state != State.PAYMENT_APPROVED
                && state != State.DENIED_BY_PAYMENT_PROVIDER
                && state != State.DENIED_BY_SUPERVISOR
                && checkoutProcess != null) {
            checkoutApi.abort(checkoutProcess, new CheckoutApi.PaymentAbortResult() {
                @Override
                public void success() {
                    synchronized (Checkout.this) {
                        notifyStateChanged(State.PAYMENT_ABORTED);

                        checkoutProcess = null;
                        invalidProducts = null;
                        paymentMethod = null;
                        shop = null;
                    }
                }

                @Override
                public void error() {
                    notifyStateChanged(State.PAYMENT_ABORT_FAILED);
                }
            });

            project.getShoppingCart().updatePrices(false);
        } else {
            notifyStateChanged(State.NONE);
        }
    }

    /**
     * Cancels outstanding http calls and notifies the backend that the checkout process
     * was cancelled
     *
      @Deprecated use abort instead
     */
    @Deprecated
    public void cancel() {
        abort();
    }

    /**
     * Aborts outstanding http calls and notifies the backend that the checkout process
     * was cancelled, but does not notify listeners
     */
    public void abortSilently() {
        if (state != State.PAYMENT_APPROVED
                && state != State.DENIED_BY_PAYMENT_PROVIDER
                && state != State.DENIED_BY_SUPERVISOR
                && checkoutProcess != null) {
            checkoutApi.abort(checkoutProcess, null);
        }

        reset();
    }

    /**
     * Aborts outstanding http calls and notifies the backend that the checkout process
     * was cancelled, but does not notify listeners
     *
     * @Deprecated use abortSilently instead
     */
    @Deprecated
    public void cancelSilently() {
        abortSilently();
    }

    public void cancelOutstandingCalls() {
        checkoutApi.cancel();
        handler.removeCallbacks(pollRunnable);
    }

    /**
     * Cancels outstanding http calls and sets the checkout to its initial state.
     * <p>
     * Does NOT notify the backend that the checkout was cancelled.
     */
    public void reset() {
        cancelOutstandingCalls();
        notifyStateChanged(State.NONE);

        checkoutProcess = null;
        invalidProducts = null;
        paymentMethod = null;
        paymentResultHandled = false;
        paymentResult = null;
        shop = null;
    }

    public void resume() {
        if (lastState == State.WAIT_FOR_APPROVAL) {
            notifyStateChanged(State.WAIT_FOR_APPROVAL);
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

    /**
     * Starts the checkout process.
     * <p>
     * Requires a shop to be set with {@link Project#setCheckedInShop(Shop)}.
     * <p>
     * If successful and there is more then 1 payment method
     * the checkout state will be {@link Checkout.State#REQUEST_PAYMENT_METHOD}.
     * You then need to sometime after call @link Checkout#pay(PaymentMethod)}
     * to pay with that payment method.
     */
    public void checkout() {
        checkoutProcess = null;
        signedCheckoutInfo = null;
        paymentMethod = null;
        priceToPay = 0;
        invalidProducts = null;
        paymentResult = null;
        paymentResultHandled = false;
        shop = project.getCheckedInShop();

        notifyStateChanged(State.HANDSHAKING);

        if (shop == null) {
            notifyStateChanged(State.NO_SHOP);
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
                        pay(paymentMethod, null, true);
                    } else {
                        notifyStateChanged(State.REQUEST_PAYMENT_METHOD);
                        Logger.d("Payment method requested");
                    }
                } else {
                    notifyStateChanged(State.REQUEST_PAYMENT_METHOD);
                    Logger.d("Payment method requested");
                }
            }

            @Override
            public void noShop() {
                notifyStateChanged(State.NO_SHOP);
            }

            @Override
            public void invalidProducts(List<Product> products) {
                invalidProducts = products;
                notifyStateChanged(State.INVALID_PRODUCTS);
            }

            @Override
            public void noAvailablePaymentMethod() {
                notifyStateChanged(State.NO_PAYMENT_METHOD_AVAILABLE);
            }

            @Override
            public void unknownError() {
                notifyStateChanged(State.CONNECTION_ERROR);
            }

            @Override
            public void connectionError() {
                PaymentMethod fallback = getFallbackPaymentMethod();
                if(fallback != null) {
                    paymentMethod = fallback;
                    priceToPay = shoppingCart.getTotalPrice();
                    checkoutRetryer.add(backendCart);
                    notifyStateChanged(State.WAIT_FOR_APPROVAL);
                } else {
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
     *
     * @param paymentMethod the payment method to pay with
     * @param paymentCredentials may be null if the payment method requires no payment credentials
     */
    public void pay(PaymentMethod paymentMethod, PaymentCredentials paymentCredentials) {
        pay(paymentMethod, paymentCredentials, false);
    }

    private void pay(final PaymentMethod paymentMethod, final PaymentCredentials paymentCredentials, boolean force) {
        if (signedCheckoutInfo != null) {
            boolean isRequestingPaymentMethod = (state == State.REQUEST_PAYMENT_METHOD);
            boolean wasRequestingPaymentMethod = (lastState == State.REQUEST_PAYMENT_METHOD
                    || lastState == State.VERIFYING_PAYMENT_METHOD);

            if (force || isRequestingPaymentMethod || ((state == State.CONNECTION_ERROR || state == State.NONE) && wasRequestingPaymentMethod)) {
                this.paymentMethod = paymentMethod;

                notifyStateChanged(State.VERIFYING_PAYMENT_METHOD);

                checkoutApi.createPaymentProcess(signedCheckoutInfo, paymentMethod, paymentCredentials,
                        false, null, new CheckoutApi.PaymentProcessResult() {
                    @Override
                    public void success(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse) {
                        synchronized (Checkout.this) {
                            checkoutProcess = checkoutProcessResponse;

                            if (!handleProcessResponse()) {
                                if (checkoutProcessResponse.paymentState == CheckoutApi.PaymentState.PROCESSING) {
                                    Logger.d("Processing payment...");
                                    notifyStateChanged(State.PAYMENT_PROCESSING);
                                } else {
                                    Logger.d("Waiting for approval...");
                                    notifyStateChanged(State.WAIT_FOR_APPROVAL);
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
                        notifyStateChanged(State.CONNECTION_ERROR);
                    }
                });
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

        checkoutApi.updatePaymentProcess(checkoutProcess, new CheckoutApi.PaymentProcessResult() {
            @Override
            public void success(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse) {
                synchronized (Checkout.this) {
                    checkoutProcess = checkoutProcessResponse;

                    if (handleProcessResponse()) {
                        Logger.d("stop polling");
                        handler.removeCallbacks(pollRunnable);
                    }
                }
            }

            @Override
            public void error() {

            }
        });

        if (state == State.WAIT_FOR_APPROVAL
                || state == State.PAYMENT_PROCESSING
                || state == State.REQUEST_ADD_PAYMENT_ORIGIN) {
            scheduleNextPoll();
        }
    }

    private boolean handleProcessResponse() {
        if (checkoutProcess.aborted) {
            Logger.d("Payment aborted");
            notifyStateChanged(State.PAYMENT_ABORTED);
            return true;
        }

        if (checkoutProcess.paymentResult != null
         && checkoutProcess.paymentResult.ehiTechDaysName != null
         && checkoutProcess.paymentResult.ehiTechDaysIban != null) {
            if (!paymentResultHandled) {
                if (paymentResult == null) {
                    Logger.d("Request adding payment origin");
                    paymentResult = checkoutProcess.paymentResult;
                    paymentResultHandled = false;
                    notifyStateChanged(State.REQUEST_ADD_PAYMENT_ORIGIN);
                }

                return false;
            }
        }

        if (checkoutProcess.paymentState == CheckoutApi.PaymentState.SUCCESSFUL) {
            approve();
            return true;
        } else if (checkoutProcess.paymentState == CheckoutApi.PaymentState.PENDING) {
            if (checkoutProcess.supervisorApproval != null && !checkoutProcess.supervisorApproval) {
                Logger.d("Payment denied by supervisor");
                notifyStateChanged(State.DENIED_BY_SUPERVISOR);
                return true;
            } else if (checkoutProcess.paymentApproval != null && !checkoutProcess.paymentApproval) {
                Logger.d("Payment denied by payment provider");
                notifyStateChanged(State.DENIED_BY_PAYMENT_PROVIDER);
                return true;
            }
        } else if (checkoutProcess.paymentState == CheckoutApi.PaymentState.FAILED) {
            Logger.d("Payment denied by payment provider");
            notifyStateChanged(State.DENIED_BY_PAYMENT_PROVIDER);
            return true;
        }

        return false;
    }

    private void approve() {
        if (state != State.PAYMENT_APPROVED) {
            Logger.d("Payment approved");
            shoppingCart.backup();
            shoppingCart.invalidate();
            clearCodes();
            notifyStateChanged(State.PAYMENT_APPROVED);
        }
    }

    public void approveOfflineMethod() {
        if (paymentMethod != null && paymentMethod.isOfflineMethod()) {
            approve();
        }
    }

    /** Continues the payment process after it stopped when requiring user interaction,
     *  for example after REQUEST_ADD_PAYMENT_ORIGIN **/
    public void continuePaymentProcess() {
        Logger.d("Continue payment process");
        paymentResultHandled = true;
        paymentResult = null;
    }

    public String getOrderId() {
        return checkoutProcess != null ? checkoutProcess.orderId : null;
    }

    public void setClientAcceptedPaymentMethods(PaymentMethod[] acceptedPaymentMethods) {
        clientAcceptedPaymentMethods = acceptedPaymentMethods;
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

    public List<Product> getInvalidProducts() {
        return invalidProducts;
    }

    public PaymentOrigin getPaymentOrigin() {
        if (paymentResult != null
                && paymentResult.ehiTechDaysIban != null
                && paymentResult.ehiTechDaysName != null) {
            return new PaymentOrigin(paymentResult.ehiTechDaysName,
                    paymentResult.ehiTechDaysIban);
        }

        return null;
    }

    /**
     * Gets all available payment methods, callable after {@link Checkout.State#REQUEST_PAYMENT_METHOD}.
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

    public void processPendingCheckouts() {
        checkoutRetryer.processPendingCheckouts();
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
        synchronized (Checkout.this) {
            if (this.state != state) {
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
    }
}
