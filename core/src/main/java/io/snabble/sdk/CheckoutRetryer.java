package io.snabble.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.utils.GsonHolder;

public class CheckoutRetryer {
    private Handler handler;
    private PaymentMethod fallbackPaymentMethod;
    private SharedPreferences sharedPreferences;
    private CheckoutApi checkoutApi;
    private CopyOnWriteArrayList<ShoppingCart.BackendCart> savedCarts;
    private CountDownLatch countDownLatch;

    public CheckoutRetryer(Project project, PaymentMethod fallbackPaymentMethod) {
        Context context = Snabble.getInstance().getApplication();
        this.sharedPreferences = context.getSharedPreferences("snabble_saved_checkouts_" + project.getId(), Context.MODE_PRIVATE);
        this.checkoutApi = new CheckoutApi(project);
        this.fallbackPaymentMethod = fallbackPaymentMethod;
        this.handler = new Handler(Looper.getMainLooper());

        String json = sharedPreferences.getString("saved_carts", null);
        if (json != null) {
            TypeToken typeToken = new TypeToken<CopyOnWriteArrayList<ShoppingCart.BackendCart>>() {};
            savedCarts = GsonHolder.get().fromJson(json, typeToken.getType());
        } else {
            savedCarts = new CopyOnWriteArrayList<>();
        }

        processSavedCheckouts();
    }

    public void add(ShoppingCart.BackendCart backendCart) {
        savedCarts.add(backendCart);
        save();
    }

    private void save() {
        String json = GsonHolder.get().toJson(savedCarts);

        sharedPreferences.edit()
                .putString("saved_carts", json)
                .apply();
    }

    public void processSavedCheckouts() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (countDownLatch != null && countDownLatch.getCount() > 0) {
                    return;
                }

                countDownLatch = new CountDownLatch(savedCarts.size());

                for (final ShoppingCart.BackendCart backendCart : savedCarts) {
                    checkoutApi.createCheckoutInfo(backendCart, null, new CheckoutApi.CheckoutInfoResult() {
                        @Override
                        public void success(CheckoutApi.SignedCheckoutInfo signedCheckoutInfo, int onlinePrice, PaymentMethod[] availablePaymentMethods) {
                            checkoutApi.createPaymentProcess(signedCheckoutInfo, fallbackPaymentMethod, null, new CheckoutApi.PaymentProcessResult() {
                                @Override
                                public void success(CheckoutApi.CheckoutProcessResponse checkoutProcessResponse) {
                                    removeSavedCart(backendCart);
                                    countDownLatch.countDown();
                                }

                                @Override
                                public void aborted() {
                                    countDownLatch.countDown();
                                }

                                @Override
                                public void error() {
                                    countDownLatch.countDown();
                                }
                            });
                        }

                        @Override
                        public void noShop() {
                            countDownLatch.countDown();
                        }

                        @Override
                        public void invalidProducts(List<Product> products) {
                            countDownLatch.countDown();
                        }

                        @Override
                        public void noAvailablePaymentMethod() {
                            countDownLatch.countDown();
                        }

                        @Override
                        public void error() {
                            countDownLatch.countDown();
                        }
                    });
                }
            }
        });
    }

    private void removeSavedCart(final ShoppingCart.BackendCart backendCart) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                savedCarts.remove(backendCart);
                save();
            }
        });
    }
}
