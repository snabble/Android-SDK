package io.snabble.sdk.ui.payment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.snabble.sdk.Environment;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.payment.data.GiropayAuthorizationData;
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

public class GiropayInputView extends FrameLayout {
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

    private String projectId;
    private boolean acceptedKeyguard;
    private WebView webView;
    private OkHttpClient okHttpClient;
    private ProgressBar progressBar;
    private AuthorizationResult authorizationResult;
    private GiropayAuthorizationData authorizationData;

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        }
    };

    public GiropayInputView(Context context) {
        super(context);
        inflateView();
    }

    public GiropayInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public GiropayInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    @SuppressLint({"InlinedApi", "SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void inflateView() {
        inflate(getContext(), R.layout.snabble_view_giropay, this);

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
                final Uri uri = request.getUrl();
                final Environment environment = Snabble.getInstance().getEnvironment();
                if (isGiropayAppLinkUrl(uri, environment)) {
                    final Intent giropayAppLinkIntent = new Intent(Intent.ACTION_VIEW);
                    giropayAppLinkIntent.setData(request.getUrl());
                    if (isGiropayAppAvailable(giropayAppLinkIntent, view.getContext(), environment)) {
                        view.getContext().startActivity(giropayAppLinkIntent);
                        return true;
                    }
                } else if (uri != null) {
                    final String url = uri.toString();
                    Logger.d("shouldOverrideUrlLoading: <" + url + ">");

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
        @Nullable final Project project = Snabble.getInstance().getCheckedInProject().getValue();
        if (project != null) projectId = project.getId();

        String url = Snabble.getInstance().getGiropayAuthUrl();
        if (url == null) {
            finishWithError();
            return;
        }

        authorizationData = new GiropayAuthorizationData(
                UUID.randomUUID().toString(),
                Build.MODEL,
                "127.0.0.1",
                "167-671",
                SUCCESS_URL,
                CANCELLED_URL,
                FAILURE_URL
        );

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
                        Dispatch.mainThread(() -> finishWithError());
                    }
                });
    }

    private boolean isGiropayAppLinkUrl(@Nullable final Uri uri, @Nullable Environment environment) {
        if (uri != null && uri.getHost() != null) {
            return uri.getHost().startsWith(getGiropayAppLinkUrlHost(environment));
        }
        return false;
    }

    @NonNull
    private String getGiropayAppLinkUrlHost(@Nullable final Environment environment) {
        if (environment == Environment.PRODUCTION) {
            return "app.paydirekt.de";
        } else {
            return "app.sandbox.paydirekt.de";
        }
    }

    private boolean isGiropayAppAvailable(
            @NonNull final Intent intent,
            @NonNull final Context context,
            @Nullable final Environment environment
    ) {
        final List<ResolveInfo> intentInfo = context.getPackageManager().queryIntentActivities(intent, 0);
        final String appPackageName = getGiropayAppPackage(environment);
        for (final ResolveInfo info : intentInfo) {
            if (info.activityInfo.packageName.contains(appPackageName)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private String getGiropayAppPackage(final @Nullable Environment environment) {
        if (environment == Environment.PRODUCTION) {
            return "com.gimb.paydirekt.app";
        } else {
            return "com.gimb.paydirekt.app.sandbox";
        }
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
        final PaymentCredentials pc = PaymentCredentials.fromGiropay(
                authorizationData,
                authorizationResult.getAuthorizationLink(),
                projectId
        );

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
        SnabbleUI.executeAction(getContext(), SnabbleUI.Event.GO_BACK);
    }

    private void finishWithError() {
        Logger.d("finishWithError");

        Toast.makeText(getContext(),
                        R.string.Snabble_Giropay_AuthorizationFailed_title,
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

    private final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
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
