package io.snabble.sdk.ui.payment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
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

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.Map;
import java.util.UUID;

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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class PaydirektInputView extends FrameLayout {
    private static class Href {
        public String href;
    }

    private static class AuthorizationResult {
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

    private static final String SUCCESS_URL = "snabble-paydirekt://success";
    private static final String CANCELLED_URL = "snabble-paydirekt://cancelled";
    private static final String FAILURE_URL = "snabble-paydirekt://failure";

    private boolean acceptedKeyguard;
    private WebView webView;
    private OkHttpClient okHttpClient;
    private ProgressBar progressBar;
    private AuthorizationResult authorizationResult;
    private PaymentCredentials.PaydirektAuthorizationData authorizationData;

    private OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        }
    };

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
        inflate(getContext(), R.layout.snabble_view_paydirekt, this);

        okHttpClient = Snabble.getInstance().getProjects().get(0).getOkHttpClient();

        progressBar = findViewById(R.id.progress);

        webView = findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Logger.d("onReceivedError " + failingUrl);
                Dispatch.mainThread(() -> finishWithError());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri != null) {
                    String url = uri.toString();
                    Logger.d("shouldOverrideUrlLoading " + url);

                    switch (url) {
                        case SUCCESS_URL:
                            authenticateAndSave();
                            return true;
                        case CANCELLED_URL:
                            finish();
                            return true;
                        case FAILURE_URL:
                            finishWithError();
                            return true;
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
        webView.getSettings().setDomStorageEnabled(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        load();
    }

    private void load() {
        String url = Snabble.getInstance().getPaydirektAuthUrl();
        if (url == null) {
            finishWithError();
            return;
        }

        authorizationData = new PaymentCredentials.PaydirektAuthorizationData();
        authorizationData.id = UUID.randomUUID().toString();
        authorizationData.name = Build.MODEL;
        authorizationData.fingerprint = "167-671";
        authorizationData.ipAddress = "127.0.0.1";
        authorizationData.redirectUrlAfterSuccess = SUCCESS_URL;
        authorizationData.redirectUrlAfterCancellation = CANCELLED_URL;
        authorizationData.redirectUrlAfterFailure = FAILURE_URL;

        String json = GsonHolder.get().toJson(authorizationData);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        okHttpClient.newCall(request).enqueue(
                new SimpleJsonCallback<AuthorizationResult>(AuthorizationResult.class) {
                    @Override
            public void success(AuthorizationResult result) {
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

    private void authenticateAndSave() {
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
    }

    private void save() {
        final PaymentCredentials pc = PaymentCredentials.fromPaydirekt(authorizationData, authorizationResult.getAuthorizationLink());

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
        onBackPressedCallback.remove();

        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            callback.execute(SnabbleUI.Action.GO_BACK, null);
        }
    }

    private void finishWithError() {
        Logger.d("finishWithError");

        Toast.makeText(getContext(),
                R.string.Snabble_paydirekt_authorizationFailed_title,
                Toast.LENGTH_LONG)
                .show();

        finish();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        ComponentActivity componentActivity = UIUtils.getHostComponentActivity(getContext());
        if (componentActivity != null) {
            componentActivity.getOnBackPressedDispatcher().addCallback(componentActivity, onBackPressedCallback);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);

        onBackPressedCallback.remove();
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
            };
}
