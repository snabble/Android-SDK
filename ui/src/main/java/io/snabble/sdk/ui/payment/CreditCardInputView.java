package io.snabble.sdk.ui.payment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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


import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.KeyguardHandler;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import io.snabble.sdk.utils.SimpleJsonCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CreditCardInputView extends FrameLayout {
    private boolean acceptedKeyguard;
    private WebView webView;
    private OkHttpClient okHttpClient;
    private Resources resources;
    private HashResponse lastHashResponse;
    private Handler handler;
    private ProgressBar progressBar;
    private boolean isAttachedToWindow;

    public CreditCardInputView(Context context) {
        super(context);
        inflateView();
    }

    public CreditCardInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public CreditCardInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    @SuppressLint({"InlinedApi", "SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void inflateView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new RuntimeException("CreditCardInputView is only supported on API 21+");
        }

        inflate(getContext(), R.layout.snabble_view_cardinput_creditcard, this);

        okHttpClient = Snabble.getInstance().getProjects().get(0).getOkHttpClient();
        resources = getContext().getResources();
        handler = new Handler(Looper.getMainLooper());

        progressBar = findViewById(R.id.progress);

        webView = findViewById(R.id.web_view);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        finishWithError();
                    }
                });
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, final int newProgress) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (newProgress == 100) {
                            progressBar.setVisibility(View.GONE);
                        } else {
                            progressBar.setVisibility(View.VISIBLE);
                        }

                        progressBar.setProgress(newProgress);
                    }
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

    private void requestHash() {
        String url = Snabble.getInstance().getTelecashSecretUrl();
        if (url == null) {
            finishWithError();
            return;
        }

        Request request = new Request.Builder()
                .url(Snabble.getInstance().getTelecashSecretUrl())
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new SimpleJsonCallback<HashResponse>(HashResponse.class) {
            @Override
            public void success(final HashResponse hashResponse) {
                lastHashResponse = hashResponse;
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadForm(hashResponse);
                    }
                });
            }

            @Override
            public void error(Throwable t) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        finishWithError();
                    }
                });
            }
        });
    }

    private void loadForm(HashResponse hashResponse) {
        if (!isAttachedToWindow) {
            return;
        }

        try {
            String data = IOUtils.toString(resources.openRawResource(R.raw.snabble_creditcardform), Charset.forName("UTF-8"));
            data = data.replace("{{url}}", hashResponse.url);
            data = data.replace("{{storeId}}", hashResponse.storeId);
            data = data.replace("{{date}}", hashResponse.date);
            data = data.replace("{{currency}}", hashResponse.currency);
            data = data.replace("{{chargeTotal}}", hashResponse.chargeTotal);
            data = data.replace("{{hash}}", hashResponse.hash);

            // hides credit card selection, V = VISA, but in reality we can enter any credit card that is supported
            data = data.replace("{{paymentMethod}}", "V");
            webView.loadData(Base64.encodeToString(data.getBytes(), Base64.DEFAULT), null, "base64");
        } catch (IOException e) {
            Logger.e(e.getMessage());
        }
    }

    private void authenticateAndSave(final CreditCardInfo creditCardInfo) {
        cancelPreAuth(creditCardInfo);

        if (Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()) {
            SnabbleUI.getUiCallback().requestKeyguard(new KeyguardHandler() {
                @Override
                public void onKeyguardResult(int resultCode) {
                    if (resultCode == Activity.RESULT_OK) {
                        save(creditCardInfo);
                    } else {
                        if (isShown()) {
                            finish();
                        } else {
                            acceptedKeyguard = true;
                        }
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
            default:
                ccBrand = PaymentCredentials.Brand.UNKNOWN;
                break;
        }

        final PaymentCredentials pc = PaymentCredentials.fromCreditCardData(info.cardHolder, ccBrand,
                info.obfuscatedCardNumber, info.expirationYear,
                info.expirationMonth, info.hostedDataId, lastHashResponse.storeId);

        if (pc == null) {
            Toast.makeText(getContext(), "Could not verify payment credentials", Toast.LENGTH_LONG)
                    .show();
        } else {
            Snabble.getInstance().getPaymentCredentialsStore().add(pc);
        }

        if (isShown()) {
            finish();
        } else {
            acceptedKeyguard = true;
        }
    }

    private void cancelPreAuth(CreditCardInfo creditCardInfo) {
        String url = Snabble.getInstance().getTelecashPreAuthUrl();
        if (url == null) {
            Logger.e("Could not cancel pre authorization, no url provided");
            return;
        }

        url = url.replace("{orderID}", creditCardInfo.transactionId);

        Request request = new Request.Builder()
                .url(url)
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
        SnabbleUICallback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            callback.goBack();
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
            };

    private class CreditCardInfo {
        String cardHolder;
        String obfuscatedCardNumber;
        String brand;
        String expirationYear;
        String expirationMonth;
        String hostedDataId;
        String transactionId;

        public CreditCardInfo(String cardHolder, String obfuscatedCardNumber,
                              String brand, String expirationYear,
                              String expirationMonth, String hostedDataId,
                              String transactionId) {
            this.cardHolder = cardHolder;
            this.obfuscatedCardNumber = obfuscatedCardNumber;
            this.brand = brand;
            this.expirationYear = expirationYear;
            this.expirationMonth = expirationMonth;
            this.hostedDataId = hostedDataId;
            this.transactionId = transactionId;
        }
    }

    @Keep
    private class HashResponse {
        String hash;
        String storeId;
        String date;
        String chargeTotal;
        String url;
        String currency;
    }

    @Keep
    private class JsInterface {
        @JavascriptInterface
        public void saveCard(final String cardHolder, final String obfuscatedCardNumber,
                             final String brand, final String expirationYear,
                             final String expirationMonth, final String hostedDataId,
                             final String transactionId) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    authenticateAndSave(new CreditCardInfo(cardHolder, obfuscatedCardNumber,
                            brand, expirationYear,
                            expirationMonth, hostedDataId, transactionId));
                }
            });
        }

        @JavascriptInterface
        public void fail() {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    finishWithError();
                }
            });
        }

        @JavascriptInterface
        public void abort() {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
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
