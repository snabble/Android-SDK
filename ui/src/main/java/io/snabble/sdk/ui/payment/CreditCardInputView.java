package io.snabble.sdk.ui.payment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.Keyguard;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.payment.creditcard.data.CreditCardInfo;
import io.snabble.sdk.ui.payment.creditcard.data.SnabbleCreditCardUrlCreator;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import okhttp3.Request;

public class CreditCardInputView extends RelativeLayout {
    public static final String ARG_PROJECT_ID = "projectId";
    public static final String ARG_PAYMENT_TYPE = "paymentType";

    private boolean acceptedKeyguard;
    private WebView webView;
    private Resources resources;
    private ProgressBar progressBar;
    private boolean isActivityResumed;
    private CreditCardInfo pendingCreditCardInfo;

    private PaymentMethod paymentType;
    private String projectId;
    private String formUrl;
    private String deleteUrl;
    private TextView threeDHint;
    private boolean isLoaded;

    public CreditCardInputView(Context context) {
        super(context);
    }

    public CreditCardInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CreditCardInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint({"InlinedApi", "SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void inflateView() {
        checkActivityResumed();

        inflate(getContext(), R.layout.snabble_view_cardinput_creditcard, this);

        resources = getContext().getResources();

        progressBar = findViewById(R.id.progress);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);

        webView = findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Dispatch.mainThread(() -> finishWithError(null));
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
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(newProgress);
                });
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new JsInterface(), "snabble");
        webView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
                webView.scrollTo(0, 0);
            }
        });


        // this disables credit card storage prompt for google pay
        ViewCompat.setImportantForAutofill(webView, View.IMPORTANT_FOR_AUTOFILL_NO);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        threeDHint = findViewById(R.id.threed_secure_hint);
        threeDHint.setVisibility(View.GONE);

        watchForBigSizeChanges();
        setProject();
        loadUrl();
    }

    public void watchForBigSizeChanges() {
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            int highestHeight = 0;

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int height = bottom - top;
                highestHeight = Math.max(highestHeight, height);
                if (height < highestHeight) {
                    threeDHint.setVisibility(View.GONE);

                    // for some reason the WebView bounds do not update itself on layout changes
                    webView.setLeft(getLeft());
                    webView.setTop(getTop());
                    webView.setRight(getRight());
                    webView.setBottom(getBottom());
                } else {
                    if (isLoaded) {
                        threeDHint.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void setProject() {
        if (getProject() == null) {
            finishWithError("No project");
        }
    }

    public void load(String projectId, PaymentMethod paymentType, String formUrl, String deletePreAuthUrl) {
        this.paymentType = paymentType;
        this.projectId = projectId;
        this.formUrl = Snabble.getInstance().absoluteUrl(formUrl);
        this.deleteUrl = Snabble.getInstance().absoluteUrl(deletePreAuthUrl);
        inflateView();
    }

    private void checkActivityResumed() {
        FragmentActivity fragmentActivity = UIUtils.getHostFragmentActivity(getContext());
        if (fragmentActivity != null) {
            isActivityResumed = fragmentActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
        } else {
            isActivityResumed = true;
        }
    }

    @Nullable
    private Project getProject() {
        Project project = null;
        for (Project p : Snabble.getInstance().getProjects()) {
            if (p.getId().equals(projectId)) {
                project = p;
            }
        }
        return project;
    }

    private void loadUrl() {
        final String formUrl = SnabbleCreditCardUrlCreator.createCreditCardUrlFor(paymentType, this.formUrl);
        webView.loadUrl(formUrl);
    }

    private void authenticateAndSave(final CreditCardInfo creditCardInfo) {
        Keyguard.unlock(UIUtils.getHostFragmentActivity(getContext()), new Keyguard.Callback() {
            @Override
            public void success() {
                save(creditCardInfo);
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

    private void save(CreditCardInfo info) {
        PaymentCredentials.Brand ccBrand;

        switch (info.getBrand()) {
            case "VISA":
                ccBrand = PaymentCredentials.Brand.VISA;
                break;
            case "MASTERCARD":
                ccBrand = PaymentCredentials.Brand.MASTERCARD;
                break;
            case "AMEX":
                ccBrand = PaymentCredentials.Brand.AMEX;
                break;
            default:
                ccBrand = PaymentCredentials.Brand.UNKNOWN;
                break;
        }

        final PaymentCredentials pc = PaymentCredentials.fromCreditCardData(info.getCardHolder(),
                ccBrand,
                projectId,
                info.getObfuscatedCardNumber(),
                info.getExpirationMonth(),
                info.getExpirationYear(),
                info.getHostedDataId(),
                info.getSchemeTransactionId(),
                info.getStoreId());

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
        deletePreAuth();
        SnabbleUI.executeAction(getContext(), SnabbleUI.Event.GO_BACK);
    }

    private void deletePreAuth() {
        Dispatch.io(() -> {
            final Request request = new Request.Builder().url(deleteUrl).delete().build();
            try {
                final Project project = getProject();
                if (project != null) {
                    project.getOkHttpClient().newCall(request).execute();
                }
            } catch (final IOException ignored) {
            }
        });
    }

    private void finishWithError(String failReason) {
        String errorMessage = getContext().getString(R.string.Snabble_Payment_CreditCard_error);
        if (failReason != null) {
            errorMessage = errorMessage + ": " + failReason;
        }

        Toast.makeText(getContext(),
                        errorMessage,
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
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
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

                @Override
                public void onActivityResumed(Activity activity) {
                    super.onActivityResumed(activity);
                    isActivityResumed = true;

                    if (pendingCreditCardInfo != null) {
                        authenticateAndSave(pendingCreditCardInfo);
                        pendingCreditCardInfo = null;
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    super.onActivityPaused(activity);
                    isActivityResumed = false;
                }
            };

    @Keep
    private class JsInterface {
        @JavascriptInterface
        public void save(final String card) {
            CreditCardInfo creditCardInfo = CreditCardInfo.toCreditCardInfo(card);

            if (isActivityResumed) {
                Dispatch.mainThread(() -> authenticateAndSave(creditCardInfo));
            } else {
                pendingCreditCardInfo = creditCardInfo;
            }
        }

        @JavascriptInterface
        public void preAuthInfo(final String totalCharge, final String currency) {
            Dispatch.mainThread(() -> {
                isLoaded = true;
                Project project = getProject();
                String companyName = project.getName();
                if (project.getCompany() != null && project.getCompany().name != null) {
                    companyName = project.getCompany().name;
                }
                NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
                numberFormat.setCurrency(Currency.getInstance(currency));
                BigDecimal chargeTotal = new BigDecimal(totalCharge);
                threeDHint.setVisibility(View.VISIBLE);
                threeDHint.setText(
                        resources.getString(R.string.Snabble_CC_3dsecureHint_retailerWithPrice,
                                numberFormat.format(chargeTotal),
                                companyName)
                );
            });
        }

        @JavascriptInterface
        public void fail(String failReason) {
            Dispatch.mainThread(() -> finishWithError(failReason));
        }

        @JavascriptInterface
        public void abort() {
            Dispatch.mainThread(CreditCardInputView.this::finish);
        }

        @JavascriptInterface
        public void toast(String message) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void log(String message) {
            Logger.d(message);
        }
    }
}
