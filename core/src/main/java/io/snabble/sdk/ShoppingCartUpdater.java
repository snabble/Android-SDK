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
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;

class ShoppingCartUpdater {
    private static final int DEBOUNCE_DELAY_MS = 1000;

    private Project project;
    private ShoppingCart cart;
    private CheckoutApi checkoutApi;
    private Handler handler;

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
        Logger.d("Updating prices...");

        if (cart.size() == 0) {
            return;
        }

        final int modCount = cart.getModCount();
        handler.post(new Runnable() {
            @Override
            public void run() {
                checkoutApi.createCheckoutInfo(cart.toBackendCart(), null, new CheckoutApi.CheckoutInfoResult() {
                    @Override
                    public void success(final CheckoutApi.SignedCheckoutInfo signedCheckoutInfo, int onlinePrice, CheckoutApi.PaymentMethodInfo[] availablePaymentMethods) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                // ignore when cart was modified mid request
                                if (cart.getModCount() != modCount) {
                                    unknownError();
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
                                                CountDownLatch countDownLatch = new CountDownLatch(1);
                                                boolean[] error = new boolean[1];

                                                project.getProductDatabase().findBySkuOnline(lineItem.sku, new OnProductAvailableListener() {
                                                    @Override
                                                    public void onProductAvailable(Product product, boolean wasOnline) {
                                                        ScannedCode scannedCode = ScannedCode.parseDefault(project, lineItem.scannedCode);
                                                        if (scannedCode == null) {
                                                            error[0] = true;
                                                            countDownLatch.countDown();
                                                            return;
                                                        }

                                                        item.replace(product, scannedCode, lineItem.amount);
                                                        countDownLatch.countDown();
                                                    }

                                                    @Override
                                                    public void onProductNotFound() {
                                                        error[0] = true;
                                                        countDownLatch.countDown();
                                                    }

                                                    @Override
                                                    public void onError() {
                                                        error[0] = true;
                                                        countDownLatch.countDown();
                                                    }
                                                });

                                                countDownLatch.await();

                                                if (error[0]) {
                                                    unknownError();
                                                    return;
                                                }
                                            }

                                            item.setLineItem(lineItem);
                                        } else {
                                            if (lineItem.type == CheckoutApi.LineItemType.DISCOUNT) {
                                                discounts += lineItem.price;
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

                                cart.checkLimits();
                                cart.notifyPriceUpdate(cart);
                            }
                        });
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
                    public void noAvailablePaymentMethod() {
                        cart.notifyPriceUpdate(cart);
                    }

                    @Override
                    public void unknownError() {
                        cart.notifyPriceUpdate(cart);
                    }

                    @Override
                    public void connectionError() {
                        cart.notifyPriceUpdate(cart);
                    }
                });
            }
        });
    }

    public void dispatchUpdate() {
        handler.removeCallbacksAndMessages(this);
        handler.postAtTime(updatePriceRunnable, this, SystemClock.uptimeMillis() + DEBOUNCE_DELAY_MS);
    }
}
