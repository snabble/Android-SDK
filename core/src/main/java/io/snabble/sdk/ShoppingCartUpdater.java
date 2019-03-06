package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;

class ShoppingCartUpdater {
    private static final int DEBOUNCE_DELAY_MS = 1000;

    private Project project;
    private ShoppingCart cart;
    private CheckoutApi checkoutApi;
    private Handler handler;
    private final Object lock = new Object();

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
        final int modCount = cart.getModCount();
        checkoutApi.createCheckoutInfo(project.getCheckedInShop(), cart.toBackendCart(), null, new CheckoutApi.CheckoutInfoResult() {
            @Override
            public void success(CheckoutApi.SignedCheckoutInfo signedCheckoutInfo, int onlinePrice, PaymentMethod[] availablePaymentMethods) {
                synchronized (lock) {
                    // ignore when cart was modified mid request
                    if (cart.getModCount() != modCount) {
                        error();
                        return;
                    }

                    cart.removeOnlinePrices();

                    try {
                        CheckoutApi.CheckoutInfo checkoutInfo = GsonHolder.get().fromJson(signedCheckoutInfo.checkoutInfo, CheckoutApi.CheckoutInfo.class);

                        Set<String> depositSkus = new HashSet<>();
                        Set<String> requiredSkus = new HashSet<>();

                        for (int i=0; i<cart.size(); i++) {
                            ShoppingCart.Item item = cart.get(i);
                            requiredSkus.add(item.getProduct().getSku());

                            Product depositProduct = item.getProduct().getDepositProduct();
                            if (depositProduct != null) {
                                depositSkus.add(depositProduct.getSku());
                            }
                        }

                        for (CheckoutApi.LineItem lineItem : checkoutInfo.lineItems) {
                            requiredSkus.remove(lineItem.sku);
                        }

                        // error out when items are missing
                        if (requiredSkus.size() > 0) {
                            Logger.e("Missing products in price update: " + requiredSkus.toString());
                            error();
                            return;
                        }

                        for (CheckoutApi.LineItem lineItem : checkoutInfo.lineItems) {
                            // exclude deposit line items
                            if (depositSkus.contains(lineItem.sku)) {
                                continue;
                            }

                            ShoppingCart.Item item = cart.getByItemId(lineItem.cartItemID);

                            if (item != null) {
                                item.setLineItem(lineItem);
                            } else {
                                cart.insert(cart.newItem(lineItem), cart.size());
                            }
                        }

                        cart.setOnlineTotalPrice(checkoutInfo.price.price);
                    } catch (Exception e) {
                        Logger.e("Could not update price: %s", e.getMessage());
                        error();
                        return;
                    }
                }

                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void noShop() {
                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void invalidProducts(List<Product> products) {
                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void error() {
                cart.notifyPriceUpdate(cart);
            }
        });
    }

    public void dispatchUpdate() {
        handler.removeCallbacksAndMessages(this);
        handler.postAtTime(updatePriceRunnable, this, SystemClock.uptimeMillis() + DEBOUNCE_DELAY_MS);
    }
}
