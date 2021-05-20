package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

                    List<String> skus = getToBeReplacedSkus(signedCheckoutInfo);

                    if (skus.size() > 0) {
                        Dispatch.background(() -> {
                            Map<String, Product> products = getReplacedProducts(skus);
                            if (products == null) {
                                Dispatch.mainThread(this::unknownError);
                            } else {
                                Dispatch.mainThread(() -> commitCartUpdate(modCount, signedCheckoutInfo, products));
                            }
                        });
                    } else {
                        Dispatch.mainThread(() -> commitCartUpdate(modCount, signedCheckoutInfo, null));
                    }
                });
            }

            @Override
            public void noShop() {
                error(true);
            }

            @Override
            public void invalidProducts(List<Product> products) {
                cart.setInvalidProducts(products);
                error(true);
            }

            @Override
            public void noAvailablePaymentMethod() {
               error(true);
            }

            @Override
            public void invalidDepositReturnVoucher() {
                cart.setInvalidDepositReturnVoucher(true);
                error(true);
            }

            @Override
            public void unknownError() {
                error(false);
            }

            @Override
            public void connectionError() {
                error(false);
            }
        }, -1));
    }

    private void error(boolean b) {
        isUpdated = b;
        lastAvailablePaymentMethods = null;
        cart.notifyPriceUpdate(cart);
    }

    private void commitCartUpdate(int modCount, CheckoutApi.SignedCheckoutInfo signedCheckoutInfo, Map<String, Product> products) {
        try {
            if (cart.getModCount() != modCount) {
                error(false);
                return;
            }

            cart.invalidateOnlinePrices();

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
                error(false);
                return;
            }

            int discounts = 0;

            for (CheckoutApi.LineItem lineItem : checkoutInfo.lineItems) {
                ShoppingCart.Item item = cart.getByItemId(lineItem.id);

                if (item != null) {
                    if (!item.getProduct().getSku().equals(lineItem.sku)) {
                        if (products == null) {
                            error(false);
                            return;
                        }

                        Product product = products.get(lineItem.sku);

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
                        boolean add = true;
                        for (ManualCoupon manualCoupon : project.getManualCoupons()) {
                            if (manualCoupon.getId().equals(lineItem.couponId)) {
                                add = false;
                                break;
                            }
                        }

                        if (add) {
                            cart.insert(cart.newItem(lineItem), cart.size(), false);
                        }
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

            if (project.isDisplayingNetPrice()) {
                cart.setOnlineTotalPrice(checkoutInfo.price.netPrice);
            } else {
                cart.setOnlineTotalPrice(checkoutInfo.price.price);
            }

            Logger.d("Successfully updated prices");
        } catch (Exception e) {
            Logger.e("Could not update price: %s", e.getMessage());
            error(false);
            return;
        }

        lastAvailablePaymentMethods = signedCheckoutInfo.getAvailablePaymentMethods(
                project.getCheckout().getClientAcceptedPaymentMethods());

        isUpdated = true;
        cart.setInvalidProducts(null);
        cart.checkLimits();
        cart.notifyPriceUpdate(cart);
    }

    private CheckoutApi.LineItem getLineItem(CheckoutApi.CheckoutInfo checkoutInfo, String id) {
        for (CheckoutApi.LineItem lineItem : checkoutInfo.lineItems) {
            if (lineItem.id.equals(id)) {
                return lineItem;
            }
        }

        return null;
    }

    private List<String> getToBeReplacedSkus(CheckoutApi.SignedCheckoutInfo signedCheckoutInfo) {
        CheckoutApi.CheckoutInfo checkoutInfo = GsonHolder.get().fromJson(signedCheckoutInfo.checkoutInfo, CheckoutApi.CheckoutInfo.class);

        List<String> skus = new ArrayList<>();
        for (CheckoutApi.LineItem lineItem : checkoutInfo.lineItems) {
            ShoppingCart.Item item = cart.getByItemId(lineItem.id);

            if (item != null) {
                if (!item.getProduct().getSku().equals(lineItem.sku)) {
                    skus.add(lineItem.sku);
                }
            }
        }

        return skus;
    }

    private Map<String, Product> getReplacedProducts(List<String> skus) {
        Map<String, Product> products = new HashMap<>();

        for (String sku : skus) {
            Product product = findProductBlocking(sku);
            if (product == null) {
                return null;
            } else {
                products.put(sku, product);
            }
        }

        return products;
    }

    private Product findProductBlocking(String sku) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final Product[] product = {null};

        project.getProductDatabase().findBySkuOnline(sku, new OnProductAvailableListener() {
            @Override
            public void onProductAvailable(Product p, boolean wasOnline) {
                product[0] = p;
                countDownLatch.countDown();
            }

            @Override
            public void onProductNotFound() {
                countDownLatch.countDown();
            }

            @Override
            public void onError() {
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return product[0];
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void dispatchUpdate() {
        handler.removeCallbacksAndMessages(this);
        handler.postAtTime(updatePriceRunnable, this, SystemClock.uptimeMillis() + DEBOUNCE_DELAY_MS);
    }
}
