package io.snabble.sdk.ui.payment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.core.view.ViewCompat;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.snabble.sdk.Snabble;
import io.snabble.sdk.payment.PaymentCredentials;
import io.snabble.sdk.ui.KeyguardHandler;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import io.snabble.sdk.utils.Utils;

public class CreditCardInputView extends FrameLayout {
    private boolean acceptedKeyguard;
    private WebView webView;

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

        inflate(getContext(), R.layout.view_cardinput_creditcard, this);
        webView = findViewById(R.id.web_view);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // this disables credit card storage prompt for google pay
        ViewCompat.setImportantForAutofill(webView, View.IMPORTANT_FOR_AUTOFILL_NO);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new JsInterface(), "snabble");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy:MM:dd-HH:mm:ss", Locale.US);

        // TODO generate this on backend
        String storeId = "12022224362"; // TODO prod id
        String formattedDate = simpleDateFormat.format(new Date());
        final String chargeTotal = "1.00";
        String currency = "978"; // EUR
        String sharedSecret = "a66KgY>\"aN"; // this should not be in the app code

        String stringToHash = storeId + formattedDate + chargeTotal + currency + sharedSecret;
        String hash = Utils.sha256Hex(Utils.hexString(stringToHash.getBytes(Charset.forName("US-ASCII"))));

        try {
            String data = IOUtils.toString(getContext().getResources().openRawResource(R.raw.creditcardform), Charset.forName("UTF-8"));
            data = data.replace("{{url}}", "https://test.ipg-online.com/connect/gateway/processing"); // TODO url from backend
            data = data.replace("{{storeId}}", storeId);
            data = data.replace("{{date}}", formattedDate);
            data = data.replace("{{currency}}", currency);
            data = data.replace("{{chargeTotal}}", chargeTotal);
            data = data.replace("{{hash}}", hash);

            webView.loadData(data, null, null);
        } catch (IOException e) {
            Logger.e(e.getMessage());
        }
    }

    private void authenticateAndSave(String cardHolder, String obfuscatedCardNumber, String creditCardBrand,
                          String expirationYear, String expirationMonth, String hostedDataId) {
        if (Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()) {
            SnabbleUI.getUiCallback().requestKeyguard(new KeyguardHandler() {
                @Override
                public void onKeyguardResult(int resultCode) {
                    if (resultCode == Activity.RESULT_OK) {
                        save(cardHolder, obfuscatedCardNumber, creditCardBrand,
                                expirationYear, expirationMonth, hostedDataId);
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
            save(cardHolder, obfuscatedCardNumber, creditCardBrand,
                    expirationYear, expirationMonth, hostedDataId);
        }
    }

    private void save(String cardHolder, String obfuscatedCardNumber, String creditCardBrand,
                     String expirationYear, String expirationMonth, String hostedDataId) {
        PaymentCredentials.Brand ccBrand;

        switch (creditCardBrand) {
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

        final PaymentCredentials pc = PaymentCredentials.fromCreditCardData(cardHolder, ccBrand, obfuscatedCardNumber,
                expirationYear, expirationMonth, hostedDataId);
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

    private void finish() {
        SnabbleUICallback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            callback.goBack();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

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
            };

    @Keep
    public class JsInterface {
        @JavascriptInterface
        public void saveCard(String cardHolder, String obfuscatedCardNumber, String creditCardBrand,
                                            String expirationYear, String expirationMonth, String hostedDataId) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    authenticateAndSave(cardHolder, obfuscatedCardNumber, creditCardBrand, expirationYear, expirationMonth, hostedDataId);
                }
            });
        }

        @JavascriptInterface
        public void fail() {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO i18n
                    Toast.makeText(getContext(), "Ihre Kreditkarte konnte nicht hinterlegt werden.", Toast.LENGTH_SHORT).show();
                    finish();
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
