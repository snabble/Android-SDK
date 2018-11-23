package io.snabble.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class Events {
    private Project project;
    private String cartId;
    private Shop shop;

    private Handler handler = new Handler(Looper.getMainLooper());
    private SimpleDateFormat simpleDateFormat;
    private boolean isResumed = false;
    private boolean hasSentSessionStart = false;

    @SuppressLint("SimpleDateFormat")
    public Events(Project project) {
        this.project = project;

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        project.getShoppingCart().addListener(new ShoppingCart.SimpleShoppingCartListener() {
            @Override
            public void onChanged(ShoppingCart list) {
                if (cartId != null && !list.getId().equals(cartId)) {
                    PayloadSessionEnd payloadSessionEnd = new PayloadSessionEnd();
                    payloadSessionEnd.session = cartId;
                    post(payloadSessionEnd, false);
                }

                post(getPayloadCart(), true);
            }
        });

        Snabble.getInstance().getApplication().registerActivityLifecycleCallbacks(new SimpleActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                isResumed = true;

                if (!hasSentSessionStart) {
                    updateShop(shop);
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                isResumed = false;
            }
        });
    }

    public void updateShop(Shop newShop) {
        if (newShop != null) {
            cartId = project.getShoppingCart().getId();
            shop = newShop;

            PayloadSessionStart payloadSessionStart = new PayloadSessionStart();
            payloadSessionStart.session = cartId;
            post(payloadSessionStart, false);
            post(getPayloadCart(), true);
        } else {
            PayloadSessionEnd payloadSessionEnd = new PayloadSessionEnd();
            payloadSessionEnd.session = cartId;
            post(payloadSessionEnd, false);

            shop = null;
        }
    }

    public void logError(String format, Object... args) {
        PayloadError error = new PayloadError();
        error.message = String.format(format, args);
        error.session = cartId;

        post(error, false);
    }

    private <T extends Payload> void post(final T payload, boolean debounce) {
        if (!isResumed && payload.getEventType() != EventType.ERROR) {
            Logger.d("Could not send event, app is not active: " + payload.getEventType());
            return;
        }

        String url = project.getEventsUrl();
        if (url == null) {
            Logger.e("Could not post event: no events url");
            return;
        }

        if (shop == null) {
            return;
        }

        Event event = new Event();
        event.type = payload.getEventType();
        event.appId = Snabble.getInstance().getClientId();
        event.project = project.getId();
        event.shopId = shop.getId();
        event.timestamp = simpleDateFormat.format(new Date());
        event.payload = GsonHolder.get().toJsonTree(payload);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                GsonHolder.get().toJson(event));

        final Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        if (debounce) {
            handler.removeCallbacksAndMessages(event.type);
            handler.postAtTime(new Runnable() {
                @Override
                public void run() {
                    send(request, payload);
                }
            }, event.type, SystemClock.uptimeMillis() + 2000);
        } else {
            send(request, payload);
        }

        if (event.type == EventType.SESSION_START) {
            hasSentSessionStart = true;
        }
    }

    private <T extends Payload> void send(Request request, final T payload) {
        OkHttpClient okHttpClient = project.getOkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    Logger.d("Successfully posted event: " + payload.getEventType());
                } else {
                    Logger.e("Failed to post event: " + payload.getEventType() + ", code " + response.code());
                }

                response.close();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Logger.e("Could not post event: " + e.toString());
            }
        });
    }

    private PayloadCart getPayloadCart() {
        ShoppingCart shoppingCart = project.getShoppingCart();

        PayloadCart payloadCart = new PayloadCart();
        payloadCart.session = shoppingCart.getId();
        payloadCart.shopId = "unknown";

        String loyaltyCardId = project.getLoyaltyCardId();
        if (loyaltyCardId != null) {
            payloadCart.customer = new PayloadCartCustomer();
            payloadCart.customer.loyaltyCard = loyaltyCardId;
        }

        Shop shop = project.getCheckedInShop();
        if (shop != null) {
            String id = shop.getId();
            if (id != null) {
                payloadCart.shopId = id;
            }
        }

        payloadCart.items = new PayloadCartItem[shoppingCart.size()];

        for (int i = 0; i < shoppingCart.size(); i++) {
            Product product = shoppingCart.getProduct(i);
            int quantity = shoppingCart.getQuantity(i);

            payloadCart.items[i] = new PayloadCartItem();
            payloadCart.items[i].sku = String.valueOf(product.getSku());
            payloadCart.items[i].scannedCode = shoppingCart.getScannedCode(i);
            payloadCart.items[i].amount = quantity;
            payloadCart.items[i].units = shoppingCart.getEmbeddedUnits(i);
            payloadCart.items[i].weight = shoppingCart.getEmbeddedWeight(i);
            payloadCart.items[i].price = shoppingCart.getEmbeddedPrice(i);

            if (product.getType() == Product.Type.UserWeighed) {
                payloadCart.items[i].amount = 1;
                payloadCart.items[i].weight = quantity;
            }
        }

        return payloadCart;
    }

    public String getPayloadCartJson() {
        return GsonHolder.get().toJson(getPayloadCart());
    }

    private enum EventType {
        @SerializedName("sessionStart")
        SESSION_START,
        @SerializedName("sessionEnd")
        SESSION_END,
        @SerializedName("cart")
        CART,
        @SerializedName("error")
        ERROR
    }

    private interface Payload {
        EventType getEventType();
    }

    private static class Event {
        public EventType type;
        public String appId;
        @SerializedName("shopID")
        public String shopId;
        public String project;
        public String timestamp;
        public JsonElement payload;
    }

    private static class PayloadError implements Payload {
        public String message;
        public String session;

        @Override
        public EventType getEventType() {
            return EventType.ERROR;
        }
    }

    private static class PayloadSessionStart implements Payload {
        public String session;

        @Override
        public EventType getEventType() {
            return EventType.SESSION_START;
        }
    }

    private static class PayloadSessionEnd implements Payload {
        public String session;

        @Override
        public EventType getEventType() {
            return EventType.SESSION_END;
        }
    }

    private static class PayloadCart implements Payload {
        private String session;
        @SerializedName("shopID")
        private String shopId;
        private PayloadCartCustomer customer;
        private PayloadCartItem[] items;

        @Override
        public EventType getEventType() {
            return EventType.CART;
        }
    }

    private static class PayloadCartCustomer {
        private String loyaltyCard;
    }

    private static class PayloadCartItem {
        private String sku;
        private String scannedCode;
        private int amount;
        private Integer price;
        private Integer weight;
        private Integer units;
    }
}
