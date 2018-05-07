package io.snabble.sdk.ui.scanner;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.OnProductAvailableListener;
import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.Utils;

public class SelfScanningView extends CoordinatorLayout implements Checkout.OnCheckoutStateChangedListener {
    private BarcodeScannerView barcodeScanner;
    private ProductDatabase productDatabase;
    private boolean isInitialized;
    private ProductConfirmationDialog productDialog;
    private boolean allowScan = true;
    private View enterBarcode;
    private View noPermission;
    private boolean isRunning;

    private DelayedProgressDialog progressDialog;
    private boolean ignore;
    private Checkout checkout;
    private long detectAfterTimeMs;

    private DialogInterface.OnCancelListener progressDialogCancelListener = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            ignore = true;

            barcodeScanner.resume();
            checkout.cancel();
        }
    };

    public SelfScanningView(Context context) {
        super(context);
        inflateView();
    }

    public SelfScanningView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public SelfScanningView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.view_self_scanning, this);

        barcodeScanner = findViewById(R.id.barcode_scanner_view);
        noPermission = findViewById(R.id.no_permission);

        enterBarcode = findViewById(R.id.enter_barcode);
        TextView light = findViewById(R.id.light);

        light.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeScanner.setTorchEnabled(!barcodeScanner.isTorchEnabled());
                Telemetry.event(Telemetry.Event.ToggleTorch);
            }
        });

        enterBarcode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showBarcodeSearch();
                }
            }
        });

        barcodeScanner.setIndicatorOffset(0, Utils.dp2px(getContext(), -36));

        barcodeScanner.addBarcodeFormat(BarcodeFormat.EAN_8);
        barcodeScanner.addBarcodeFormat(BarcodeFormat.EAN_13);
        barcodeScanner.addBarcodeFormat(BarcodeFormat.CODE_128);

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_pleaseWait));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnCancelListener(progressDialogCancelListener);

        setSdkInstance(SnabbleUI.getSdkInstance());
    }

    private void setSdkInstance(SnabbleSdk sdkInstance) {
        if (sdkInstance == null) {
            return;
        }

        this.productDatabase = sdkInstance.getProductDatabase();

        barcodeScanner.setCallback(new BarcodeScannerView.Callback() {
            @Override
            public void onBarcodeDetected(final Barcode barcode) {
                handleBarcodeDetected(barcode);
            }
        });

        productDialog = new ProductConfirmationDialog(getContext(), sdkInstance);
        productDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                barcodeScanner.resume();
                allowScan = true;
            }
        });

        productDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                allowScan = false;
                barcodeScanner.pause();
            }
        });

        checkout = sdkInstance.getCheckout();

        isInitialized = true;
        startBarcodeScanner();
    }

    public void lookupAndShowProduct(final ScannableCode scannedCode) {
        ignore = false;
        productDialog.dismiss();

        progressDialog.setOnCancelListener(progressDialogCancelListener);
        progressDialog.showAfterDelay(300);
        barcodeScanner.pause();

        if(scannedCode.hasEmbeddedData()){
            productDatabase.findByWeighItemIdOnline(scannedCode.getLookupCode(), new OnProductAvailableListener() {
                @Override
                public void onProductAvailable(Product product, boolean wasOnlineProduct) {
                    handleProductAvailable(product, wasOnlineProduct, scannedCode);
                }

                @Override
                public void onProductNotFound() {
                    handleProductNotFound(scannedCode);
                }

                @Override
                public void onError() {
                    handleProductError();
                }
            });
        } else {
            productDatabase.findByCodeOnline(scannedCode.getCode(), new OnProductAvailableListener() {
                @Override
                public void onProductAvailable(Product product, boolean wasOnlineProduct) {
                    handleProductAvailable(product, wasOnlineProduct, scannedCode);
                }

                @Override
                public void onProductNotFound() {
                    handleProductNotFound(scannedCode);
                }

                @Override
                public void onError() {
                    handleProductError();
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void handleBarcodeDetected(final Barcode barcode) {
        if (SystemClock.elapsedRealtime() > detectAfterTimeMs) {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.VIBRATE)
                    == PackageManager.PERMISSION_GRANTED) {
                vibrator.vibrate(500L);
            }

            lookupAndShowProduct(ScannableCode.parse(SnabbleUI.getSdkInstance(), barcode.getText()));
        }
    }

    private void handleProductAvailable(Product product, boolean wasOnlineProduct, ScannableCode scannedCode) {
        if (ignore) {
            return;
        }

        progressDialog.dismiss();
        showProduct(product, scannedCode);

        if (wasOnlineProduct) {
            Telemetry.event(Telemetry.Event.ScannedOnlineProduct, product);
        } else {
            Telemetry.event(Telemetry.Event.ScannedProduct, product);
        }
    }

    private void handleProductNotFound(ScannableCode scannedCode) {
        if (ignore) {
            return;
        }

        progressDialog.dismiss();
        barcodeScanner.resume();
        detectAfterTimeMs = SystemClock.elapsedRealtime() + 2000;

        Telemetry.event(Telemetry.Event.ScannedUnknownCode, scannedCode.getCode());
        UIUtils.snackbar(SelfScanningView.this,
                R.string.Snabble_Scanner_unknownBarcode,
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void handleProductError() {
        if (ignore) {
            return;
        }

        progressDialog.dismiss();
        barcodeScanner.resume();
        detectAfterTimeMs = SystemClock.elapsedRealtime() + 2000;

        UIUtils.snackbar(SelfScanningView.this,
                R.string.Snabble_Scanner_networkError,
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void showProduct(Product product, ScannableCode scannedCode) {
        barcodeScanner.pause();
        allowScan = false;
        showProductDialog(product, scannedCode);
    }

    public void hideProduct() {
        productDialog.dismiss();
    }

    protected void showProductDialog(Product product, ScannableCode scannedCode) {
        productDialog.show(product, scannedCode);
    }

    private void startBarcodeScanner() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (isInitialized && allowScan) {
                noPermission.setVisibility(View.GONE);
                barcodeScanner.setVisibility(View.VISIBLE);
                barcodeScanner.start();
            }
        } else if (isRunning) {
            noPermission.setVisibility(View.VISIBLE);
            barcodeScanner.setVisibility(View.GONE);
        }
    }

    private void stopBarcodeScanner() {
        if (isInitialized) {
            barcodeScanner.stop();
        }
    }

    public void addBarcodeFormat(BarcodeFormat barcodeFormat) {
        barcodeScanner.addBarcodeFormat(barcodeFormat);
    }

    public void removeBarcodeFormat(BarcodeFormat barcodeFormat) {
        barcodeScanner.removeBarcodeFormat(barcodeFormat);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        progressDialog.setOnCancelListener(progressDialogCancelListener);
        checkout.addOnCheckoutStateChangedListener(this);

        startBarcodeScanner();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        stopBarcodeScanner();
        checkout.removeOnCheckoutStateChangedListener(this);

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);

        progressDialog.setOnCancelListener(null);
        progressDialog.dismiss();
        productDialog.dismiss();
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityResumed(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        isRunning = true;
                        startBarcodeScanner();
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        isRunning = false;
                        stopBarcodeScanner();
                    }
                }
            };

    @Override
    public void onStateChanged(Checkout.State state) {
        if (!isShown()) {
            return;
        }

        switch (state) {
            case HANDSHAKING:
                progressDialog.setOnCancelListener(progressDialogCancelListener);
                progressDialog.showAfterDelay(500);
                break;
            case REQUEST_PAYMENT_METHOD:
            case WAIT_FOR_APPROVAL:
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showCheckout();
                }

                progressDialog.dismiss();
                checkout.removeOnCheckoutStateChangedListener(this);
                break;
            case CONNECTION_ERROR:
                progressDialog.dismiss();
                checkout.removeOnCheckoutStateChangedListener(this);

                UIUtils.snackbar(SelfScanningView.this,
                        R.string.Snabble_Payment_errorStarting,
                        Snackbar.LENGTH_LONG)
                        .show();
                break;
            default:
                progressDialog.dismiss();
        }
    }
}
