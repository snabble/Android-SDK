package io.snabble.sdk;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.checkout.CheckoutInfo;
import io.snabble.sdk.checkout.CheckoutInfoResult;
import io.snabble.sdk.checkout.DefaultCheckoutApi;
import io.snabble.sdk.checkout.LineItem;
import io.snabble.sdk.checkout.LineItemType;
import io.snabble.sdk.checkout.PaymentMethodInfo;
import io.snabble.sdk.checkout.SignedCheckoutInfo;
import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;

class ShoppingCartUpdater {
    private static final int DEBOUNCE_DELAY_MS = 1000;

    private final Project project;
    private final ShoppingCart cart;
    private final DefaultCheckoutApi checkoutApi;
    private final Handler handler;
    private List<PaymentMethodInfo> lastAvailablePaymentMethods;
    private boolean isUpdated;
    private int successfulModCount = -1;

    ShoppingCartUpdater(Project project, ShoppingCart shoppingCart) {
        this.project = project;
        this.cart = shoppingCart;
        this.checkoutApi = new DefaultCheckoutApi(project, shoppingCart);
        this.handler = new Handler(Looper.getMainLooper());
    }

    private final Runnable updatePriceRunnable = () -> update(false);

    public List<PaymentMethodInfo> getLastAvailablePaymentMethods() {
        return lastAvailablePaymentMethods;
    }

    public void update(boolean force) {
        Logger.d("Updating prices...");

        if (cart.size() == 0) {
            lastAvailablePaymentMethods = null;
            cart.notifyPriceUpdate(cart);
            return;
        }

        final int modCount = cart.getModCount();
        if (modCount == successfulModCount && !force) {
            return;
        }

        Dispatch.mainThread(() -> checkoutApi.createCheckoutInfo(cart.toBackendCart(), new CheckoutInfoResult() {
            @Override
            public void onSuccess(final SignedCheckoutInfo signedCheckoutInfo, int onlinePrice, List<PaymentMethodInfo> availablePaymentMethods) {
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
                                Dispatch.mainThread(this::onUnknownError);
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
            public void onNoShopFound() {
                error(true);
            }

            @Override
            public void onInvalidProducts(@NotNull List<? extends Product> products) {
                cart.setInvalidProducts((List<Product>) products);
                error(true);
            }

            @Override
            public void onNoAvailablePaymentMethodFound() {
               error(true);
            }

            @Override
            public void onInvalidDepositReturnVoucher() {
                cart.setInvalidDepositReturnVoucher(true);
                error(true);
            }

            @Override
            public void onUnknownError() {
                error(false);
            }

            @Override
            public void onConnectionError() {
                error(false);
            }
        }, -1));
    }

    private void error(boolean b) {
        isUpdated = b;
        lastAvailablePaymentMethods = null;
        cart.notifyPriceUpdate(cart);
    }

    private void commitCartUpdate(int modCount, SignedCheckoutInfo signedCheckoutInfo, Map<String, Product> products) {
        try {
            if (cart.getModCount() != modCount) {
                error(false);
                return;
            }

            cart.invalidateOnlinePrices();

            CheckoutInfo checkoutInfo = GsonHolder.get().fromJson(signedCheckoutInfo.getCheckoutInfo(), CheckoutInfo.class);

            Set<String> referrerIds = new HashSet<>();
            Set<String> requiredIds = new HashSet<>();

            for (int i=0; i<cart.size(); i++) {
                ShoppingCart.Item item = cart.get(i);
                if (item.getType() != ShoppingCart.ItemType.COUPON) {
                    requiredIds.add(item.getId());
                    referrerIds.add(item.getId());
                }
            }

            for (LineItem lineItem : checkoutInfo.getLineItems()) {
                requiredIds.remove(lineItem.getId());
            }

            // error out when items are missing
            if (requiredIds.size() > 0) {
                Logger.e("Missing products in price update: " + requiredIds.toString());
                error(false);
                return;
            }

            int discounts = 0;

            for (LineItem lineItem : checkoutInfo.getLineItems()) {
                ShoppingCart.Item item = cart.getByItemId(lineItem.getId());

                if (item != null) {
                    if (!item.getProduct().getSku().equals(lineItem.getSku())) {
                        if (products == null) {
                            error(false);
                            return;
                        }

                        Product product = products.get(lineItem.getSku());

                        if (product != null) {
                            ScannedCode scannedCode = ScannedCode.parseDefault(project, lineItem.getScannedCode());
                            if (scannedCode != null) {
                                item.replace(product, scannedCode, lineItem.getAmount());
                                item.setLineItem(lineItem);
                            }
                        }
                    } else {
                        item.setLineItem(lineItem);
                    }
                } else {
                    if (lineItem.getType() == LineItemType.DISCOUNT) {
                        discounts += lineItem.getTotalPrice();
                    } else {
                        boolean add = true;
                        for (Coupon coupon : project.getCoupons()) {
                            if (coupon.getId().equals(lineItem.getCouponId())) {
                                add = false;
                                break;
                            }
                        }

                        if (add) {
                            cart.insert(cart.newItem(lineItem), cart.size(), false);
                        }

                        if (lineItem.getType() == LineItemType.COUPON) {
                            ShoppingCart.Item refersTo = cart.getByItemId(lineItem.getRefersTo());
                            if (refersTo != null) {
                                refersTo.setManualCouponApplied(lineItem.getRedeemed());
                                discounts += refersTo.getModifiedPrice();
                            }
                        }
                    }
                }
            }

            if (discounts != 0) {
                LineItem lineItem = new LineItem();
                lineItem.setType(LineItemType.DISCOUNT);
                lineItem.setAmount(1);
                lineItem.setPrice(discounts);
                lineItem.setTotalPrice(lineItem.getPrice());
                lineItem.setId(UUID.randomUUID().toString());
                cart.insert(cart.newItem(lineItem), cart.size(), false);
            }

            if (project.isDisplayingNetPrice()) {
                cart.setOnlineTotalPrice(checkoutInfo.getPrice().getNetPrice());
            } else {
                cart.setOnlineTotalPrice(checkoutInfo.getPrice().getPrice());
            }

            successfulModCount = modCount;

            Logger.d("Successfully updated prices");
        } catch (Exception e) {
            Logger.e("Could not update price: %s", e.getMessage());
            error(false);
            return;
        }

        lastAvailablePaymentMethods = signedCheckoutInfo.getAvailablePaymentMethods();

        isUpdated = true;
        cart.setInvalidProducts(null);
        cart.checkLimits();
        cart.notifyPriceUpdate(cart);
    }

    private LineItem getLineItem(CheckoutInfo checkoutInfo, String id) {
        for (LineItem lineItem : checkoutInfo.getLineItems()) {
            if (lineItem.getId().equals(id)) {
                return lineItem;
            }
        }

        return null;
    }

    private List<String> getToBeReplacedSkus(SignedCheckoutInfo signedCheckoutInfo) {
        CheckoutInfo checkoutInfo = GsonHolder.get().fromJson(signedCheckoutInfo.getCheckoutInfo(), CheckoutInfo.class);

        List<String> skus = new ArrayList<>();
        for (LineItem lineItem : checkoutInfo.getLineItems()) {
            ShoppingCart.Item item = cart.getByItemId(lineItem.getId());

            if (item != null) {
                if (!item.getProduct().getSku().equals(lineItem.getSku())) {
                    skus.add(lineItem.getSku());
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