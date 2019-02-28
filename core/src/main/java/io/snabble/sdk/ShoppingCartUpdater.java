package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Arrays;
import java.util.List;

import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;

class ShoppingCartUpdater {
    private Project project;
    private ShoppingCart cart;
    private CheckoutApi checkoutApi;
    private Handler handler;
    private Object lock = new Object();

    ShoppingCartUpdater(Project project, ShoppingCart shoppingCart) {
        this.project = project;
        this.cart = shoppingCart;
        this.checkoutApi = new CheckoutApi(project);
        this.handler = new Handler(Looper.getMainLooper());
    }

    private Runnable updatePriceRunnable = new Runnable() {
        @Override
        public void run() {
            update();
        }
    };

    public void update() {
        checkoutApi.createCheckoutInfo(project.getCheckedInShop(), cart.toBackendCart(), null, new CheckoutApi.CheckoutInfoResult() {
            @Override
            public void success(CheckoutApi.SignedCheckoutInfo signedCheckoutInfo, int onlinePrice, PaymentMethod[] availablePaymentMethods) {
                // TODO thread safety
                synchronized (lock) {
                    try {
                        CheckoutApi.CheckoutInfo checkoutInfo = GsonHolder.get().fromJson(signedCheckoutInfo.checkoutInfo, CheckoutApi.CheckoutInfo.class);

                        // TODO remove missing items ?

                        checkoutInfo.lineItems = Arrays.copyOf(checkoutInfo.lineItems, checkoutInfo.lineItems.length + 1);
                        CheckoutApi.LineItem debugItem = new CheckoutApi.LineItem();
                        debugItem.amount = 1;
                        debugItem.name = "Snabble Rabatt";
                        debugItem.price = -100;
                        debugItem.totalPrice = -100;

                        checkoutInfo.lineItems[checkoutInfo.lineItems.length - 1] = debugItem;

                        for (CheckoutApi.LineItem lineItem : checkoutInfo.lineItems) {
                            ShoppingCart.Item item = cart.getByItemId(lineItem.cartItemID);

                            if (item != null) {
                                item.setLineItem(lineItem);
                            } else {
                                cart.insert(cart.newItem(lineItem), cart.size());
                            }
                        }
                    } catch (Exception e) {
                        Logger.e("Could not dispatchUpdate price: %s", e.getMessage());
                    }
                }

                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void noShop() {
                // ignore
                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void invalidProducts(List<Product> products) {
                // ignore
                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void error() {
                // ignore
                cart.notifyPriceUpdate(cart);
            }
        });
    }

    public void dispatchUpdate() {
        handler.removeCallbacksAndMessages(this);
        handler.postAtTime(updatePriceRunnable, this, SystemClock.uptimeMillis() + 2000);
    }
}
