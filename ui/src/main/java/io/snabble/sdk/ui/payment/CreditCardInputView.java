package io.snabble.sdk.ui.payment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

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
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreditCardInputView extends FrameLayout {
    private boolean acceptedKeyguard;
    private WebView webView;
    private OkHttpClient okHttpClient;
    private Resources resources;
    private HashResponse lastHashResponse;
    private String cancelPreAuthUrl;
    private ProgressBar progressBar;
    private boolean isActivityResumed;
    private CreditCardInfo pendingCreditCardInfo;

    private PaymentMethod paymentType;
    private String projectId;

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

        okHttpClient = Snabble.getInstance().getProjects().get(0).getOkHttpClient();
        resources = getContext().getResources();

        progressBar = findViewById(R.id.progress);

        webView = findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Dispatch.mainThread(() -> finishWithError());
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
        webView.addJavascriptInterface(new JsInterface(), "snabble");

        // this disables credit card storage prompt for google pay
        ViewCompat.setImportantForAutofill(webView, View.IMPORTANT_FOR_AUTOFILL_NO);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        requestHash();
    }

    public void load(String projectId, PaymentMethod paymentType) {
        this.projectId = projectId;
        this.paymentType = paymentType;

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

    private void requestHash() {
        Project project = null;
        for (Project p : Snabble.getInstance().getProjects()) {
            if (p.getId().equals(projectId)) {
                project = p;
            }
        }

        if (project == null) {
            finishWithError();
            return;
        }

        String url = project.getTelecashVaultItemsUrl();
        if (url == null) {
            finishWithError();
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create("", null))
                .build();

        okHttpClient.newCall(request).enqueue(new SimpleJsonCallback<HashResponse>(HashResponse.class) {
            @Override
            public void success(final HashResponse hashResponse) {
                lastHashResponse = hashResponse;
                cancelPreAuthUrl = headers().get("Location");
                Dispatch.mainThread(() -> loadForm(hashResponse));
            }

            @Override
            public void error(Throwable t) {
                Dispatch.mainThread(() -> finishWithError());
            }
        });
    }

    private void loadForm(HashResponse hashResponse) {
        try {
            String data = IOUtils.toString(resources.openRawResource(R.raw.snabble_creditcardform), Charset.forName("UTF-8"));
            data = data.replace("{{url}}", hashResponse.url);
            data = data.replace("{{hostedDataId}}", UUID.randomUUID().toString());
            data = data.replace("{{storeId}}", hashResponse.storeId);
            data = data.replace("{{orderId}}", hashResponse.orderId);
            data = data.replace("{{date}}", hashResponse.date);
            data = data.replace("{{currency}}", hashResponse.currency);
            data = data.replace("{{chargeTotal}}", hashResponse.chargeTotal);
            data = data.replace("{{hash}}", hashResponse.hash);

            // hides credit card selection, V = VISA, but in reality we can enter any credit card that is supported
            switch(paymentType) {
                case MASTERCARD:
                    data = data.replace("{{paymentMethod}}", "M");
                    break;
                case AMEX:
                    data = data.replace("{{paymentMethod}}", "A");
                    break;
                case VISA:
                default:
                    data = data.replace("{{paymentMethod}}", "V");
                    break;
            }

            webView.loadData(Base64.encodeToString(data.getBytes(), Base64.DEFAULT), null, "base64");
        } catch (IOException e) {
            Logger.e(e.getMessage());
        }
    }

    private void authenticateAndSave(final CreditCardInfo creditCardInfo) {
        cancelPreAuth(creditCardInfo);

        if (Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()) {
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
        } else {
            save(creditCardInfo);
        }
    }

    private void save(CreditCardInfo info) {
        PaymentCredentials.Brand ccBrand;

        switch (info.brand) {
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

        final PaymentCredentials pc = PaymentCredentials.fromCreditCardData(info.cardHolder,
                ccBrand,
                projectId,
                info.obfuscatedCardNumber,
                info.expirationYear,
                info.expirationMonth,
                info.hostedDataId,
                info.schemeTransactionId,
                lastHashResponse.storeId);

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

    private void cancelPreAuth(CreditCardInfo creditCardInfo) {
        String url = cancelPreAuthUrl;
        if (url == null) {
            Logger.e("Could not abort pre authorization, no url provided");
            return;
        }

        url = url.replace("{orderID}", creditCardInfo.transactionId);

        Request request = new Request.Builder()
                .url(Snabble.getInstance().absoluteUrl(url))
                .delete()
                .build();

        // fire and forget
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // ignore
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // ignore
            }
        });
    }

    private void finish() {
        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            callback.execute(SnabbleUI.Action.GO_BACK, null);
        }
    }

    private void finishWithError() {
        Toast.makeText(getContext(),
                R.string.Snabble_Payment_CreditCard_error,
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

    private class CreditCardInfo {
        String cardHolder;
        String obfuscatedCardNumber;
        String brand;
        String expirationYear;
        String expirationMonth;
        String hostedDataId;
        String schemeTransactionId;
        String transactionId;

        public CreditCardInfo(String cardHolder,
                              String obfuscatedCardNumber,
                              String brand,
                              String expirationYear,
                              String expirationMonth,
                              String hostedDataId,
                              String schemeTransactionId,
                              String transactionId) {
            this.cardHolder = cardHolder;
            this.obfuscatedCardNumber = obfuscatedCardNumber;
            this.brand = brand;
            this.expirationYear = expirationYear;
            this.expirationMonth = expirationMonth;
            this.hostedDataId = hostedDataId;
            this.schemeTransactionId = schemeTransactionId;
            this.transactionId = transactionId;
        }
    }

    @Keep
    private class HashResponse {
        String hash;
        String storeId;
        String orderId;
        String date;
        String chargeTotal;
        String url;
        String currency;
    }

    @Keep
    private class JsInterface {
        @JavascriptInterface
        public void saveCard(final String cardHolder,
                             final String obfuscatedCardNumber,
                             final String brand,
                             final String expirationYear,
                             final String expirationMonth,
                             final String hostedDataId,
                             final String schemeTransactionId,
                             final String transactionId) {
            CreditCardInfo creditCardInfo = new CreditCardInfo(cardHolder, obfuscatedCardNumber,
                    brand, expirationYear,
                    expirationMonth, hostedDataId, schemeTransactionId, transactionId);

            if (isActivityResumed) {
                Dispatch.mainThread(() -> authenticateAndSave(creditCardInfo));
            } else {
                pendingCreditCardInfo = creditCardInfo;
            }
        }

        @JavascriptInterface
        public void fail() {
            Dispatch.mainThread(CreditCardInputView.this::finishWithError);
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
