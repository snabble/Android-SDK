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

    private Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollForResult();
        }
    };

    Checkout(Project project) {
        this.project = project;
        this.shoppingCart = project.getShoppingCart();
        this.checkoutApi = new CheckoutApi(project);

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
            checkoutApi.abort(checkoutProcess);
            notifyStateChanged(State.PAYMENT_ABORTED);
        } else {
            notifyStateChanged(State.NONE);
        }

        checkoutProcess = null;
        invalidProducts = null;
        paymentMethod = null;
        shop = null;
    }

    /**
     * Cancels outstanding http calls and notifies the backend that the checkout process
     * was cancelled, but does not notify listeners
     */
    public void cancelSilently() {
        if (state != State.PAYMENT_APPROVED
                && state != State.DENIED_BY_PAYMENT_PROVIDER
                && state != State.DENIED_BY_SUPERVISOR
                && checkoutProcess != null) {
            checkoutApi.abort(checkoutProcess);
        }

        reset();
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
        shop = null;
    }

    public boolean isAvailable() {
        return project.getCheckoutUrl() != null && project.isCheckoutAvailable();
    }

    private PaymentMethod getFallbackPaymentMethod() {
        if(project.getEncodedCodesOptions() != null) {
            return PaymentMethod.ENCODED_CODES;
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
        shop = project.getCheckedInShop();

        notifyStateChanged(State.HANDSHAKING);

        checkoutApi.createCheckoutInfo(project.getCheckedInShop(),
                shoppingCart.toBackendCart(),
                clientAcceptedPaymentMethods,
                new CheckoutApi.CheckoutInfoResult() {
            @Override
            public void success(CheckoutApi.SignedCheckoutInfo checkoutInfo,
                                int onlinePrice,
                                PaymentMethod[] availablePaymentMethods) {
                signedCheckoutInfo = checkoutInfo;
                priceToPay = shoppingCart.getTotalPrice();

                if (availablePaymentMethods.length == 1 && !availablePaymentMethods[0].isRequiringCredentials()) {
                    pay(availablePaymentMethods[0], null, true);
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
            public void error() {
                PaymentMethod fallback = getFallbackPaymentMethod();
                if(fallback != null) {
                    paymentMethod = fallback;
                    priceToPay = shoppingCart.getTotalPrice();
                    retryPostSilent();
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
                        new CheckoutApi.PaymentProcessResult() {
                    @Override
                    public void success(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse) {
                        checkoutProcess = checkoutProcessResponse;

                        if (!handleProcessResponse()) {
                            Logger.d("Waiting for approval...");
                            notifyStateChanged(State.WAIT_FOR_APPROVAL);

                            if (!paymentMethod.isOfflineMethod()) {
                                scheduleNextPoll();
                            }
                        }
                    }

                    @Override
                    public void aborted() {
                        Logger.e("Bad request - aborting");
                        notifyStateChanged(State.PAYMENT_ABORTED);
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
                checkoutProcess = checkoutProcessResponse;

                if (handleProcessResponse()) {
                    handler.removeCallbacks(pollRunnable);
                }
            }

            @Override
            public void aborted() {

            }

            @Override
            public void error() {

            }
        });

        if (state == State.WAIT_FOR_APPROVAL) {
            scheduleNextPoll();
        }
    }

    private boolean handleProcessResponse() {
        if (checkoutProcess.aborted) {
            Logger.d("Payment aborted");
            notifyStateChanged(State.PAYMENT_ABORTED);
            return true;
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


    private void retryPostSilent() {
        final PaymentMethod pm = paymentMethod;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkoutApi.createCheckoutInfo(shop, shoppingCart.toBackendCart(), clientAcceptedPaymentMethods,
                        new CheckoutApi.CheckoutInfoResult() {
                    @Override
                    public void success(CheckoutApi.SignedCheckoutInfo signedCheckoutInfo,
                                        int onlinePrice,
                                        PaymentMethod[] availablePaymentMethods) {
                        priceToPay = shoppingCart.getTotalPrice();

                        checkoutApi.createPaymentProcess(signedCheckoutInfo, pm, null,
                                new CheckoutApi.PaymentProcessResult() {
                            @Override
                            public void success(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse) {

                            }

                            @Override
                            public void aborted() {

                            }

                            @Override
                            public void error() {

                            }
                        });
                    }

                    @Override
                    public void noShop() {

                    }

                    @Override
                    public void invalidProducts(List<Product> products) {

                    }

                    @Override
                    public void noAvailablePaymentMethod() {

                    }

                    @Override
                    public void error() {

                    }
                });
            }
        }, 2000);
    }

    private void approve() {
        Logger.d("Payment approved");
        shoppingCart.invalidate();
        clearCodes();
        notifyStateChanged(State.PAYMENT_APPROVED);
    }

    public void approveOfflineMethod() {
        if (paymentMethod != null && paymentMethod.isOfflineMethod()) {
            approve();
        }
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

    public int getPriceToPay() {
        return priceToPay;
    }

    public List<Product> getInvalidProducts() {
        return invalidProducts;
    }

    /**
     * Gets all available payment methods, callable after {@link Checkout.State#REQUEST_PAYMENT_METHOD}.
     */
    public PaymentMethod[] getAvailablePaymentMethods() {
        if (signedCheckoutInfo != null) {
            return signedCheckoutInfo.getAvailablePaymentMethods(clientAcceptedPaymentMethods);
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
