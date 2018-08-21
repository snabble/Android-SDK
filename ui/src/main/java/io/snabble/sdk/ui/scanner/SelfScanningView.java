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
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.snabble.sdk.Checkout;
import io.snabble.sdk.OnProductAvailableListener;
import io.snabble.sdk.Product;
import io.snabble.sdk.ProductDatabase;
import io.snabble.sdk.Project;
import io.snabble.sdk.codes.ScannableCode;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.SnabbleUICallback;
import io.snabble.sdk.ui.telemetry.Telemetry;
import io.snabble.sdk.ui.utils.DelayedProgressDialog;
import io.snabble.sdk.ui.utils.UIUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
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
    private long detectAfterTimeMs;
    private boolean ignoreNextDialog;

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

        Project project = SnabbleUI.getProject();

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
                onClickEnterBarcode();
            }
        });

        barcodeScanner.setIndicatorOffset(0, Utils.dp2px(getContext(), -36));

        barcodeScanner.addBarcodeFormat(BarcodeFormat.EAN_8);
        barcodeScanner.addBarcodeFormat(BarcodeFormat.EAN_13);
        barcodeScanner.addBarcodeFormat(BarcodeFormat.CODE_128);

        progressDialog = new DelayedProgressDialog(getContext());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getContext().getString(R.string.Snabble_loadingProductInformation));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    resumeBarcodeScanner();
                    progressDialog.dismiss();

                    ignoreNextDialog = true;

                    return true;
                }
                return false;
            }
        });

        this.productDatabase = project.getProductDatabase();

        barcodeScanner.setCallback(new BarcodeScannerView.Callback() {
            @Override
            public void onBarcodeDetected(final Barcode barcode) {
                handleBarcodeDetected(barcode);
            }
        });

        productDialog = new ProductConfirmationDialog(getContext(), project);
        productDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                resumeBarcodeScanner();
                allowScan = true;
            }
        });

        productDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                allowScan = false;
                pauseBarcodeScanner();
            }
        });

        isInitialized = true;
        startBarcodeScanner();
    }

    public void lookupAndShowProduct(final ScannableCode scannedCode) {
        productDialog.dismiss();
        ignoreNextDialog = false;

        if(scannedCode.hasEmbeddedData() && !scannedCode.isEmbeddedDataOk()){
            delayNextScan();

            Telemetry.event(Telemetry.Event.ScannedUnknownCode, scannedCode.getCode());
            UIUtils.snackbar(SelfScanningView.this,
                    R.string.Snabble_Scanner_unknownBarcode,
                    Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        progressDialog.showAfterDelay(300);
        pauseBarcodeScanner();

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

    private void delayNextScan() {
        detectAfterTimeMs = SystemClock.elapsedRealtime() + 2000;
    }

    @SuppressLint("MissingPermission")
    private void handleBarcodeDetected(final Barcode barcode) {
        if (SystemClock.elapsedRealtime() > detectAfterTimeMs) {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.VIBRATE)
                    == PackageManager.PERMISSION_GRANTED) {
                vibrator.vibrate(500L);
            }

            lookupAndShowProduct(ScannableCode.parse(SnabbleUI.getProject(), barcode.getText()));
        }
    }

    private void handleProductAvailable(Product product, boolean wasOnlineProduct, ScannableCode scannedCode) {
        if (ignoreNextDialog) {
            return;
        }

        progressDialog.dismiss();

        if(product.getBundleProducts().length > 0){
            showBundleDialog(product);
        } else {
            showProduct(product, scannedCode);

            if (wasOnlineProduct) {
                Telemetry.event(Telemetry.Event.ScannedOnlineProduct, product);
            } else {
                Telemetry.event(Telemetry.Event.ScannedProduct, product);
            }
        }
    }

    private void handleProductNotFound(ScannableCode scannedCode) {
        progressDialog.dismiss();
        resumeBarcodeScanner();
        delayNextScan();

        Telemetry.event(Telemetry.Event.ScannedUnknownCode, scannedCode.getCode());
        UIUtils.snackbar(SelfScanningView.this,
                R.string.Snabble_Scanner_unknownBarcode,
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void handleProductError() {
        progressDialog.dismiss();
        resumeBarcodeScanner();
        delayNextScan();

        UIUtils.snackbar(SelfScanningView.this,
                R.string.Snabble_Scanner_networkError,
                Snackbar.LENGTH_LONG)
                .show();
    }

    private void onClickEnterBarcode() {
        SnabbleUICallback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            if(productDatabase.isAvailableOffline() && productDatabase.isUpToDate()){
                callback.showBarcodeSearch();
            } else {
                final EditText input = new EditText(getContext());
                MarginLayoutParams lp = new MarginLayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);

                new AlertDialog.Builder(getContext())
                        .setView(input)
                        .setTitle(R.string.Snabble_Scanner_enterBarcode)
                        .setPositiveButton(R.string.Snabble_Done, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                lookupAndShowProduct(ScannableCode.parse(SnabbleUI.getProject(),
                                        input.getText().toString()));
                            }
                        })
                        .setNegativeButton(R.string.Snabble_Cancel, null)
                        .create()
                        .show();
            }
        }
    }

    public void resume() {
        resumeBarcodeScanner();
    }

    public void pause() {
        pauseBarcodeScanner();
    }

    private void pauseBarcodeScanner() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeScanner.pause();
        }
    }

    private void resumeBarcodeScanner() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeScanner.resume();
        }
    }

    private void showBundleDialog(Product product) {
        pauseBarcodeScanner();
        allowScan = false;

        SelectBundleDialog.show(getContext(), product, new SelectBundleDialog.Callback() {
            @Override
            public void onProductSelected(Product product) {
                Telemetry.event(Telemetry.Event.SelectedBundleProduct, product);

                String[] codes = product.getScannableCodes();
                if(codes.length > 0) {
                    showProduct(product, ScannableCode.parse(SnabbleUI.getProject(), codes[0]));
                }
            }

            @Override
            public void onDismissed() {
                resumeBarcodeScanner();
            }
        });
    }

    private void showProduct(Product product, ScannableCode scannedCode) {
        pauseBarcodeScanner();
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

    public void registerListeners() {
        isRunning = true;

        startBarcodeScanner();
    }

    public void unregisterListeners() {
        isRunning = false;

        stopBarcodeScanner();

        progressDialog.dismiss();
        productDialog.dismiss();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        registerListeners();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Application application = (Application) getContext().getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);

        unregisterListeners();
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks =
            new SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        registerListeners();
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        unregisterListeners();
                    }
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    if (UIUtils.getHostActivity(getContext()) == activity) {
                        startBarcodeScanner();
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
                progressDialog.showAfterDelay(500);
                break;
            case REQUEST_PAYMENT_METHOD:
            case WAIT_FOR_APPROVAL:
                SnabbleUICallback callback = SnabbleUI.getUiCallback();
                if (callback != null) {
                    callback.showCheckout();
                }

                progressDialog.dismiss();
                break;
            case CONNECTION_ERROR:
                progressDialog.dismiss();

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
