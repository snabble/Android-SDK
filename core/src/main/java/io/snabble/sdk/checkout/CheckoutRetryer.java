package io.snabble.sdk.checkout;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.reflect.TypeToken;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Product;
import io.snabble.sdk.Project;
import io.snabble.sdk.ShoppingCart;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;

class CheckoutRetryer {
    private class SavedCart {
        ShoppingCart.BackendCart backendCart;
        Date finalizedAt;
        int failureCount;

        SavedCart(ShoppingCart.BackendCart backendCart, Date finalizedAt) {
            this.backendCart = backendCart;
            this.finalizedAt = finalizedAt;
        }
    }

    private final PaymentMethod fallbackPaymentMethod;
    private final SharedPreferences sharedPreferences;
    private final Project project;
    private final CopyOnWriteArrayList<SavedCart> savedCarts;
    private CountDownLatch countDownLatch;

    CheckoutRetryer(Project project, PaymentMethod fallbackPaymentMethod) {
        Context context = Snabble.getInstance().getApplication();
        this.sharedPreferences = context.getSharedPreferences("snabble_saved_checkouts_" + project.getId(), Context.MODE_PRIVATE);
        this.project = project;
        this.fallbackPaymentMethod = fallbackPaymentMethod;

        String json = sharedPreferences.getString("saved_carts", null);
        if (json != null) {
            TypeToken typeToken = new TypeToken<CopyOnWriteArrayList<SavedCart>>() {};
            savedCarts = GsonHolder.get().fromJson(json, typeToken.getType());
        } else {
            savedCarts = new CopyOnWriteArrayList<>();
        }

        processPendingCheckouts();
    }

    public void add(final ShoppingCart.BackendCart backendCart) {
        Dispatch.mainThread(() -> {
            savedCarts.add(new SavedCart(backendCart, new Date()));
            save();
        });
    }

    private void save() {
        Dispatch.mainThread(() -> {
            String json = GsonHolder.get().toJson(savedCarts);

            sharedPreferences.edit()
                    .putString("saved_carts", json)
                    .apply();
        });
    }

    public void processPendingCheckouts() {
        Context context = Snabble.getInstance().getApplication();
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return;
        }

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            return;
        }

        Dispatch.mainThread(() -> {
            if (countDownLatch != null && countDownLatch.getCount() > 0) {
                return;
            }

            countDownLatch = new CountDownLatch(savedCarts.size());

            for (final SavedCart savedCart : savedCarts) {
                if (savedCart.failureCount >= 3) {
                    removeSavedCart(savedCart);
                }

                final DefaultCheckoutApi checkoutApi = new DefaultCheckoutApi(project, project.getShoppingCart());
                checkoutApi.createCheckoutInfo(savedCart.backendCart, null, new CheckoutInfoResult() {
                    @Override
                    public void success(SignedCheckoutInfo signedCheckoutInfo, int onlinePrice, List<PaymentMethodInfo> availablePaymentMethods) {
                        checkoutApi.createPaymentProcess(UUID.randomUUID().toString(), signedCheckoutInfo, fallbackPaymentMethod, null,
                                true, savedCart.finalizedAt, new PaymentProcessResult() {
                            @Override
                            public void success(CheckoutProcessResponse checkoutProcessResponse, String rawResponse) {
                                Logger.d("Successfully resend checkout " + savedCart.backendCart.session);
                                removeSavedCart(savedCart);
                                countDownLatch.countDown();
                            }

                            @Override
                            public void error() {
                                fail();
                            }
                        });
                    }

                    @Override
                    public void noShop() {
                        fail();
                    }

                    @Override
                    public void invalidProducts(List<? extends Product> products) {
                        fail();
                    }

                    @Override
                    public void noAvailablePaymentMethod() {
                        fail();
                    }

                    @Override
                    public void invalidDepositReturnVoucher() {
                        fail();
                    }

                    @Override
                    public void unknownError() {
                        fail();
                    }

                    @Override
                    public void connectionError() {
                        fail();
                    }

                    private void fail() {
                        savedCart.failureCount++;
                        save();
                        countDownLatch.countDown();
                    }
                }, -1);
            }
        });
    }

    private void removeSavedCart(final SavedCart savedCart) {
        Dispatch.mainThread(() -> {
            savedCarts.remove(savedCart);
            save();
        });
    }
}