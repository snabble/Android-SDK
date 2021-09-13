package io.snabble.sdk;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

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

public class Events {
    private Project project;
    private String cartId;
    private Shop shop;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasSentSessionStart = false;

    @SuppressLint("SimpleDateFormat")
    Events(Project project) {
        this.project = project;

        project.getShoppingCart().addListener(new ShoppingCart.SimpleShoppingCartListener() {
            @Override
            public void onChanged(ShoppingCart cart) {
                if (cartId != null && !cart.getId().equals(cartId)) {
                    PayloadSessionEnd payloadSessionEnd = new PayloadSessionEnd();
                    payloadSessionEnd.session = cartId;
                    post(payloadSessionEnd, false);
                    cartId = cart.getId();
                    hasSentSessionStart = false;
                    return;
                }

                if (!hasSentSessionStart) {
                    PayloadSessionStart payloadSessionStart = new PayloadSessionStart();
                    payloadSessionStart.session = cartId;
                    post(payloadSessionStart, false);
                }

                post(Events.this.project.getShoppingCart().toBackendCart(), true);
            }
        });
    }

    public void updateShop(Shop newShop) {
        if (newShop != null) {
            ShoppingCart cart = project.getShoppingCart();
            cartId = cart.getId();
            shop = newShop;
        } else {
            shop = null;
            hasSentSessionStart = false;
        }
    }

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

    public void analytics(String key, String value, String comment) {
        PayloadAnalytics analytics = new PayloadAnalytics();

        analytics.key = key;
        analytics.value = value;
        analytics.comment = comment;
        analytics.session = cartId;

        post(analytics, false);
    }

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

                if (payload.getEventType() == EventType.SESSION_START) {
                    hasSentSessionStart = false;
                }
            }
        });
    }

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

    public interface Payload {
        EventType getEventType();
    }

    public static class Event {
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

    public static void logErrorEvent(String projectId, String format, Object... args) {
        Project project = getUsableProject(projectId);
        if (project != null) {
            project.logErrorEvent(format, args);
        }
    }

    public static void logWarningEvent(String projectId, String format, Object... args) {
        Project project = getUsableProject(projectId);
        if (project != null) {
            project.logErrorEvent(format, args);
        }
    }
}
