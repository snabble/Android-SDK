package io.snabble.sdk;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;

import io.snabble.sdk.codes.ScannedCode;
import io.snabble.sdk.utils.DateUtils;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.Utils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Class for dispatching events to the snabble Backend
 */
public class Events {
    private final Project project;
    private final ShoppingCart shoppingCart;
    private String cartId;
    private Shop shop;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasSentSessionStart = false;

    @SuppressLint("SimpleDateFormat")
    Events(Project project, ShoppingCart shoppingCart) {
        this.project = project;
        this.shoppingCart = shoppingCart;

        project.getShoppingCart().addListener(new ShoppingCart.SimpleShoppingCartListener() {

            @Override
            public void onChanged(ShoppingCart cart) {
                updateShop(Snabble.getInstance().getCheckedInShop());

                if (shop != null) {
                    if (!hasSentSessionStart) {
                        PayloadSessionStart payloadSessionStart = new PayloadSessionStart();
                        payloadSessionStart.session = cartId;
                        post(payloadSessionStart, false);
                    }
                    post(Events.this.project.getShoppingCart().toBackendCart(), true);
                }
            }

            @Override
            public void onCleared(ShoppingCart cart) {
                final boolean isSameCartWithNewId = shoppingCart == cart && !cart.getId().equals(cartId);
                if (isSameCartWithNewId) {
                    PayloadSessionEnd payloadSessionEnd = new PayloadSessionEnd();
                    payloadSessionEnd.session = cartId;
                    post(payloadSessionEnd, false);
                    cartId = cart.getId();
                    hasSentSessionStart = false;
                }
            }

            @Override
            public void onProductsUpdated(ShoppingCart list) {
                // Override because it shouldn't trigger onChanged(Cart)
            }

            @Override
            public void onPricesUpdated(ShoppingCart list) {
                // Override because it shouldn't trigger onChanged(Cart)
            }

            @Override
            public void onTaxationChanged(ShoppingCart list, ShoppingCart.Taxation taxation) {
                // Override because it shouldn't trigger onChanged(Cart)
            }

            @Override
            public void onCartDataChanged(ShoppingCart list) {
                // Override because it shouldn't trigger onChanged(Cart)
            }
        });
    }

    /**
     * Updates the current shop which will be associated with future events
     */
    public void updateShop(Shop newShop) {
        if (newShop != null) {
            cartId = shoppingCart.getId();
            shop = newShop;
        } else {
            shop = null;
            cartId = null;
            hasSentSessionStart = false;
        }
    }

    /**
     * Pack a error log message into a event and dispatch it to the backend
     */
    public void logError(String format, Object... args) {
        if (Utils.isDebugBuild(Snabble.getInstance().getApplication())) {
            return; // do not log errors in debug builds
        }

        PayloadError error = new PayloadError();

        try {
            error.message = String.format(format, args);
        } catch (IllegalFormatException e) {
            Logger.e("Could not post event error: invalid format");
        }

        error.session = cartId;

        post(error, false);
    }

    /**
     * Pack a log message into a event and dispatch it to the backend
     */
    public void log(String format, Object... args) {
        PayloadLog log = new PayloadLog();

        try {
            log.message = String.format(format, args);
        } catch (IllegalFormatException e) {
            Logger.e("Could not post event error: invalid format");
        }

        log.session = cartId;

        post(log, false);
    }

    /**
     * Pack a analytics event and dispatch it to the backend
     */
    public void analytics(String key, String value, String comment) {
        PayloadAnalytics analytics = new PayloadAnalytics();

        analytics.key = key;
        analytics.value = value;
        analytics.comment = comment;
        analytics.session = cartId;

        post(analytics, false);
    }

    /**
     * Dispatch a product not found event
     */
    public void productNotFound(List<ScannedCode> scannedCodes) {
        try {
            PayloadProductNotFound payload = new PayloadProductNotFound();

            if (scannedCodes != null && scannedCodes.size() > 0) {
                payload.scannedCode = scannedCodes.get(0).getCode();
                payload.matched = new HashMap<>();

                for (ScannedCode scannedCode : scannedCodes) {
                    payload.matched.put(scannedCode.getTemplateName(), scannedCode.getLookupCode());
                }
            }

            post(payload, false);
        } catch (Exception e) {
            Logger.e("Could not post event productNotFound: " + e);
        }
    }

    private <T extends Payload> void post(final T payload, boolean debounce) {
        String url = project.getEventsUrl();
        if (url == null) {
            Logger.e("Could not post event: no events url");
            return;
        }

        Event event = new Event();
        event.type = payload.getEventType();
        event.appId = Snabble.getInstance().getClientId();
        event.project = project.getId();
        if (shop != null) {
            event.shopId = shop.getId();
        }
        event.timestamp = DateUtils.toRFC3339(new Date());
        event.payload = GsonHolder.get().toJsonTree(payload);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                GsonHolder.get().toJson(event));

        final Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        if (debounce) {
            handler.removeCallbacksAndMessages(event.type);
            handler.postAtTime(() -> send(request, payload), event.type, SystemClock.uptimeMillis() + 2000);
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
            public void onResponse(@NonNull Call call, Response response) {
                if (response.isSuccessful()) {
                    Logger.d("Successfully posted event: " + payload.getEventType());
                } else {
                    Logger.e("Failed to post event: " + payload.getEventType() + ", code " + response.code());
                }

                response.close();
            }

            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                Logger.e("Could not post event: " + e.toString());

                if (payload.getEventType() == EventType.SESSION_START) {
                    hasSentSessionStart = false;
                }
            }
        });
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public enum EventType {
        @SerializedName("sessionStart")
        SESSION_START,
        @SerializedName("sessionEnd")
        SESSION_END,
        @SerializedName("cart")
        CART,
        @SerializedName("error")
        ERROR,
        @SerializedName("log")
        LOG,
        @SerializedName("analytics")
        ANALYTICS,
        @SerializedName("productNotFound")
        PRODUCT_NOT_FOUND
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public interface Payload {
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

    private static class PayloadProductNotFound implements Payload {
        public String scannedCode;
        public Map<String, String> matched;

        @Override
        public EventType getEventType() {
            return EventType.PRODUCT_NOT_FOUND;
        }
    }

    private static class PayloadAnalytics implements Payload {
        public String key;
        public String value;
        public String comment;
        public String session;

        @Override
        public EventType getEventType() {
            return EventType.ANALYTICS;
        }
    }

    private static class PayloadLog implements Payload {
        public String message;
        public String session;

        @Override
        public EventType getEventType() {
            return EventType.LOG;
        }
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

    private static Project getUsableProject(String projectId) {
        // since we have no error logging without a project, we try to find the project by id
        // and if no project is found we just use the first project to at least log it to something
        Project project = Snabble.getInstance().getProjectById(projectId);
        if (project == null) {
            List<Project> projects = Snabble.getInstance().getProjects();
            if (projects != null && projects.size() > 0) {
                project = projects.get(0);
            }
        }

        return project;
    }

    /**
     * Log a error event to the project with the matching id
     */
    public static void logErrorEvent(@Nullable String projectId, String format, Object... args) {
        Project project = getUsableProject(projectId);
        if (project != null) {
            if (format == null) {
                project.logErrorEvent("Broken log message");
            } else {
                project.logErrorEvent(format, args);
            }
        }
    }
}