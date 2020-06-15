package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;

class ShoppingCartUpdater {
    private static final int DEBOUNCE_DELAY_MS = 1000;

    private Project project;
    private ShoppingCart cart;
    private CheckoutApi checkoutApi;
    private Handler handler;
    private CheckoutApi.PaymentMethodInfo[] lastAvailablePaymentMethods;
    private boolean isUpdated;

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

    public CheckoutApi.PaymentMethodInfo[] getLastAvailablePaymentMethods() {
        return lastAvailablePaymentMethods;
    }

    public void update() {
        Logger.d("Updating prices...");

        if (cart.size() == 0) {
            lastAvailablePaymentMethods = null;
            cart.notifyPriceUpdate(cart);
            return;
        }

        final int modCount = cart.getModCount();
        Dispatch.mainThread(() -> checkoutApi.createCheckoutInfo(cart.toBackendCart(), null, new CheckoutApi.CheckoutInfoResult() {
            @Override
            public void success(final CheckoutApi.SignedCheckoutInfo signedCheckoutInfo, int onlinePrice, CheckoutApi.PaymentMethodInfo[] availablePaymentMethods) {
                Dispatch.mainThread(() -> {
                    // ignore when cart was modified mid request
                    if (cart.getModCount() != modCount) {
                        return;
                    }

                    cart.invalidateOnlinePrices();

                    try {
                        CheckoutApi.CheckoutInfo checkoutInfo = GsonHolder.get().fromJson(signedCheckoutInfo.checkoutInfo, CheckoutApi.CheckoutInfo.class);

                        Set<String> referrerIds = new HashSet<>();
                        Set<String> requiredIds = new HashSet<>();

                        for (int i=0; i<cart.size(); i++) {
                            ShoppingCart.Item item = cart.get(i);
                            requiredIds.add(item.getId());
                            referrerIds.add(item.getId());
                        }

                        for (CheckoutApi.LineItem lineItem : checkoutInfo.lineItems) {
                            requiredIds.remove(lineItem.id);
                        }

                        // error out when items are missing
                        if (requiredIds.size() > 0) {
                            Logger.e("Missing products in price update: " + requiredIds.toString());
                            unknownError();
                            return;
                        }

                        int discounts = 0;

                        for (CheckoutApi.LineItem lineItem : checkoutInfo.lineItems) {
                            ShoppingCart.Item item = cart.getByItemId(lineItem.id);

                            if (item != null) {
                                if (!item.getProduct().getSku().equals(lineItem.sku)) {
                                    Product product = project.getProductDatabase().findBySku(lineItem.sku);

                                    if (product != null) {
                                        ScannedCode scannedCode = ScannedCode.parseDefault(project, lineItem.scannedCode);
                                        if (scannedCode != null) {
                                            item.replace(product, scannedCode, lineItem.amount);
                                            item.setLineItem(lineItem);
                                        }
                                    }
                                } else {
                                    item.setLineItem(lineItem);
                                }
                            } else {
                                if (lineItem.type == CheckoutApi.LineItemType.DISCOUNT) {
                                    discounts += lineItem.totalPrice;
                                } else {
                                    cart.insert(cart.newItem(lineItem), cart.size(), false);
                                }
                            }
                        }

                        if (discounts != 0) {
                            CheckoutApi.LineItem lineItem = new CheckoutApi.LineItem();
                            lineItem.type = CheckoutApi.LineItemType.DISCOUNT;
                            lineItem.amount = 1;
                            lineItem.price = discounts;
                            lineItem.totalPrice = lineItem.price;
                            lineItem.id = UUID.randomUUID().toString();
                            cart.insert(cart.newItem(lineItem), cart.size(), false);
                        }

                        cart.setOnlineTotalPrice(checkoutInfo.price.price);
                        Logger.d("Successfully updated prices");
                    } catch (Exception e) {
                        Logger.e("Could not update price: %s", e.getMessage());
                        unknownError();
                        return;
                    }

                    lastAvailablePaymentMethods = signedCheckoutInfo.getAvailablePaymentMethods(
                            project.getCheckout().getClientAcceptedPaymentMethods());

                    isUpdated = true;
                    cart.setInvalidProducts(null);
                    cart.checkLimits();
                    cart.notifyPriceUpdate(cart);
                });
            }

            @Override
            public void noShop() {
                isUpdated = true;
                lastAvailablePaymentMethods = null;
                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void invalidProducts(List<Product> products) {
                isUpdated = true;
                lastAvailablePaymentMethods = null;
                cart.setInvalidProducts(products);
                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void noAvailablePaymentMethod() {
                isUpdated = true;
                lastAvailablePaymentMethods = null;
                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void unknownError() {
                isUpdated = false;
                lastAvailablePaymentMethods = null;
                cart.notifyPriceUpdate(cart);
            }

            @Override
            public void connectionError() {
                isUpdated = false;
                lastAvailablePaymentMethods = null;
                cart.notifyPriceUpdate(cart);
            }
        }));
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void dispatchUpdate() {
        handler.removeCallbacksAndMessages(this);
        handler.postAtTime(updatePriceRunnable, this, SystemClock.uptimeMillis() + DEBOUNCE_DELAY_MS);
    }
}
