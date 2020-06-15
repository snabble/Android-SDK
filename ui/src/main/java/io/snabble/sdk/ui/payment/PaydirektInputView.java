package io.snabble.sdk.ui.payment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

import io.snabble.sdk.CheckoutApi;
import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.Keyguard;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PaydirektInputView extends FrameLayout {
    private static class Href {
        public String href;
    }

    private static class PaydirektAuthorizationResult {
        public String id;
        public Map<String, Href> links;

        public String getAuthorizationLink() {
            Href href = links.get("self");
            if (href != null && href.href != null) {
                return href.href;
            }
            return null;
        }

        public String getWebLink() {
            Href href = links.get("web");
            if (href != null && href.href != null) {
                return href.href;
            }
            return null;
        }
    }

    private static class PaydirektAuthorizationRequest {
        String id;
        String name;
        String ipAddress;
        String fingerprint;
        String redirectUrlAfterSuccess;
        String redirectUrlAfterCancellation;
        String redirectUrlAfterFailure;
    }

    private enum RedirectUrl {
        SUCCESS("success"),
        CANCELLED("cancelled"),
        FAILURE("failure");

        String path;

        RedirectUrl(String path) {
            this.path = path;
        }

        public String getUrl() {
            return "snabble-paydirekt://" + path;
        }
    }

    private class PaydirektInfo {
        String whateverId;

        public PaydirektInfo(String whateverId) {

        }
    }

    private boolean acceptedKeyguard;
    private WebView webView;
    private OkHttpClient okHttpClient;
    private ProgressBar progressBar;
    private boolean isAttachedToWindow;
    private boolean isActivityResumed;
    private PaydirektInfo pendingPaydirektInfo;
    private PaydirektAuthorizationResult authorizationResult;

    public PaydirektInputView(Context context) {
        super(context);
        inflateView();
    }

    public PaydirektInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public PaydirektInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    @SuppressLint({"InlinedApi", "SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void inflateView() {
        checkActivityResumed();

        inflate(getContext(), R.layout.snabble_view_paydirekt, this);

        okHttpClient = Snabble.getInstance().getProjects().get(0).getOkHttpClient();

        progressBar = findViewById(R.id.progress);

        webView = findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Dispatch.mainThread(() -> finishWithError());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Logger.d("shouldOverrideUrlLoading " + request.getUrl().toString());

                Uri uri = request.getUrl();
                if (uri != null) {
                    String url = uri.toString();
                    for (RedirectUrl redirectUrl : RedirectUrl.values()) {
                        if (url.equals(redirectUrl.getUrl())) {
                            switch (redirectUrl) {
                                case SUCCESS:
                                    authenticateAndSave();
                                    break;
                                case CANCELLED:
                                    finish();
                                    break;
                                case FAILURE:
                                    finishWithError();
                                    break;
                            }

                            return true;
                        }
                    }
                }
                return super.shouldOverrideUrlLoading(view, request);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, final int newProgress) {
                Dispatch.mainThread(() -> {
                    if (newProgress == 100) {
                        progressBar.setVisibility(View.GONE);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                    }

                    progressBar.setProgress(newProgress);
                });
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
//        webView.getSettings().setDomStorageEnabled(true);

//        CookieManager.getInstance().setAcceptCookie(true);
//        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);


        load();
    }

    private void load() {
        Project project = SnabbleUI.getProject();
        String url = Snabble.getInstance().getPaydirektAuthUrl();
        if (url == null) {
            finishWithError();
            return;
        }

        PaydirektAuthorizationRequest paydirektAuthorizationRequest = new PaydirektAuthorizationRequest();
        paydirektAuthorizationRequest.id = Build.MODEL;
        paydirektAuthorizationRequest.name = Build.PRODUCT; // TODO correct?
        paydirektAuthorizationRequest.fingerprint = "167-671"; // WTF?
        paydirektAuthorizationRequest.ipAddress = "127.0.0.1"; // WTF?
        paydirektAuthorizationRequest.redirectUrlAfterSuccess = RedirectUrl.SUCCESS.getUrl();
        paydirektAuthorizationRequest.redirectUrlAfterCancellation = RedirectUrl.CANCELLED.getUrl();
        paydirektAuthorizationRequest.redirectUrlAfterFailure = RedirectUrl.FAILURE.getUrl();

        String json = GsonHolder.get().toJson(paydirektAuthorizationRequest);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        project.getOkHttpClient().newCall(request).enqueue(
                new SimpleJsonCallback<PaydirektAuthorizationResult>(PaydirektAuthorizationResult.class) {
                    @Override
            public void success(PaydirektAuthorizationResult result) {
                Dispatch.mainThread(() -> {
                    authorizationResult = result;
                    String webLink = result.getWebLink();
                    if (webLink == null) {
                        finishWithError();
                        return;
                    }

                    webView.loadUrl(webLink);
                });
            }

            @Override
            public void error(Throwable t) {
                Dispatch.mainThread(() -> {
                    finishWithError();
                });
            }
        });
    }

    private void checkActivityResumed() {
        FragmentActivity fragmentActivity = UIUtils.getHostFragmentActivity(getContext());
        if (fragmentActivity != null) {
            isActivityResumed = fragmentActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
        } else {
            isActivityResumed = true;
        }
    }

    private void authenticateAndSave() {
        if (Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()) {
            Keyguard.unlock(UIUtils.getHostFragmentActivity(getContext()), new Keyguard.Callback() {
                @Override
                public void success() {
                    save();
                }

                @Override
                public void error() {
                    if (isShown()) {
                        finish();
                    } else {
                        acceptedKeyguard = true;
                    }
                }
            });
        } else {
            save();
        }
    }

    private void save() {
        final PaymentCredentials pc = PaymentCredentials.fromPaydirektInfo(authorizationResult.getAuthorizationLink());

        if (pc == null) {
            Toast.makeText(getContext(), "Could not verify payment credentials", Toast.LENGTH_LONG)
                    .show();
        } else {
            Snabble.getInstance().getPaymentCredentialsStore().add(pc);
            Telemetry.event(Telemetry.Event.PaymentMethodAdded, pc.getType().name());
        }

        if (isShown()) {
            finish();
        } else {
            acceptedKeyguard = true;
        }
    }

    private void finish() {
        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            callback.execute(SnabbleUI.Action.GO_BACK, null);
        }
    }

    private void finishWithError() {
        Toast.makeText(getContext(),
                R.string.Snabble_paydirekt_authorizationFailed_message,
                Toast.LENGTH_SHORT)
                .show();

        finish();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        checkActivityResumed();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        isAttachedToWindow = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);

        isAttachedToWindow = false;
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        if (acceptedKeyguard) {
                            finish();
                            acceptedKeyguard = false;
                        }
                    }
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    super.onActivityResumed(activity);
                    isActivityResumed = true;

                    if (pendingPaydirektInfo != null) {
                        authenticateAndSave();
                        pendingPaydirektInfo = null;
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    super.onActivityPaused(activity);
                    isActivityResumed = false;
                }
            };


}
